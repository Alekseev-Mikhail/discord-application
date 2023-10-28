package handlers

import Server
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import notifyInDiscord

class DirtyEventHandler(
    private val servers: MutableMap<String, Server>,
    private val playerManager: DefaultAudioPlayerManager,
    private val developer: User,
) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach { guild -> servers[guild.id] = Server(guild, playerManager, developer) }
        Runtime.getRuntime()
            .addShutdownHook(Thread { servers.forEach { (guildId, application) -> application.save(guildId) } })
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        if (!servers.containsKey(event.guild.id)) {
            servers[event.guild.id] = Server(event.guild, playerManager, developer)
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        servers.remove(event.guild.id)
    }

    override fun onChannelDelete(event: ChannelDeleteEvent) {
        val application = servers[event.guild.id]
            ?: throw NullPointerException("Channel was deleted in an unknown guild").notifyInDiscord(event.guild, developer)
        val deletedChannel = event.channel
        if (application.defaultChannel == deletedChannel) {
            application.defaultChannel = null
        }
        if (application.currentChannel == deletedChannel) {
            application.currentChannel = null
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val application = servers[event.guild.id]
            ?: throw NullPointerException("Audio channel was updated in an unknown guild").notifyInDiscord(event.guild, developer)
        if (application.afkMode && application.isAlone()) {
            application.afkMode(event.guild)
        }
    }
}
