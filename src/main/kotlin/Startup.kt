import handlers.DirtyEventHandler
import handlers.SlashCommandHandler
import net.dv8tion.jda.api.JDABuilder.createLight
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
    val application = Application()
    val dirtyEventHandler = DirtyEventHandler(application)
    val slashCommandHandler = SlashCommandHandler(application)

    createLight(token)
        .enableIntents(MESSAGE_CONTENT)
        .enableIntents(GUILD_MEMBERS)
        .enableCache(VOICE_STATE)
        .setMemberCachePolicy(VOICE)
        .addEventListeners(slashCommandHandler)
        .addEventListeners(dirtyEventHandler)
        .setActivity(listening("/$infoName"))
        .build()
        .updateCommands()
        .addCommands(
            slash(infoName, infoDescription),
            slash(stateName, stateDescription),

            slash(connectName, connectDescription)
                .addOption(CHANNEL, optionChannelName, optionChannelDescription, false),
            slash(disconnectName, disconnectDescription),
            slash(defchannelName, defchannelDescription)
                .addOption(CHANNEL, optionChannelName, optionChannelDescription, false),
            slash(afkTimeName, afkTimeDescription)
                .addOption(INTEGER, optionTimeName, optionTimeDescription, true),

            slash(afkModeName, afkModeDescription)
                .addOption(BOOLEAN, optionValueName, optionValueDescription, false)
                .setDefaultPermissions(enabledFor(ADMINISTRATOR)),
        )
        .queue()
}
