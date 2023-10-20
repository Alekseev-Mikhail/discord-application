import handlers.DirtyEventHandler
import handlers.SlashCommandHandler
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission.ADMINISTRATOR
import net.dv8tion.jda.api.entities.Activity.listening
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor
import net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN
import net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL
import net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER
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
        .setActivity(listening("/$COMMAND_INFO_NAME"))
        .build()

    val application = Application(jda)
    val dirtyEventHandler = DirtyEventHandler(application)
    val slashCommandHandler = SlashCommandHandler(application)

    jda.addEventListener(dirtyEventHandler, slashCommandHandler)
    jda.updateCommands()
        .addCommands(
            slash(COMMAND_INFO_NAME, COMMAND_INFO_DESCRIPTION),
            slash(COMMAND_STATE_NAME, COMMAND_STATE_DESCRIPTION),

            slash(COMMAND_CONNECT_NAME, COMMAND_CONNECT_DESCRIPTION)
                .addOption(CHANNEL, COMMAND_OPTION_CHANNEL_NAME, COMMAND_OPTION_CHANNEL_DESCRIPTION, false),
            slash(COMMAND_DISCONNECT_NAME, COMMAND_DISCONNECT_DESCRIPTION),
            slash(COMMAND_DEFCHANNEL_NAME, COMMAND_DEFCHANNEL_DESCRIPTION)
                .addOption(CHANNEL, COMMAND_OPTION_CHANNEL_NAME, COMMAND_OPTION_CHANNEL_DESCRIPTION, false),

            slash(COMMAND_AFK_TIME_NAME, COMMAND_AFK_TIME_DESCRIPTION)
                .addOption(INTEGER, COMMAND_OPTION_TIME_NAME, COMMAND_OPTION_TIME_DESCRIPTION, true)
                .setDefaultPermissions(enabledFor(ADMINISTRATOR)),
            slash(COMMAND_AFK_MODE_NAME, COMMAND_AFK_MODE_DESCRIPTION)
                .addOption(BOOLEAN, COMMAND_OPTION_VALUE_NAME, COMMAND_OPTION_VALUE_DESCRIPTION, false)
                .setDefaultPermissions(enabledFor(ADMINISTRATOR)),
        )
        .queue()
}
