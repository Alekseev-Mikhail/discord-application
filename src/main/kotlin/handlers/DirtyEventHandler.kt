package handlers

import Application
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
        if (application.afkMode && application.isAlone()) {
            application.afkMode(event.guild)
        }
    }
}
