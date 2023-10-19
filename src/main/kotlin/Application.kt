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
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion
import net.dv8tion.jda.api.events.Event
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

val load = getProperty("command.load")
val replyProblemType = getProperty("command.reply.problem.type")

val optionChannelName = getProperty("command.option.channel.name")
val optionValueName = getProperty("command.option.value.name")
val optionTimeName = getProperty("command.option.time.name")

val optionChannelDescription = getProperty("command.option.channel.description")
val optionValueDescription = getProperty("command.option.value.description")
val optionTimeDescription = getProperty("command.option.time.description")

val infoName = getProperty("command.info.name")
val infoDescription = getProperty("command.info.description")
val infoReply = getProperty("command.info.reply")

val stateName = getProperty("command.state.name")
val stateDescription = getProperty("command.state.description")
val stateTitle = getProperty("command.state.title")
val stateNull = getProperty("command.state.null")
val stateDefChannel = getProperty("command.state.defchannel")
val stateCurChannel = getProperty("command.state.curchannel")
val stateAfkMode = getProperty("command.state.afk.mode")
val stateAfkModeEnable = getProperty("command.state.afk.mode.enable")
val stateAfkModeDisable = getProperty("command.state.afk.mode.disable")
val stateAfkTime = getProperty("command.state.afk.time")

val connectName = getProperty("command.connect.name")
val connectDescription = getProperty("command.connect.description")
val connectReply = getProperty("command.connect.reply")
val connectReplyProblemAlready = getProperty("command.connect.reply.problem.already")
val connectReplyProblemMemberout = getProperty("command.connect.reply.problem.memberout")

val disconnectName = getProperty("command.disconnect.name")
val disconnectDescription = getProperty("command.disconnect.description")
val disconnectReply = getProperty("command.disconnect.reply")
val disconnectReplyProblem = getProperty("command.disconnect.reply.problem")

val defchannelName = getProperty("command.defchannel.name")
val defchannelDescription = getProperty("command.defchannel.description")
val defchannelReplyEnable = getProperty("command.defchannel.reply.enable")
val defchannelReplyDisable = getProperty("command.defchannel.reply.disable")

val afkTimeName = getProperty("command.afk.time.name")
val afkTimeDescription = getProperty("command.afk.time.description")
val afkTimeReply = getProperty("command.afk.time.reply")

val afkModeName = getProperty("command.afk.mode.name")
val afkModeDescription = getProperty("command.afk.mode.description")
val afkModeReplyEnable = getProperty("command.afk.mode.reply.enable")
val afkModeReplyDisable = getProperty("command.afk.mode.reply.disable")

class Application {
    var developer: User? = null
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

    fun read(event: Event) {
        val file = File("$PATH/app.json")
        if (file.exists()) {
            val info = Json.decodeFromString<ApplicationInfo>(Scanner(file).nextLine())
            val defaultGuild = getDefaultGuild(event)
            if (info.defaultChannelId.isNotEmpty()) defaultChannel = defaultGuild.getVoiceChannelById(info.defaultChannelId)
            afkMode = info.afkMode
            afkTime = info.afkTime
        }
    }

    fun findDeveloper(event: Event) {
        developer = event.jda.retrieveUserById(DEVELOPER_ID).complete()
    }

    fun connect(guild: Guild, channel: AudioChannel?): String {
        if (channel == null) throw NullPointerException("Channel cannot be null").notifyInDiscord()
        cancelExitJob()
        if (channel == currentChannel) return connectReplyProblemAlready
        guild.audioManager.openAudioConnection(channel)
        currentChannel = channel
        return connectReply
    }

    fun connect(event: SlashCommandInteractionEvent): String {
        cancelExitJob()
        val channel = getMemberChannel(event)
        if (channel != null) {
            return connect(getGuild(event), channel)
        }
        return connectReplyProblemMemberout
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

    fun checkCurrentGuild(event: Event, guild: Guild) {
        val currentGuild = getDefaultGuild(event)
        if (guild.id != GUILD_ID) throw IllegalArgumentException("Event from another guild. Default Guild Name: ${currentGuild.name}. Current Guild Name: ${guild.name}").notifyInDiscord()
    }

    fun getDefaultGuild(event: Event) =
        event.jda.getGuildById(GUILD_ID)
            ?: throw NullPointerException("Default Guild is invalid. Id: $GUILD_ID").notifyInDiscord()

    fun getGuild(event: SlashCommandInteractionEvent) =
        event.guild ?: throw NullPointerException("Guild cannot be null").notifyInDiscord()

    fun getOption(event: SlashCommandInteractionEvent, name: String) =
        event.getOption(name) ?: throw NullPointerException("Option cannot be null. Name: $name").notifyInDiscord()

    private fun getMemberChannel(event: SlashCommandInteractionEvent): AudioChannelUnion? {
        val member = event.member ?: throw NullPointerException("Member cannot be null").notifyInDiscord()
        val voiceState = member.voiceState ?: throw NullPointerException("VoiceState cannot be null").notifyInDiscord()
        return voiceState.channel
    }

    private fun Exception.notifyInDiscord(): Exception {
        developer?.openPrivateChannel()?.queue { channel ->
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
