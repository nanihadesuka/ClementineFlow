package my.nanihadesuka.clementineflow.backend

import my.nanihadesuka.clementineflow.backend.pb.Remotecontrolmessages
import my.nanihadesuka.clementineflow.backend.pb.Remotecontrolmessages.*

object MessageBuilder
{
    fun <A> MsgType.message(specificBuilder: Message.Builder.() -> A, modify: A.() -> Unit = {}): Message
    {
        val builder = Message.newBuilder().also {
            it.version = it.defaultInstanceForType.version
            it.type = this
        }
        modify(specificBuilder(builder))
        return builder.build()
    }

    fun MsgType.message(): Message
    {
        val builder = Message.newBuilder().also {
            it.version = it.defaultInstanceForType.version
            it.type = this
        }
        return builder.build()
    }

    fun requestConnect(password: Int? = null) = MsgType.CONNECT.message({
        requestConnectBuilder
    }) {
        this.downloader = false
        this.sendPlaylistSongs = true
        if (password != null)
            this.authCode = password
    }

    fun requestSetTrackPosition(position: Int) = MsgType.SET_TRACK_POSITION.message({
        requestSetTrackPositionBuilder
    }) {
        this.position = position
    }

    fun previous() = MsgType.PREVIOUS.message()
    fun playPause() = MsgType.PLAYPAUSE.message()
    fun next() = MsgType.NEXT.message()

    fun repeat(mode: RepeatMode) = MsgType.REPEAT.message({ repeatBuilder }) {
        this.repeatMode = mode
    }

    fun shuffle(mode: ShuffleMode) = MsgType.SHUFFLE.message({ shuffleBuilder }) {
        this.shuffleMode = mode
    }

    fun requestPlaylistSongs(playlistId: Int) = MsgType.REQUEST_PLAYLIST_SONGS.message({
        requestPlaylistSongsBuilder
    }) {
        this.id = playlistId
    }

    fun changeSong(playlistId: Int, songIndex:Int) = MsgType.CHANGE_SONG.message({
        requestChangeSongBuilder
    }) {
        this.playlistId = playlistId
        this.songIndex = songIndex
    }

    fun closePlaylist(playlistId: Int) = MsgType.CLOSE_PLAYLIST.message({
        requestClosePlaylistBuilder
    }) {
        this.playlistId = playlistId
    }

    fun setVolume(volume: Int) = MsgType.SET_VOLUME.message({
        requestSetVolumeBuilder
    }) {
        this.volume = volume
    }

    fun disconnect() = MsgType.DISCONNECT.message()
}