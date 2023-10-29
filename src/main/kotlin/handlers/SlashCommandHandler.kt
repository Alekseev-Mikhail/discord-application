package handlers

import COMMAND_AFK_MODE_NAME
import COMMAND_AFK_MODE_REPLY_DISABLE
import COMMAND_AFK_MODE_REPLY_ENABLE
import COMMAND_AFK_TIME_NAME
import COMMAND_AFK_TIME_REPLY
import COMMAND_CHECK_NAME
import COMMAND_CHECK_REPLY
import COMMAND_CLEAR_NAME
import COMMAND_CLEAR_REPLY
import COMMAND_CONNECT_NAME
import COMMAND_CONNECT_REPLY_PROBLEM_MEMBEROUT
import COMMAND_CONNECT_REPLY_PROBLEM_TYPE
import COMMAND_DEFCHANNEL_NAME
import COMMAND_DEFCHANNEL_REPLY_DISABLE
import COMMAND_DEFCHANNEL_REPLY_ENABLE
import COMMAND_DEFCHANNEL_REPLY_PROBLEM_TYPE
import COMMAND_DISCONNECT_NAME
import COMMAND_DISCONNECT_PROBLEM
import COMMAND_DISCONNECT_REPLY
import COMMAND_INSTRUCTION_NAME
import COMMAND_INSTRUCTION_REPLY
import COMMAND_OPTION_ADDRESS_NAME
import COMMAND_OPTION_CHANNEL_NAME
import COMMAND_OPTION_TIME_NAME
import COMMAND_OPTION_VALUE_NAME
import COMMAND_PLAY_NAME
import COMMAND_PLAY_REPLY
import COMMAND_PLAY_REPLY_PLAYLIST
import COMMAND_PLAY_REPLY_PROBLEM_ALREADY
import COMMAND_PLAY_REPLY_PROBLEM_FAILED
import COMMAND_PLAY_REPLY_PROBLEM_FOUND
import COMMAND_PLAY_REPLY_PROBLEM_MEMBEROUT
import COMMAND_PLAY_REPLY_PROBLEM_NOTHING
import COMMAND_PLAY_REPLY_TRACK
import COMMAND_QUEUE_NAME
import COMMAND_QUEUE_REPLY_PLAYLIST
import COMMAND_QUEUE_REPLY_PROBLEM_FAILED
import COMMAND_QUEUE_REPLY_PROBLEM_FOUND
import COMMAND_QUEUE_REPLY_TRACK
import COMMAND_STATE_AFK_MODE
import COMMAND_STATE_AFK_TIME
import COMMAND_STATE_CHANNEL_CURRENT
import COMMAND_STATE_CHANNEL_DEFAULT
import COMMAND_STATE_NAME
import COMMAND_STATE_TITLE
import COMMAND_STATE_VALUE_DISABLE
import COMMAND_STATE_VALUE_ENABLE
import COMMAND_STATE_VALUE_NULL
import Server
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import notifyInDiscord
import java.lang.StringBuilder
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SlashCommandHandler(
    private val servers: MutableMap<String, Server>,
    private val playerManager: DefaultAudioPlayerManager,
    private val developer: User,
) : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            COMMAND_INSTRUCTION_NAME -> reply(event, COMMAND_INSTRUCTION_REPLY)
            COMMAND_STATE_NAME -> reply(event, state(event))
            COMMAND_CONNECT_NAME -> reply(event, connect(event))
            COMMAND_DISCONNECT_NAME -> reply(event, disconnect(event))
            COMMAND_DEFCHANNEL_NAME -> reply(event, defChannel(event))
            COMMAND_QUEUE_NAME -> queue(event)
            COMMAND_CHECK_NAME -> reply(event, check(event))
            COMMAND_CLEAR_NAME -> reply(event, clear(event))
            COMMAND_PLAY_NAME -> play(event)
            COMMAND_AFK_TIME_NAME -> reply(event, afkTime(event))
            COMMAND_AFK_MODE_NAME -> reply(event, afkMode(event))
        }
    }

    private fun state(event: SlashCommandInteractionEvent): String {
        val server = getServer(event)
        val message = StringBuilder()
        message.append("$COMMAND_STATE_TITLE\n")
        message.append("$COMMAND_STATE_CHANNEL_CURRENT: ${server.currentChannel?.name ?: COMMAND_STATE_VALUE_NULL}\n")
        message.append("$COMMAND_STATE_CHANNEL_DEFAULT: ${server.defaultChannel?.name ?: COMMAND_STATE_VALUE_NULL}\n")
        message.append("$COMMAND_STATE_AFK_MODE: ${if (server.afkMode) COMMAND_STATE_VALUE_ENABLE else COMMAND_STATE_VALUE_DISABLE}\n")
        message.append("$COMMAND_STATE_AFK_TIME: ${server.afkTime.milliseconds.inWholeSeconds}\n")
        return message.toString()
    }

    private fun connect(event: SlashCommandInteractionEvent): String {
        val guild = getGuild(event)
        val server = getServer(guild)
        val option = event.getOption(COMMAND_OPTION_CHANNEL_NAME)

        if (option != null) {
            val channel = option.asChannel
            if (channel.type.isAudio) {
                return server.connect(channel.asAudioChannel())
            }
            return COMMAND_CONNECT_REPLY_PROBLEM_TYPE
        }

        if (server.defaultChannel != null) {
            return server.connect(server.defaultChannel)
        }

        val channel = getMemberChannel(event)
        if (channel != null) {
            return server.connect(channel)
        }
        return COMMAND_CONNECT_REPLY_PROBLEM_MEMBEROUT
    }

    private fun disconnect(event: SlashCommandInteractionEvent): String {
        val guild = getGuild(event)
        val server = getServer(guild)
        if (guild.audioManager.isConnected) {
            server.disconnect()
            return COMMAND_DISCONNECT_REPLY
        }
        return COMMAND_DISCONNECT_PROBLEM
    }

    private fun defChannel(event: SlashCommandInteractionEvent): String {
        val server = getServer(event)
        val option = event.getOption(COMMAND_OPTION_CHANNEL_NAME)

        if (option != null) {
            val channel = option.asChannel
            if (channel.type.isAudio) {
                server.defaultChannel = channel.asAudioChannel()
                return COMMAND_DEFCHANNEL_REPLY_ENABLE
            }
            return COMMAND_DEFCHANNEL_REPLY_PROBLEM_TYPE
        }
        server.defaultChannel = null
        return COMMAND_DEFCHANNEL_REPLY_DISABLE
    }

    private fun queue(event: SlashCommandInteractionEvent) {
        val guild = getGuild(event)
        val server = getServer(event)
        val option = getOption(event, COMMAND_OPTION_ADDRESS_NAME).asString

        playerManager.loadItem(
            option,
            object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) {
                    server.trackQueue.add(track)
                    reply(event, COMMAND_QUEUE_REPLY_TRACK)
                }

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    playlist.tracks.forEach { track ->
                        server.trackQueue.add(track)
                    }
                    reply(event, COMMAND_QUEUE_REPLY_PLAYLIST)
                }

                override fun noMatches() {
                    reply(event, COMMAND_QUEUE_REPLY_PROBLEM_FOUND)
                }

                override fun loadFailed(throwable: FriendlyException) {
                    throwable.notifyInDiscord(guild, developer)
                    reply(event, "$COMMAND_QUEUE_REPLY_PROBLEM_FAILED: ${throwable.message}")
                }
            },
        )
    }

    private fun check(event: SlashCommandInteractionEvent): String =
        getServer(event).trackQueue.joinToString(separator = "\n") { "${it.info.title}: ${it.info.uri}" }
            .ifBlank { COMMAND_CHECK_REPLY }

    private fun clear(event: SlashCommandInteractionEvent): String {
        val server = getServer(event)
        server.player.stopTrack()
        server.trackQueue.clear()
        return COMMAND_CLEAR_REPLY
    }

    private fun play(event: SlashCommandInteractionEvent) {
        val guild = getGuild(event)
        val server = getServer(guild)
        val option = event.getOption(COMMAND_OPTION_ADDRESS_NAME)

        if (server.player.playingTrack != null) {
            reply(event, COMMAND_PLAY_REPLY_PROBLEM_ALREADY)
            return
        }
        if (option != null) {
            playerManager.loadItem(
                option.asString,
                object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        server.trackQueue.add(track)
                        if (connectAndPlay(event, guild, server)) {
                            reply(event, COMMAND_PLAY_REPLY_TRACK)
                            return
                        }
                        reply(event, COMMAND_PLAY_REPLY_PROBLEM_MEMBEROUT)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist) {
                        playlist.tracks.forEach { track ->
                            server.trackQueue.add(track)
                        }
                        if (connectAndPlay(event, guild, server)) {
                            reply(event, COMMAND_PLAY_REPLY_PLAYLIST)
                            return
                        }
                        reply(event, COMMAND_PLAY_REPLY_PROBLEM_MEMBEROUT)
                    }

                    override fun noMatches() {
                        reply(event, COMMAND_PLAY_REPLY_PROBLEM_FOUND)
                    }

                    override fun loadFailed(throwable: FriendlyException) {
                        throwable.notifyInDiscord(guild, developer)
                        reply(event, "$COMMAND_PLAY_REPLY_PROBLEM_FAILED: ${throwable.message}")
                    }
                },
            )
        } else {
            if (server.trackQueue.isEmpty()) {
                reply(event, COMMAND_PLAY_REPLY_PROBLEM_NOTHING)
                return
            }
            if (connectAndPlay(event, guild, server)) {
                reply(event, COMMAND_PLAY_REPLY)
                return
            }
            reply(event, COMMAND_PLAY_REPLY_PROBLEM_MEMBEROUT)
        }
    }

    private fun connectAndPlay(event: SlashCommandInteractionEvent, guild: Guild, server: Server): Boolean {
        if (server.currentChannel == null) {
            val channel = getMemberChannel(event) ?: return false
            server.connect(channel)
        }
        guild.audioManager.sendingHandler = AudioPlayerSendHandler(server.player)
        server.player.playTrack(server.trackQueue.first().makeClone())
        return true
    }

    private fun afkTime(event: SlashCommandInteractionEvent): String {
        val server = getServer(event)
        val time = getOption(event, COMMAND_OPTION_TIME_NAME).asInt
        server.afkTime = time.seconds.inWholeMilliseconds
        return COMMAND_AFK_TIME_REPLY
    }

    private fun afkMode(event: SlashCommandInteractionEvent): String {
        val guild = getGuild(event)
        val server = getServer(guild)
        val option = event.getOption(COMMAND_OPTION_VALUE_NAME)

        if (option != null) {
            return when (option.asBoolean) {
                true -> {
                    server.afkMode = true
                    server.afkMode()
                    COMMAND_AFK_MODE_REPLY_ENABLE
                }

                false -> {
                    server.afkMode = false
                    COMMAND_AFK_MODE_REPLY_DISABLE
                }
            }
        }

        return when (server.afkMode) {
            true -> {
                server.afkMode = false
                COMMAND_AFK_MODE_REPLY_DISABLE
            }

            false -> {
                server.afkMode = true
                server.afkMode()
                COMMAND_AFK_MODE_REPLY_ENABLE
            }
        }
    }
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

    private fun reply(event: SlashCommandInteractionEvent, message: String) =
        event.reply(message).setEphemeral(true).queue()

    private fun getServer(guild: Guild) = servers[guild.id]
        ?: throw NullPointerException("Audio channel was updated in an unknown guild").notifyInDiscord(guild, developer)

    private fun getServer(event: SlashCommandInteractionEvent): Server {
        val guild = getGuild(event)
        return servers[guild.id]
            ?: throw NullPointerException("Audio channel was updated in an unknown guild").notifyInDiscord(
                guild,
                developer,
            )
    }

    private fun getGuild(event: SlashCommandInteractionEvent) =
        event.guild ?: throw NullPointerException("Guild cannot be null").notifyInDiscord(null, developer)

    private fun getOption(event: SlashCommandInteractionEvent, name: String) =
        event.getOption(name) ?: throw NullPointerException("Option cannot be null. Name: $name").notifyInDiscord(
            getGuild(event),
            developer,
        )

    private fun getMemberChannel(event: SlashCommandInteractionEvent): AudioChannelUnion? {
        val guild = getGuild(event)
        val member =
            event.member ?: throw NullPointerException("Member cannot be null").notifyInDiscord(guild, developer)
        val voiceState =
            member.voiceState ?: throw NullPointerException("VoiceState cannot be null").notifyInDiscord(
                guild,
                developer,
            )
        return voiceState.channel
    }
}
