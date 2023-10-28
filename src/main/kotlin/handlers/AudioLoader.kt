package handlers

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

abstract class AudioLoader : AudioLoadResultHandler {
    override fun trackLoaded(track: AudioTrack) {}

    override fun playlistLoaded(playlist: AudioPlaylist) {}

    override fun noMatches() {}

    override fun loadFailed(exception: FriendlyException) {}
}
