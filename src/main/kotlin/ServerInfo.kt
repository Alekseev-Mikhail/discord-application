import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class ServerInfo(
    val defaultChannelId: String? = null,
    val afkMode: Boolean = true,
    val afkTime: Long = 10.seconds.inWholeMilliseconds,
    val addressQueue: List<String> = emptyList(),
)
