package handlers

import Application
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class DirtyEventHandler(private val application: Application) : ListenerAdapter() {
    override fun onChannelDelete(event: ChannelDeleteEvent) {
        val deletedChannel = event.channel
        if (application.defaultChannel == deletedChannel) {
            application.defaultChannel = null
        }
        if (application.currentChannel == deletedChannel) {
            application.currentChannel = null
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val currentChannel = application.currentChannel
        if (currentChannel != null) {
            val guild = event.guild
            if (currentChannel.members.size <= 1) {
                afkMode(guild)
            }
        }
    }

    private fun afkMode(guild: Guild) {
//        Thread.sleep(10_000)
//        val currentChannel = application.currentChannel ?: throw NullPointerException("Current channel cannot be null")
//        if (currentChannel.members.size <= 1) {
//            application.disconnectToChannel(guild)
//        }
//        delay(Duration.ofSeconds(5_000))
    }
}
