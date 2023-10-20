import handlers.SlashCommandHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
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
val GUILD_ID = getProperty("app.guild")

val COMMAND_OPTION_CHANNEL_NAME = getProperty("command.option.channel.name")
val COMMAND_OPTION_VALUE_NAME = getProperty("command.option.value.name")
val COMMAND_OPTION_TIME_NAME = getProperty("command.option.time.name")

val COMMAND_OPTION_CHANNEL_DESCRIPTION = getProperty("command.option.channel.description")
val COMMAND_OPTION_VALUE_DESCRIPTION = getProperty("command.option.value.description")
val COMMAND_OPTION_TIME_DESCRIPTION = getProperty("command.option.time.description")

val COMMAND_INFO_NAME = getProperty("command.info.name")
val COMMAND_INFO_DESCRIPTION = getProperty("command.info.description")
val COMMAND_INFO_REPLY = getProperty("command.info.reply")

val COMMAND_STATE_NAME = getProperty("command.state.name")
val COMMAND_STATE_DESCRIPTION = getProperty("command.state.description")
val COMMAND_STATE_TITLE = getProperty("command.state.title")
val COMMAND_STATE_CHANNEL_DEFAULT = getProperty("command.state.channel.default")
val COMMAND_STATE_CHANNEL_CURRENT = getProperty("command.state.channel.current")
val COMMAND_STATE_AFK_MODE = getProperty("command.state.afk.mode")
val COMMAND_STATE_AFK_TIME = getProperty("command.state.afk.time")
val COMMAND_STATE_VALUE_NULL = getProperty("command.state.value.null")
val COMMAND_STATE_VALUE_ENABLE = getProperty("command.state.value.enable")
val COMMAND_STATE_VALUE_DISABLE = getProperty("command.state.value.disable")

val COMMAND_CONNECT_NAME = getProperty("command.connect.name")
val COMMAND_CONNECT_DESCRIPTION = getProperty("command.connect.description")
val COMMAND_CONNECT_REPLY = getProperty("command.connect.reply")
val COMMAND_CONNECT_REPLY_PROBLEM_ALREADY = getProperty("command.connect.reply.problem.already")
val COMMAND_CONNECT_REPLY_PROBLEM_MEMBEROUT = getProperty("command.connect.reply.problem.memberout")
val COMMAND_CONNECT_REPLY_PROBLEM_TYPE = getProperty("command.connect.reply.problem.type")

val COMMAND_DISCONNECT_NAME = getProperty("command.disconnect.name")
val COMMAND_DISCONNECT_DESCRIPTION = getProperty("command.disconnect.description")
val COMMAND_DISCONNECT_REPLY = getProperty("command.disconnect.reply")
val COMMAND_DISCONNECT_PROBLEM = getProperty("command.disconnect.reply.problem")

val COMMAND_DEFCHANNEL_NAME = getProperty("command.defchannel.name")
val COMMAND_DEFCHANNEL_DESCRIPTION = getProperty("command.defchannel.description")
val COMMAND_DEFCHANNEL_REPLY_ENABLE = getProperty("command.defchannel.reply.enable")
val COMMAND_DEFCHANNEL_REPLY_DISABLE = getProperty("command.defchannel.reply.disable")
val COMMAND_DEFCHANNEL_REPLY_PROBLEM_TYPE = getProperty("command.defchannel.reply.problem.type")

val COMMAND_AFK_TIME_NAME = getProperty("command.afk.time.name")
val COMMAND_AFK_TIME_DESCRIPTION = getProperty("command.afk.time.description")
val COMMAND_AFK_TIME_REPLY = getProperty("command.afk.time.reply")

val COMMAND_AFK_MODE_NAME = getProperty("command.afk.mode.name")
val COMMAND_AFK_MODE_DESCRIPTION = getProperty("command.afk.mode.description")
val COMMAND_AFK_MODE_REPLY_ENABLE = getProperty("command.afk.mode.reply.enable")
val COMMAND_AFK_MODE_REPLY_DISABLE = getProperty("command.afk.mode.reply.disable")

class Application(private val jda: JDA) {
    val developer: User = jda.retrieveUserById(DEVELOPER_ID).complete()
    private var exitJob: Job? = null

    var defaultChannel: AudioChannel? = null
    var currentChannel: AudioChannel? = null
    var afkMode = true
    var afkTime = 10.seconds.inWholeMilliseconds

    fun save() {
        val path = "$PATH/app.json"
        val file = FileWriter(path)
        val string = Json.encodeToString(ApplicationInfo(defaultChannel?.id ?: "", afkMode, afkTime))
        file.write(string)
        file.close()
    }

    fun read() {
        val file = File("$PATH/app.json")
        if (file.exists()) {
            val info = Json.decodeFromString<ApplicationInfo>(Scanner(file).nextLine())
            val defaultGuild = getDefaultGuild()
            if (info.defaultChannelId.isNotEmpty()) {
                defaultChannel =
                    defaultGuild.getVoiceChannelById(info.defaultChannelId)
            }
            afkMode = info.afkMode
            afkTime = info.afkTime
        }
    }

    fun connect(guild: Guild, channel: AudioChannel?): String {
        if (channel == null) throw NullPointerException("Channel cannot be null").notifyInDiscord()
        cancelExitJob()
        if (channel == currentChannel) return COMMAND_CONNECT_REPLY_PROBLEM_ALREADY
        guild.audioManager.openAudioConnection(channel)
        currentChannel = channel
        return COMMAND_CONNECT_REPLY
    }

    fun connect(event: SlashCommandInteractionEvent): String {
        cancelExitJob()
        val channel = getMemberChannel(event)
        if (channel != null) {
            return connect(getGuild(event), channel)
        }
        return COMMAND_CONNECT_REPLY_PROBLEM_MEMBEROUT
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

    fun checkDefaultGuild(guild: Guild) {
        val defaultGuild = getDefaultGuild()
        if (guild.id != GUILD_ID) throw IllegalArgumentException("Event from another guild. Default Guild Name: ${defaultGuild.name}. Current Guild Name: ${guild.name}").notifyInDiscord()
    }

    fun getGuild(event: SlashCommandInteractionEvent) =
        event.guild ?: throw NullPointerException("Guild cannot be null").notifyInDiscord()

    fun getOption(event: SlashCommandInteractionEvent, name: String) =
        event.getOption(name) ?: throw NullPointerException("Option cannot be null. Name: $name").notifyInDiscord()

    private fun getDefaultGuild() =
        jda.getGuildById(GUILD_ID)
            ?: throw NullPointerException("Default Guild is invalid. Id: $GUILD_ID").notifyInDiscord()

    private fun getMemberChannel(event: SlashCommandInteractionEvent): AudioChannelUnion? {
        val member = event.member ?: throw NullPointerException("Member cannot be null").notifyInDiscord()
        val voiceState = member.voiceState ?: throw NullPointerException("VoiceState cannot be null").notifyInDiscord()
        return voiceState.channel
    }

    private fun Exception.notifyInDiscord(): Exception {
        developer.openPrivateChannel().queue { channel ->
            channel.sendMessage("Oups! Something went wrong! Message: $message").queue()
        }
        return this
    }

    private fun cancelExitJob() {
        val job = exitJob
        if (job != null) {
            job.cancel()
            exitJob = null
        }
    }
}

fun getProperty(key: String): String =
    PROPERTIES.getProperty(key) ?: throw NullPointerException("Key is invalid: $key")
