import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val defaultChannelId: String,
    val afkMode: Boolean,
    val afkTime: Long,
    val addressQueue: List<String>,
)
