import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.FINISHED

class TrackScheduler(
    private val tracksQueue: MutableList<AudioTrack>,
    private val addressQueue: MutableList<String>,
    private var playerState: PlayerState,
) : AudioEventAdapter() {
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason == FINISHED) {
            if (!playerState.repeat) {
                tracksQueue.remove(track)
                addressQueue.removeAt(0)
                if (tracksQueue.isEmpty()) {
                    playerState.playing = false
                }
                player.playTrack(tracksQueue[0])
            } else {
                tracksQueue.add(1, tracksQueue[0].makeClone())
                tracksQueue.removeAt(0)
                player.playTrack(tracksQueue[0])
            }
        }
    }
}
