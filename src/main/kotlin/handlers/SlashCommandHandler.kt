package handlers

import Application
import defchannelReplyDisable
import defchannelReplyEnable
import disconnectReply
import disconnectReplyProblem
import infoReply
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import replyProblemExist

class SlashCommandHandler(private val application: Application) : ListenerAdapter() {
//    private val playerManager = DefaultAudioPlayerManager().apply { registerRemoteSources(this) }
//    private val players = mutableMapOf<Guild, AudioPlayer>()
//    private val addressQueue = mutableListOf<String>()
//    private val trackQueue = mutableListOf<AudioTrack>()
//    private val playerState = PlayerState()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "info" -> reply(event, infoReply)
            "connect" -> reply(event, connect(event))
            "disconnect" -> reply(event, disconnect(event))
            "def-channel" -> reply(event, defChannel(event))
        }
    }

    private fun connect(event: SlashCommandInteractionEvent): String {
        val guild = application.getGuild(event)
        val option = event.getOption("channel")

        if (option != null) {
            val index = option.asInt - 1
            if (index == -1) return application.connectToChannel(event)
            if (index >= guild.voiceChannels.size || index < 0) return replyProblemExist
            return application.connectToChannel(guild, guild.getVoiceChannelById(guild.voiceChannels[index].id))
        }

        if (application.defaultChannel != null) {
            return application.connectToChannel(guild, application.defaultChannel)
        }

        return application.connectToChannel(event)
    }

    private fun disconnect(event: SlashCommandInteractionEvent): String {
        val guild = application.getGuild(event)
        if (guild.audioManager.isConnected) {
            application.disconnectToChannel(guild)
            return disconnectReply
        }
        return disconnectReplyProblem
    }

    private fun defChannel(event: SlashCommandInteractionEvent): String {
        val guild = application.getGuild(event)
        val option = application.getOption(event, "channel")
        val index = option.asInt - 1
        if (index < -1 || index >= guild.voiceChannels.size) return replyProblemExist
        if (index == -1) {
            application.defaultChannel = null
            return defchannelReplyDisable
        }
        application.defaultChannel = guild.voiceChannels[index]
        return defchannelReplyEnable
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

    private fun reply(event: SlashCommandInteractionEvent, reply: String) =
        event.reply(reply).setEphemeral(true).queue()
}
