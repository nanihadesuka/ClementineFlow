package my.nanihadesuka.clementineflow.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import my.nanihadesuka.clementineflow.*
import my.nanihadesuka.clementineflow.backend.MessagesFlow
import my.nanihadesuka.clementineflow.backend.backend
import java.util.*

@ExperimentalCoroutinesApi
@FlowPreview
class Model : ViewModel()
{
    val currentMetadata = backend.messagesFlow.responseCurrentMetadata.flow.asLiveData()
    val playlists = backend.messagesFlow.responsePlaylists.flow.asLiveData()
    val playerRunState = backend.messagesFlow.playerRunState.asLiveData()
    val repeat = backend.messagesFlow.repeat.flow.asLiveData()
    val shuffle = backend.messagesFlow.shuffle.flow.asLiveData()
    val updateTrackPosition = backend.messagesFlow.responseUpdateTrackPosition.flow.debounce(100).asLiveData()
    val activePlaylistId = backend.messagesFlow.activePlaylistId.asLiveData()
    val volume = backend.messagesFlow.requestSetVolume.flow.asLiveData()
    val playlistSongs = backend.messagesFlow.responsePlaylistSongs.flow.asLiveData()
    val responseDisconnect = backend.messagesFlow.responseDisconnect.flow.asLiveData()

    var isFirstLoadedPlaylist by mutableStateOf(true)

    var timeSliderSelected by mutableStateOf(false)
    var timeSliderSelectedPos by mutableStateOf(updateTrackPosition.value?.position?.toFloat() ?: 0f)

    var playlistSongsSearchVisible by mutableStateOf(false)
    var playlistSongsSearchText by mutableStateOf("")

    val playlistSongsSearchTextFlow = MutableStateFlow(playlistSongsSearchText)
    val playlistSongsSearchVisibleFlow = MutableStateFlow(playlistSongsSearchVisible)

    val socketConnectionState = backend.messagesFlow.socketConnectionState.flow.asLiveData()
    val socketConnectionError = backend.messagesFlow.socketConnectionError.flow.asLiveData()

    val songsList = combine(
        backend.messagesFlow.responsePlaylistSongs.flow,
        playlistSongsSearchTextFlow.debounce(200),
        playlistSongsSearchVisibleFlow
    ) { it, text, visible ->
        if (!visible) return@combine it.songsList
        val input = text.trim()
        if (text.isEmpty()) return@combine it.songsList
        it.songsList.filter { song -> song.title.contains(input, ignoreCase = true) }
    }.asLiveData()

    val clementineConnectError = backend.messagesFlow.clementineConnectError.asLiveData()

    val remote_ip_flow = App.preferences.REMOTE_IP_flow()
    val remote_port_flow = App.preferences.REMOTE_PORT_flow()
    val remote_authcode_flow = App.preferences.REMOTE_AUTHCODE_flow()
    val remote_needs_authcode_flow = App.preferences.REMOTE_NEEDS_AUTHCODE_flow()


    val remote_ip = remote_ip_flow.asLiveData()
    val remote_port = remote_port_flow.asLiveData()
    val remote_authcode = remote_authcode_flow.asLiveData()
    val remote_needs_authcode = remote_needs_authcode_flow.asLiveData()
    val theme_follow_system = App.preferences.THEME_FOLLOW_SYSTEM_flow().asLiveData()
    val theme_type = App.preferences.THEME_TYPE_flow().asLiveData()
    val show_volume_slider = App.preferences.SHOW_VOLUME_SLIDER_flow().asLiveData()

    val clementineConnectionState = backend.messagesFlow.clementineConnectionState.asLiveData()

    private fun isConnectionInterfaceNeeded() = run {
        App.preferences.REMOTE_IP.isBlank() ||
                (App.preferences.REMOTE_NEEDS_AUTHCODE && App.preferences.REMOTE_AUTHCODE < 0)
    }

    var connectionInterfaceShow by mutableStateOf(isConnectionInterfaceNeeded())
    var connectionInterfaceAllowDismiss by mutableStateOf(false)

    private val clementineConnectErrorListenerJob = CoroutineScope(Dispatchers.IO).launch {
        backend.messagesFlow.clementineConnectError.collect {
            when (it)
            {
                MessagesFlow.ClementineConnectError.NEEDS_PASSWORD,
                MessagesFlow.ClementineConnectError.WRONG_PASSWORD ->
                {
                    CoroutineScope(Dispatchers.Main).launch {
                        connectionInterfaceShow = true
                    }
                }
            }
        }
    }
}