import handlers.DirtyEventHandler
import handlers.SlashCommandHandler
import net.dv8tion.jda.api.JDABuilder.createLight
import net.dv8tion.jda.api.entities.Activity.ActivityType
import net.dv8tion.jda.api.entities.Activity.listening
import net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER
import net.dv8tion.jda.api.interactions.commands.build.Commands.slash
import net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT
import net.dv8tion.jda.api.utils.MemberCachePolicy.VOICE
import net.dv8tion.jda.api.utils.cache.CacheFlag.VOICE_STATE

class Startup {
    fun create() {
        val application = Application()
        val dirtyEventHandler = DirtyEventHandler(application)
        val slashCommandHandler = SlashCommandHandler(application)

        ActivityType.CUSTOM_STATUS
        createLight(token)
            .enableIntents(MESSAGE_CONTENT)
            .enableCache(VOICE_STATE)
            .setMemberCachePolicy(VOICE)
            .addEventListeners(slashCommandHandler)
            .addEventListeners(dirtyEventHandler)
            .setActivity(listening("/info"))
            .build()
            .updateCommands()
            .addCommands(
                slash("info", infoDescription),

                slash("connect", connectDescription)
                    .addOption(INTEGER, "channel", optionChannel, false),
                slash("disconnect", disconnectDescription),
                slash("def-channel", defchannelDescription)
                    .addOption(INTEGER, "channel", optionChannel, true),
            ).queue()
    }
}

fun main() {
    Startup().create()
}
