package handlers

import Application
import checkCurrentGuild
import getCurrentGuild
import getProperty
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import path
import properties
import setProperty
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DirtyEventHandler(private val application: Application) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        val currentGuild = getCurrentGuild(event)
        application.defaultChannel = currentGuild.getVoiceChannelById(getProperty("app.defchannel"))
        application.afkMode = getProperty("app.afk.mode").toBooleanStrictOrNull() ?: true
        application.afkTime =
            getProperty("app.afk.time").toLongOrNull()?.seconds?.inWholeMilliseconds ?: 10.seconds.inWholeMilliseconds

        Runtime.getRuntime().addShutdownHook(
            Thread {
                val output = FileOutputStream(path)

                setProperty("app.defchannel", application.defaultChannel?.id ?: "-1")
                setProperty("app.afk.mode", application.afkMode.toString())
                setProperty("app.afk.time", application.afkTime.milliseconds.inWholeSeconds.toString())

                properties.store(output, null)
            },
        )
    }

    override fun onChannelDelete(event: ChannelDeleteEvent) {
        checkCurrentGuild(event, event.guild)
        val deletedChannel = event.channel
        if (application.defaultChannel == deletedChannel) {
            application.defaultChannel = null
        }
        if (application.currentChannel == deletedChannel) {
            application.currentChannel = null
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        checkCurrentGuild(event, event.guild)
        if (application.afkMode && application.isAlone()) {
            application.afkMode(event.guild)
        }
    }
}
