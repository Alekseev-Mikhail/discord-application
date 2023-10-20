package handlers

import Application
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import notifyInDiscord

class DirtyEventHandler(private val applications: MutableMap<String, Application>, private val developer: User) :
    ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach { guild -> applications[guild.id] = Application(guild, developer) }
        Runtime.getRuntime()
            .addShutdownHook(Thread { applications.forEach { (guildId, application) -> application.save(guildId) } })
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        if (!applications.containsKey(event.guild.id)) {
            applications[event.guild.id] = Application(event.guild, developer)
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        applications.remove(event.guild.id)
    }

    override fun onChannelDelete(event: ChannelDeleteEvent) {
        val application = applications[event.guild.id]
            ?: throw NullPointerException("Channel was deleted in an unknown guild").notifyInDiscord(developer)
        val deletedChannel = event.channel
        if (application.defaultChannel == deletedChannel) {
            application.defaultChannel = null
        }
        if (application.currentChannel == deletedChannel) {
            application.currentChannel = null
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val application = applications[event.guild.id]
            ?: throw NullPointerException("Audio channel was updated in an unknown guild").notifyInDiscord(developer)
        if (application.afkMode && application.isAlone()) {
            application.afkMode(event.guild)
        }
    }
}
