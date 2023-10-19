import kotlinx.serialization.Serializable

@Serializable
data class ApplicationInfo(
    val defaultChannelId: String,
    val afkMode: Boolean,
    val afkTime: Long,
)
