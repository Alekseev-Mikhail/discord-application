import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import handler.DirtyEventHandler
import handler.SlashCommandHandler
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission.ADMINISTRATOR
import net.dv8tion.jda.api.entities.Activity.listening
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor
import net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN
import net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL
import net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands.slash
import net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MEMBERS
import net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT
import net.dv8tion.jda.api.utils.MemberCachePolicy.VOICE
import net.dv8tion.jda.api.utils.cache.CacheFlag.VOICE_STATE

fun main() {
    val jda = JDABuilder.createLight(TOKEN)
        .enableIntents(MESSAGE_CONTENT)
        .enableIntents(GUILD_MEMBERS)
        .enableCache(VOICE_STATE)
        .setMemberCachePolicy(VOICE)
        .setActivity(listening("/$COMMAND_INSTRUCTION_NAME"))
        .build()

    val developer = jda.retrieveUserById(DEVELOPER_ID).complete()
    val servers = mutableMapOf<String, Server>()
    val playerManager = DefaultAudioPlayerManager().apply { AudioSourceManagers.registerRemoteSources(this) }
    val dirtyEventHandler = DirtyEventHandler(servers, playerManager, developer)
    val slashCommandHandler = SlashCommandHandler(servers, playerManager, developer)

    jda.addEventListener(dirtyEventHandler, slashCommandHandler)
    jda.updateCommands()
        .addCommands(
            slash(COMMAND_INSTRUCTION_NAME, COMMAND_INSTRUCTION_DESCRIPTION),
            slash(COMMAND_STATE_NAME, COMMAND_STATE_DESCRIPTION),

            slash(COMMAND_CONNECT_NAME, COMMAND_CONNECT_DESCRIPTION)
                .addOption(CHANNEL, COMMAND_OPTION_CHANNEL_NAME, COMMAND_OPTION_CHANNEL_DESCRIPTION, false),
            slash(COMMAND_DISCONNECT_NAME, COMMAND_DISCONNECT_DESCRIPTION),
            slash(COMMAND_DEFCHANNEL_NAME, COMMAND_DEFCHANNEL_DESCRIPTION)
                .addOption(CHANNEL, COMMAND_OPTION_CHANNEL_NAME, COMMAND_OPTION_CHANNEL_DESCRIPTION, false),

            slash(COMMAND_QUEUE_NAME, COMMAND_QUEUE_DESCRIPTION)
                .addOption(STRING, COMMAND_OPTION_ADDRESS_NAME, COMMAND_OPTION_ADDRESS_DESCRIPTION, true),
            slash(COMMAND_CHECK_NAME, COMMAND_CHECK_DESCRIPTION),
            slash(COMMAND_CLEAR_NAME, COMMAND_CLEAR_DESCRIPTION),
            slash(COMMAND_PLAY_NAME, COMMAND_PLAY_DESCRIPTION)
                .addOption(STRING, COMMAND_OPTION_ADDRESS_NAME, COMMAND_OPTION_ADDRESS_DESCRIPTION, false),

            slash(COMMAND_AFK_TIME_NAME, COMMAND_AFK_TIME_DESCRIPTION)
                .addOption(INTEGER, COMMAND_OPTION_TIME_NAME, COMMAND_OPTION_TIME_DESCRIPTION, true)
                .setDefaultPermissions(enabledFor(ADMINISTRATOR)),
            slash(COMMAND_AFK_MODE_NAME, COMMAND_AFK_MODE_DESCRIPTION)
                .addOption(BOOLEAN, COMMAND_OPTION_VALUE_NAME, COMMAND_OPTION_VALUE_DESCRIPTION, false)
                .setDefaultPermissions(enabledFor(ADMINISTRATOR)),
        )
        .queue()
}
