package handlers

import Application
import COMMAND_AFK_MODE_NAME
import COMMAND_AFK_MODE_REPLY_DISABLE
import COMMAND_AFK_MODE_REPLY_ENABLE
import COMMAND_AFK_TIME_NAME
import COMMAND_AFK_TIME_REPLY
import COMMAND_CONNECT_NAME
import COMMAND_CONNECT_REPLY_PROBLEM_TYPE
import COMMAND_DEFCHANNEL_NAME
import COMMAND_DEFCHANNEL_REPLY_DISABLE
import COMMAND_DEFCHANNEL_REPLY_ENABLE
import COMMAND_DEFCHANNEL_REPLY_PROBLEM_TYPE
import COMMAND_DISCONNECT_NAME
import COMMAND_DISCONNECT_PROBLEM
import COMMAND_DISCONNECT_REPLY
import COMMAND_INFO_NAME
import COMMAND_INFO_REPLY
import COMMAND_OPTION_CHANNEL_NAME
import COMMAND_OPTION_TIME_NAME
import COMMAND_OPTION_VALUE_NAME
import COMMAND_STATE_AFK_MODE
import COMMAND_STATE_AFK_TIME
import COMMAND_STATE_CHANNEL_CURRENT
import COMMAND_STATE_CHANNEL_DEFAULT
import COMMAND_STATE_NAME
import COMMAND_STATE_TITLE
import COMMAND_STATE_VALUE_DISABLE
import COMMAND_STATE_VALUE_ENABLE
import COMMAND_STATE_VALUE_NULL
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.lang.StringBuilder
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SlashCommandHandler(private val application: Application) : ListenerAdapter() {
//    private val playerManager = DefaultAudioPlayerManager().apply { registerRemoteSources(this) }
//    private val players = mutableMapOf<Guild, AudioPlayer>()
//    private val addressQueue = mutableListOf<String>()
//    private val trackQueue = mutableListOf<AudioTrack>()
//    private val playerState = PlayerState()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        application.checkDefaultGuild(application.getGuild(event))
        when (event.name) {
            COMMAND_INFO_NAME -> reply(event, COMMAND_INFO_REPLY)
            COMMAND_STATE_NAME -> reply(event, state())
            COMMAND_CONNECT_NAME -> reply(event, connect(event))
            COMMAND_DISCONNECT_NAME -> reply(event, disconnect(event))
            COMMAND_DEFCHANNEL_NAME -> reply(event, defChannel(event))
            COMMAND_AFK_TIME_NAME -> reply(event, afkTime(event))
            COMMAND_AFK_MODE_NAME -> reply(event, afkMode(event))
        }
    }

    private fun state(): String {
        val message = StringBuilder()
        message.append("$COMMAND_STATE_TITLE\n")
        message.append("$COMMAND_STATE_CHANNEL_CURRENT: ${application.currentChannel?.name ?: COMMAND_STATE_VALUE_NULL}\n")
        message.append("$COMMAND_STATE_CHANNEL_DEFAULT: ${application.defaultChannel?.name ?: COMMAND_STATE_VALUE_NULL}\n")
        message.append("$COMMAND_STATE_AFK_MODE: ${if (application.afkMode) COMMAND_STATE_VALUE_ENABLE else COMMAND_STATE_VALUE_DISABLE}\n")
        message.append("$COMMAND_STATE_AFK_TIME: ${application.afkTime.milliseconds.inWholeSeconds}\n")
        return message.toString()
    }

    private fun connect(event: SlashCommandInteractionEvent): String {
        val guild = application.getGuild(event)
        val option = event.getOption(COMMAND_OPTION_CHANNEL_NAME)

        if (option != null) {
            val channel = option.asChannel
            if (channel.type.isAudio) {
                return application.connect(guild, channel.asAudioChannel())
            }
            return COMMAND_CONNECT_REPLY_PROBLEM_TYPE
        }

        if (application.defaultChannel != null) {
            return application.connect(guild, application.defaultChannel)
        }

        return application.connect(event)
    }

    private fun disconnect(event: SlashCommandInteractionEvent): String {
        val guild = application.getGuild(event)
        if (guild.audioManager.isConnected) {
            application.disconnect(guild)
            return COMMAND_DISCONNECT_REPLY
        }
        return COMMAND_DISCONNECT_PROBLEM
    }

    private fun defChannel(event: SlashCommandInteractionEvent): String {
        val option = event.getOption(COMMAND_OPTION_CHANNEL_NAME)

        if (option != null) {
            val channel = option.asChannel
            if (channel.type.isAudio) {
                application.defaultChannel = channel.asAudioChannel()
                return COMMAND_DEFCHANNEL_REPLY_ENABLE
            }
            return COMMAND_DEFCHANNEL_REPLY_PROBLEM_TYPE
        }
        application.defaultChannel = null
        return COMMAND_DEFCHANNEL_REPLY_DISABLE
    }

    private fun afkTime(event: SlashCommandInteractionEvent): String {
        val time = application.getOption(event, COMMAND_OPTION_TIME_NAME).asInt
        application.afkTime = time.seconds.inWholeMilliseconds
        return COMMAND_AFK_TIME_REPLY
    }

    private fun afkMode(event: SlashCommandInteractionEvent): String {
        val option = event.getOption(COMMAND_OPTION_VALUE_NAME)

        if (option != null) {
            return when (option.asBoolean) {
                true -> {
                    application.afkMode = true
                    application.afkMode(application.getGuild(event))
                    COMMAND_AFK_MODE_REPLY_ENABLE
                }

                false -> {
                    application.afkMode = false
                    COMMAND_AFK_MODE_REPLY_DISABLE
                }
            }
        }

        return when (application.afkMode) {
            true -> {
                application.afkMode = false
                COMMAND_AFK_MODE_REPLY_DISABLE
            }

            false -> {
                application.afkMode = true
                application.afkMode(application.getGuild(event))
                COMMAND_AFK_MODE_REPLY_ENABLE
            }
        }
    }
//    private fun afk(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild ?: throw NullPointerException("Guild cannot be null in afk")
//
//    }
//
//    fun debug() {
//        for (logger in LoggerFactory.getILoggerFactory()) {
//            val index = logger.iteratorForAppenders()
//            while (index.hasNext()) {
//                val appender = index.next()
//            }
//        }
//        val guild = event.guild!!
//        val message = StringBuilder(":/")
//        addServer(guild)
//        trackQueue[guild]!!.forEach { e -> message.append(' ').append(e) }
//        message.append(", Playing: " + playerState[guild]!!.playing)
//        message.append(", Repeat: " + playerState[guild]!!.repeat)
//        message.append(", AddressQueuesSize: " + addressQueue.size)
//        message.append(", TrackQueuesSize: " + trackQueue.size)
//        message.append(", PlayerStatesSize: " + playerState.size)
//        message.append(", PlayersSize: " + players.size)
//        message.append(", ServersSize: " + servers.size)
//        return message.toString()
//    }
//
//
//
//    private fun play(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        val playerState = playerState[guild]!!
//        if (!playerState.playing) {
//            val option = event.getOption("address")
//            val trackQueue = trackQueue[guild]!!
//            if (option != null) {
//                connect(event)
//                queue(event, option.asString, false)
//                return "Музыка играет!"
//            } else if (trackQueue.isNotEmpty()) {
//                connect(event)
//                event.guild!!.audioManager.sendingHandler = handlers.AudioPlayerSendHandler(players[guild]!!)
//                players[guild]!!.playTrack(trackQueue[0])
//                this.playerState[guild]!!.playing = true
//                return "Музыка играет!"
//            } else {
//                return "В очереди нету музыки."
//            }
//        } else {
//            return "Музыка уже играет."
//        }
//    }
//
//    private fun pause(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        return if (playerState[guild]!!.playing) {
//            players[guild]!!.isPaused = true
//            "Музыка поставлена на паузу."
//        } else {
//            "Музыка уже не играет."
//        }
//    }
//
//    private fun resume(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        return if (playerState[guild]!!.playing) {
//            players[guild]!!.isPaused = false
//            "Музыка снята с паузы."
//        } else {
//            "Нечего возобнавлять."
//        }
//    }
//
//    private fun repeat(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        playerState[guild]!!.repeat = event.getOption("value")!!.asBoolean
//        return "Состояние изменено."
//    }
//
//    private fun queue(event: SlashCommandInteractionEvent, option: String, queueCommand: Boolean) {
//        val guild = event.guild!!
//        addServer(guild)
//        playerManager.loadItem(option, object : AudioLoadResultHandler {
//            override fun trackLoaded(track: AudioTrack) {
//                val trackQueue = trackQueue[guild]!!
//                val addressQueue = addressQueue[guild]!!
//                val player = players[guild]!!
//                val playerState = playerState[guild]!!
//                if (queueCommand) {
//                    trackQueue.add(track)
//                    addressQueue.add(option)
//                    event.reply("Музыка добавлена в очередь.").setEphemeral(true).queue()
//                } else {
//                    trackQueue.add(track)
//                    addressQueue.add(option)
//                    play(event)
//                    playerState.playing = true
//                    player.playTrack(track)
//                }
//            }
//
//            override fun playlistLoaded(playlist: AudioPlaylist) {
//                val trackQueue = trackQueue[guild]!!
//                val addressQueue = addressQueue[guild]!!
//                val player = players[guild]!!
//                val playerState = playerState[guild]!!
//                if (queueCommand) {
//                    for (track in playlist.tracks) {
//                        trackQueue.add(track)
//                        addressQueue.add(option)
//                    }
//                    event.reply("Музыка добавлена в очередь.").setEphemeral(true).queue()
//                } else {
//                    for (track in playlist.tracks) {
//                        trackQueue.add(track)
//                        addressQueue.add(option)
//                    }
//                    playerState.playing = true
//                    player.playTrack(trackQueue[0])
//                }
//            }
//
//            override fun noMatches() {
//                event.reply("По данной ссылке музыки нет.").setEphemeral(true).queue()
//            }
//
//            override fun loadFailed(throwable: FriendlyException) {
//                event.reply("Ошибка при загрузке музыки.").setEphemeral(true).queue()
//            }
//        })
//    }
//
//    private fun clone(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        val trackQueue = trackQueue[guild]!!
//        return if (trackQueue.isNotEmpty()) {
//            val addressQueue = addressQueue[guild]!!
//            trackQueue.add(1, trackQueue[0].makeClone())
//            addressQueue.add(1, addressQueue[0])
//            "Трек клонирован."
//        } else {
//            "Очередь пуста."
//        }
//    }
//
//    private fun del(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        val trackQueue = trackQueue[guild]!!
//        val option = event.getOption("id")!!.asInt
//        return if (trackQueue.isNotEmpty()) {
//            if (option <= trackQueue.size && option > 0) {
//                val playerState = playerState[guild]!!
//                val addressQueue = addressQueue[guild]!!
//                if (playerState.playing) {
//                    val player = players[guild]!!
//                    player.stopTrack()
//                    playerState.playing = false
//                }
//                val option = event.getOption("id")!!.asInt - 1
//                trackQueue.removeAt(option)
//                addressQueue.removeAt(option)
//                "Трек удалён."
//            } else {
//                "Трека под таким айди нет."
//            }
//        } else {
//            "Очередь пуста."
//        }
//    }
//
//    private fun clear(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        val playerState = playerState[guild]!!
//        val trackQueue = trackQueue[guild]!!
//        val addressQueue = addressQueue[guild]!!
//        if (playerState.playing) {
//            val player = players[guild]!!
//            player.stopTrack()
//            playerState.playing = false
//        }
//        trackQueue.clear()
//        addressQueue.clear()
//        return "Очередь очищена."
//    }
//
//    private fun check(event: SlashCommandInteractionEvent): String {
//        val guild = event.guild!!
//        addServer(guild)
//        val addressQueue = addressQueue[guild]!!
//        var message = ""
//        addressQueue.forEachIndexed { i, e ->
//            if (i > 0) {
//                message = "$message, $e"
//            } else {
//                message += e
//            }
//        }
//        return message.ifBlank {
//            "Очередь пуста."
//        }
//    }

    private fun reply(event: SlashCommandInteractionEvent, message: String) =
        event.reply(message).setEphemeral(true).queue()
}
