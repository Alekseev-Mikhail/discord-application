import handlers.SlashCommandHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

private val propertiesPath: String =
    File(SlashCommandHandler::class.java.protectionDomain.codeSource.location.path).parentFile.absolutePath
private val properties =
    Properties().apply { load(InputStreamReader(FileInputStream(getDevPath(propertiesPath)), "UTF-8")) }

val token = getProperty("app.test1.token")

val replyProblemExist = getProperty("command.reply.problem.exist")
val optionChannel = getProperty("command.option.channel")
val optionValue = getProperty("command.option.value")

val infoDescription = getProperty("command.info.description")
val infoReply = getProperty("command.info.reply")

val connectDescription = getProperty("command.connect.description")
val connectReply = getProperty("command.connect.reply")
val connectReplyProblemAlready = getProperty("command.connect.reply.problem.already")
val connectReplyProblemMemberout = getProperty("command.connect.reply.problem.memberout")

val disconnectDescription = getProperty("command.disconnect.description")
val disconnectReply = getProperty("command.disconnect.reply")
val disconnectReplyProblem = getProperty("command.disconnect.reply.problem")

val defchannelDescription = getProperty("command.defchannel.description")
val defchannelReplyEnable = getProperty("command.defchannel.reply.enable")
val defchannelReplyDisable = getProperty("command.defchannel.reply.disable")

val afkDescription = getProperty("command.afk.description")

class Application {
    var defaultChannel: AudioChannel? = null
    var currentChannel: AudioChannel? = null

    fun connectToChannel(guild: Guild, channel: AudioChannel?): String {
        if (channel == null) throw NullPointerException("Channel cannot be null")
        if (channel == currentChannel) return connectReplyProblemAlready
        guild.audioManager.openAudioConnection(channel)
        currentChannel = channel
        return connectReply
    }

    fun connectToChannel(event: SlashCommandInteractionEvent): String {
        val channel = getMemberChannel(event)
        if (channel != null) {
            return connectToChannel(getGuild(event), channel)
        }
        return connectReplyProblemMemberout
    }

    fun disconnectToChannel(guild: Guild) {
        guild.audioManager.closeAudioConnection()
        currentChannel = null
    }

    fun getGuild(event: SlashCommandInteractionEvent) =
        event.guild ?: throw NullPointerException("Guild cannot be null")

    fun getOption(event: SlashCommandInteractionEvent, name: String) =
        event.getOption(name) ?: throw NullPointerException("Option cannot be null")

    fun getMemberChannel(event: SlashCommandInteractionEvent): AudioChannelUnion? {
        val member = event.member ?: throw NullPointerException("Member cannot be null")
        val voiceState = member.voiceState ?: throw NullPointerException("VoiceState cannot be null")
        return voiceState.channel
    }
}

private fun getProperty(key: String): String =
    properties.getProperty(key) ?: throw NullPointerException("Nothing found: $key")

private fun getDeployPath(path: String) = "$path/../../app.properties"

private fun getDevPath(path: String) = "$path/../../distributions/app.properties"
