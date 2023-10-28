
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import handlers.AudioLoader
import handlers.SlashCommandHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.Properties
import java.util.Scanner
import kotlin.time.Duration.Companion.seconds

private val DEPLOY_PATH =
    "${File(SlashCommandHandler::class.java.protectionDomain.codeSource.location.path).parentFile.absolutePath}/../.."
private const val DEV_PATH = "src/main/resources"
const val PATH = DEV_PATH
val PROPERTIES = Properties().apply { load(InputStreamReader(FileInputStream("$PATH/app.properties"), "UTF-8")) }

val DEVELOPER_ID = getProperty("app.developer")
val TOKEN = getProperty("app.token")

val COMMAND_OPTION_CHANNEL_NAME = getProperty("command.option.channel.name")
val COMMAND_OPTION_VALUE_NAME = getProperty("command.option.value.name")
val COMMAND_OPTION_TIME_NAME = getProperty("command.option.time.name")
val COMMAND_OPTION_ADDRESS_NAME = getProperty("command.option.address.name")

val COMMAND_OPTION_CHANNEL_DESCRIPTION = getProperty("command.option.channel.description")
val COMMAND_OPTION_VALUE_DESCRIPTION = getProperty("command.option.value.description")
val COMMAND_OPTION_TIME_DESCRIPTION = getProperty("command.option.time.description")
val COMMAND_OPTION_ADDRESS_DESCRIPTION = getProperty("command.option.address.description")

const val COMMAND_INSTRUCTION_NAME = "instruction"
val COMMAND_INSTRUCTION_DESCRIPTION = getProperty("command.instruction.description")
val COMMAND_INSTRUCTION_REPLY = getProperty("command.instruction.reply")

const val COMMAND_STATE_NAME = "state"
val COMMAND_STATE_DESCRIPTION = getProperty("command.state.description")
val COMMAND_STATE_TITLE = getProperty("command.state.title")
val COMMAND_STATE_CHANNEL_DEFAULT = getProperty("command.state.channel.default")
val COMMAND_STATE_CHANNEL_CURRENT = getProperty("command.state.channel.current")
val COMMAND_STATE_AFK_MODE = getProperty("command.state.afk.mode")
val COMMAND_STATE_AFK_TIME = getProperty("command.state.afk.time")
val COMMAND_STATE_VALUE_NULL = getProperty("command.state.value.null")
val COMMAND_STATE_VALUE_ENABLE = getProperty("command.state.value.enable")
val COMMAND_STATE_VALUE_DISABLE = getProperty("command.state.value.disable")

const val COMMAND_CONNECT_NAME = "connect"
val COMMAND_CONNECT_DESCRIPTION = getProperty("command.connect.description")
val COMMAND_CONNECT_REPLY = getProperty("command.connect.reply")
val COMMAND_CONNECT_REPLY_PROBLEM_ALREADY = getProperty("command.connect.reply.problem.already")
val COMMAND_CONNECT_REPLY_PROBLEM_MEMBEROUT = getProperty("command.connect.reply.problem.memberout")
val COMMAND_CONNECT_REPLY_PROBLEM_TYPE = getProperty("command.connect.reply.problem.type")

const val COMMAND_DISCONNECT_NAME = "disconnect"
val COMMAND_DISCONNECT_DESCRIPTION = getProperty("command.disconnect.description")
val COMMAND_DISCONNECT_REPLY = getProperty("command.disconnect.reply")
val COMMAND_DISCONNECT_PROBLEM = getProperty("command.disconnect.reply.problem")

const val COMMAND_DEFCHANNEL_NAME = "def-channel"
val COMMAND_DEFCHANNEL_DESCRIPTION = getProperty("command.defchannel.description")
val COMMAND_DEFCHANNEL_REPLY_ENABLE = getProperty("command.defchannel.reply.enable")
val COMMAND_DEFCHANNEL_REPLY_DISABLE = getProperty("command.defchannel.reply.disable")
val COMMAND_DEFCHANNEL_REPLY_PROBLEM_TYPE = getProperty("command.defchannel.reply.problem.type")

const val COMMAND_QUEUE_NAME = "queue"
val COMMAND_QUEUE_DESCRIPTION = getProperty("command.queue.description")
val COMMAND_QUEUE_REPLY_TRACK = getProperty("command.queue.reply.track")
val COMMAND_QUEUE_REPLY_PLAYLIST = getProperty("command.queue.reply.playlist")
val COMMAND_QUEUE_REPLY_PROBLEM_FOUND = getProperty("command.queue.reply.problem.found")
val COMMAND_QUEUE_REPLY_PROBLEM_FAILED = getProperty("command.queue.reply.problem.failed")

const val COMMAND_CHECK_NAME = "check"
val COMMAND_CHECK_DESCRIPTION = getProperty("command.check.description")
val COMMAND_CHECK_REPLY = getProperty("command.check.reply")

const val COMMAND_AFK_TIME_NAME = "afk-time"
val COMMAND_AFK_TIME_DESCRIPTION = getProperty("command.afk.time.description")
val COMMAND_AFK_TIME_REPLY = getProperty("command.afk.time.reply")

const val COMMAND_AFK_MODE_NAME = "afk-mode"
val COMMAND_AFK_MODE_DESCRIPTION = getProperty("command.afk.mode.description")
val COMMAND_AFK_MODE_REPLY_ENABLE = getProperty("command.afk.mode.reply.enable")
val COMMAND_AFK_MODE_REPLY_DISABLE = getProperty("command.afk.mode.reply.disable")

class Server(guild: Guild, playerManager: DefaultAudioPlayerManager, private val developer: User) {
    private val player = playerManager.createPlayer()
    private var exitJob: Job? = null

    var defaultChannel: AudioChannel? = null
    var currentChannel: AudioChannel? = null
    var afkMode = true
    var afkTime = 10.seconds.inWholeMilliseconds
    val trackQueue = mutableListOf<AudioTrack>()

    init {
        val file = File("$PATH/${guild.id}/app.json")
        if (file.exists()) {
            val info = Json.decodeFromString<ServerInfo>(Scanner(file).nextLine())
            if (info.defaultChannelId.isNotEmpty()) {
                defaultChannel = guild.getVoiceChannelById(info.defaultChannelId)
            }
            afkMode = info.afkMode
            afkTime = info.afkTime
            info.addressQueue.forEach { address ->
                playerManager.loadItem(
                    address,
                    object : AudioLoader() {
                        override fun trackLoaded(track: AudioTrack) {
                            trackQueue.add(track)
                        }

                        override fun playlistLoaded(playlist: AudioPlaylist) {
                            playlist.tracks.forEach { track ->
                                trackQueue.add(track)
                            }
                        }

                        override fun loadFailed(exception: FriendlyException) {
                            exception.notifyInDiscord(guild, developer)
                        }
                    },
                )
            }
        }
    }

    fun save(id: String) {
        val path = "$PATH/$id"
        touchDirectory(path)
        val file = FileWriter("$path/app.json")
        val addressQueue = mutableListOf<String>()
        trackQueue.forEach { track -> addressQueue.add(track.info.uri) }
        val string = Json.encodeToString(ServerInfo(defaultChannel?.id ?: "", afkMode, afkTime, addressQueue))
        file.write(string)
        file.close()
    }

    fun connect(guild: Guild, channel: AudioChannel?): String {
        if (channel == null) throw NullPointerException("Channel cannot be null").notifyInDiscord(guild, developer)
        cancelExitJob()
        if (channel == currentChannel) return COMMAND_CONNECT_REPLY_PROBLEM_ALREADY
        guild.audioManager.openAudioConnection(channel)
        currentChannel = channel
        return COMMAND_CONNECT_REPLY
    }

    fun disconnect(guild: Guild) {
        cancelExitJob()
        guild.audioManager.closeAudioConnection()
        currentChannel = null
    }

    fun isAlone(): Boolean {
        val currentChannel = this.currentChannel
        return if (currentChannel != null) currentChannel.members.size <= 1 else false
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun afkMode(guild: Guild) {
        exitJob = GlobalScope.launch {
            coroutineScope {
                delay(afkTime)
                if (afkMode && isAlone()) disconnect(guild)
            }
        }
    }

    private fun cancelExitJob() {
        val job = exitJob
        if (job != null) {
            job.cancel()
            exitJob = null
        }
    }
}

fun Exception.notifyInDiscord(guild: Guild?, developer: User): Exception {
    developer.openPrivateChannel().queue { channel ->
        channel.sendMessage("Oups! Something went wrong! Guild Name: ${guild?.name} Message: $message").queue()
    }
    return this
}

fun touchDirectory(path: String) {
    if (!File(path).exists()) {
        File(path).mkdir()
    }
}

fun getProperty(key: String): String =
    PROPERTIES.getProperty(key) ?: throw NullPointerException("Key is invalid: $key")