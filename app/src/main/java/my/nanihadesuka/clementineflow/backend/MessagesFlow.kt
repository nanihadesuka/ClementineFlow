package my.nanihadesuka.clementineflow.backend

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import my.nanihadesuka.clementineflow.backend.pb.Remotecontrolmessages
import my.nanihadesuka.clementineflow.ui.utils.utils
import java.time.Instant

class MessagesFlow
{
    class MessageSystem<T>()
    {
        val emitter = MutableSharedFlow<T>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val flow = emitter.debounce(200)
    }

    class MessageData<T>(mapper: (Remotecontrolmessages.Message) -> T)
    {
        val emitter = MutableSharedFlow<Remotecontrolmessages.Message>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val flow = emitter.map { mapper(it) }.debounce(200)
    }

    class MessageUnit
    {
        val emitter = MutableSharedFlow<Remotecontrolmessages.MsgType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val flow = emitter.debounce(200)
    }

    val responseClementineInfo = MessageData { it.responseClementineInfo }
    val responseCurrentMetadata = MessageData { it.responseCurrentMetadata }
    val responsePlaylists = MessageData { it.responsePlaylists }
    val responsePlaylistSongs = MessageData { it.responsePlaylistSongs }
    val responseEngineStateChanged = MessageData { it.responseEngineStateChanged }
    val responseUpdateTrackPosition = MessageData { it.responseUpdateTrackPosition }
    val responseDisconnect = MessageData { it.responseDisconnect }
    val responseActiveChanged = MessageData { it.responseActiveChanged }
    val responseLyrics = MessageData { it.responseLyrics }
    val responseSongFileChunk = MessageData { it.responseSongFileChunk }
    val responseSongOffer = MessageData { it.responseSongOffer }
    val responseLibraryChunk = MessageData { it.responseLibraryChunk }
    val responseDownloadTotalSize = MessageData { it.responseDownloadTotalSize }
    val responseGlobalSearch = MessageData { it.responseGlobalSearch }
    val responseTranscoderStatus = MessageData { it.responseTranscoderStatus }
    val responseGlobalSearchStatus = MessageData { it.responseGlobalSearchStatus }
    val requestSetVolume = MessageData { it.requestSetVolume }

    val repeat = MessageData { it.repeat }
    val shuffle = MessageData { it.shuffle }

    val PLAY = MessageUnit()
    val PLAYPAUSE = MessageUnit()
    val PAUSE = MessageUnit()
    val STOP = MessageUnit()
    val NEXT = MessageUnit()
    val PREVIOUS = MessageUnit()
    val KEEP_ALIVE = MessageUnit()
    val FIRST_DATA_SENT_COMPLETE = MessageUnit()


    enum class SocketConnectionState {
        DISCONNECTED, CONNECTED, CONNECTING
    }

    val socketConnectionState = MessageSystem<SocketConnectionState>()

    enum class SocketConnectionError {
        NONE, ERROR
    }
    val socketConnectionError = MessageSystem<SocketConnectionError>()

    enum class PlayerRunState
    { playing, paused, stopped }

    val playerRunState = listOf(
        PLAY.flow,
        PAUSE.flow,
        STOP.flow,
        responseClementineInfo.flow.map {
            when (it.state)
            {
                Remotecontrolmessages.EngineState.Playing -> Remotecontrolmessages.MsgType.PLAY
                Remotecontrolmessages.EngineState.Paused -> Remotecontrolmessages.MsgType.PAUSE
                else -> Remotecontrolmessages.MsgType.STOP
            }
        }
    ).merge()
        .map {
            when (it)
            {
                Remotecontrolmessages.MsgType.PLAY -> PlayerRunState.playing
                Remotecontrolmessages.MsgType.PAUSE -> PlayerRunState.paused
                else -> PlayerRunState.stopped
            }
        }

    val activePlaylistId = listOf(
        responsePlaylists.flow.mapNotNull { it.playlistList.find { playlist -> playlist.active }?.id },
        responseActiveChanged.flow.map { it.id }
    ).merge()

    enum class ClementineConnectError
    { NONE, WRONG_PASSWORD, NEEDS_PASSWORD }

    val clementineConnectError = listOf(
        responseDisconnect.flow.map {
            when (it.reasonDisconnect)
            {
                Remotecontrolmessages.ReasonDisconnect.Wrong_Auth_Code -> ClementineConnectError.WRONG_PASSWORD
                Remotecontrolmessages.ReasonDisconnect.Not_Authenticated -> ClementineConnectError.NEEDS_PASSWORD
                else -> null
            }
        }.filterNotNull(),
        FIRST_DATA_SENT_COMPLETE.flow.map { ClementineConnectError.NONE }
    ).merge()


    enum class ClementineDisconnectError
    { NONE, SERVER_SHUTDOWN }

    val clementineDisconnectError = listOf(
        responseDisconnect.flow.map {
            when (it.reasonDisconnect)
            {
                Remotecontrolmessages.ReasonDisconnect.Server_Shutdown -> ClementineDisconnectError.SERVER_SHUTDOWN
                else -> null
            }
        }.filterNotNull(),
        FIRST_DATA_SENT_COMPLETE.flow.map { ClementineDisconnectError.NONE }
    ).merge()

    enum class ClementineConnectionState
    {
        DEAD, ALIVE
    }

    @ExperimentalCoroutinesApi
    val clementineConnectionState = run {
        fun seconds() = Instant.now().epochSecond

        val intervalTime: Long = 5 // Server ping time is 10
        val timeAssumeDead: Long = 20

        // ---|-o--------|--------o-|----o-----|----------|---

        val supposedStateCheck = listOf(
            FIRST_DATA_SENT_COMPLETE.flow.map { ClementineConnectionState.ALIVE },
            responseDisconnect.flow.map { ClementineConnectionState.DEAD }
        ).merge()

        val aliveCheck = listOf(
            KEEP_ALIVE.flow,
            FIRST_DATA_SENT_COMPLETE.flow
        ).merge().map { seconds() }

        val intervalCheck = flow {
            emit(seconds())
            while (true)
            {
                delay(1000 * intervalTime)
                emit(seconds())
            }
        }

        combine(
            intervalCheck,
            aliveCheck,
            supposedStateCheck,
        ) { interval, alive, supposedState ->
            when (supposedState)
            {
                ClementineConnectionState.ALIVE ->
                {
                    if (interval - alive > timeAssumeDead)
                        ClementineConnectionState.DEAD
                    else
                        ClementineConnectionState.ALIVE
                }
                ClementineConnectionState.DEAD -> ClementineConnectionState.DEAD
            }
        }.distinctUntilChanged()
    }


    suspend fun pipeMessage(message: Remotecontrolmessages.Message)
    {
        when
        {
            message.hasResponseClementineInfo() -> responseClementineInfo.emitter.emit(message)
            message.hasResponseCurrentMetadata() -> responseCurrentMetadata.emitter.emit(message)
            message.hasResponsePlaylists() -> responsePlaylists.emitter.emit(message)
            message.hasResponsePlaylistSongs() -> responsePlaylistSongs.emitter.emit(message)
            message.hasResponseEngineStateChanged() -> responseEngineStateChanged.emitter.emit(message)
            message.hasResponseUpdateTrackPosition() -> responseUpdateTrackPosition.emitter.emit(message)
            message.hasResponseDisconnect() -> responseDisconnect.emitter.emit(message)
            message.hasResponseActiveChanged() -> responseActiveChanged.emitter.emit(message)
            message.hasResponseLyrics() -> responseLyrics.emitter.emit(message)
            message.hasResponseSongFileChunk() -> responseSongFileChunk.emitter.emit(message)
            message.hasResponseSongOffer() -> responseSongOffer.emitter.emit(message)
            message.hasResponseLibraryChunk() -> responseLibraryChunk.emitter.emit(message)
            message.hasResponseDownloadTotalSize() -> responseDownloadTotalSize.emitter.emit(message)
            message.hasResponseGlobalSearch() -> responseGlobalSearch.emitter.emit(message)
            message.hasResponseTranscoderStatus() -> responseTranscoderStatus.emitter.emit(message)
            message.hasResponseGlobalSearchStatus() -> responseGlobalSearchStatus.emitter.emit(message)
            message.hasRequestSetVolume() -> requestSetVolume.emitter.emit(message)

            message.hasRepeat() -> repeat.emitter.emit(message)
            message.hasShuffle() -> shuffle.emitter.emit(message)

            message.type == Remotecontrolmessages.MsgType.PLAY -> PLAY.emitter.emit(message.type)
            message.type == Remotecontrolmessages.MsgType.PLAYPAUSE -> PLAYPAUSE.emitter.emit(message.type)
            message.type == Remotecontrolmessages.MsgType.PAUSE -> PAUSE.emitter.emit(message.type)
            message.type == Remotecontrolmessages.MsgType.STOP -> STOP.emitter.emit(message.type)
            message.type == Remotecontrolmessages.MsgType.NEXT -> NEXT.emitter.emit(message.type)
            message.type == Remotecontrolmessages.MsgType.KEEP_ALIVE -> KEEP_ALIVE.emitter.emit(message.type)
            message.type == Remotecontrolmessages.MsgType.FIRST_DATA_SENT_COMPLETE -> FIRST_DATA_SENT_COMPLETE.emitter.emit(message.type)
            else -> utils.log("pipeMessage: message not processed", message.type.name)
        }
    }
}