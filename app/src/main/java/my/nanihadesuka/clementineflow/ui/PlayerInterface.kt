package my.nanihadesuka.clementineflow.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import my.nanihadesuka.clementineflow.*
import my.nanihadesuka.clementineflow.ui.utils.blend
import my.nanihadesuka.clementineflow.backend.MessageBuilder
import my.nanihadesuka.clementineflow.backend.MessageBuilder.message
import my.nanihadesuka.clementineflow.backend.MessagesFlow
import my.nanihadesuka.clementineflow.backend.backend
import my.nanihadesuka.clementineflow.backend.pb.Remotecontrolmessages
import my.nanihadesuka.clementineflow.ui.theme.Orange
import kotlin.math.roundToInt

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun PlayerInterface()
{
    val model = viewModel<Model>()
    val currentMetadata by model.currentMetadata.observeAsState()
    val playlistSongs by model.playlistSongs.observeAsState()
    val songsList by model.songsList.observeAsState()

    LazyColumn(
        state = rememberLazyListState(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 200.dp)
    ) {
        if (model.playlistSongsSearchVisible)
        {
            stickyHeader {
                val focusRequester = remember { FocusRequester() }
                Surface {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                    {
                        Box(Modifier.height(10.dp))
                        Playlists()
                        OutlinedTextField(
                            value = model.playlistSongsSearchText,
                            onValueChange = {
                                model.playlistSongsSearchText = it
                                CoroutineScope(Dispatchers.Default).launch {
                                    model.playlistSongsSearchTextFlow.emit(it)
                                }
                            },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .padding(top = 5.dp),
                            singleLine = true,
                            maxLines = 1,
                            shape = CircleShape,
                            colors = TextFieldDefaults.textFieldColors(
                                cursorColor = MaterialTheme.colors.onPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Search song") }
                        )
                        Divider(Modifier.padding(top = 10.dp))
                        SelectedPlaylistTitle()
                        Divider()
                    }
                }

                DisposableEffect(Unit) {
                    focusRequester.requestFocus()
                    onDispose { }
                }
            }
        } else
        {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    SongPlaying()
                    SettingsButton(Modifier.align(Alignment.CenterEnd))
                }
            }
            item { VolumeSlider() }
            item { PlayerControls() }
            item { SongTimeText() }
            stickyHeader {
                Surface {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                    {
                        SongTimeSlider()
                        Playlists()
                        Divider(Modifier.padding(top = 10.dp))
                        SelectedPlaylistTitle()
                        Divider()
                    }
                }
            }
        }

        val songs = songsList
        val playlist = playlistSongs
        if (songs != null && playlist != null) items(songs) {
            SelectedPlaylistSong(it, playlist, currentMetadata?.songMetadata)
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@Composable
private fun SongPlaying(modifier: Modifier = Modifier)
{
    val model = viewModel<Model>()
    val currentMetadata by model.currentMetadata.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 45.dp, end = 45.dp)
            .then(modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = currentMetadata?.songMetadata?.title,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { targetTitle ->
            Text(
                text = targetTitle ?: "No song title",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp)
                    .padding(top = 20.dp)
                    .height(45.dp),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@Composable
private fun SettingsButton(modifier: Modifier)
{
    val model = viewModel<Model>()

    val dropdownOpen = remember { mutableStateOf(false) }
    IconButton(
        onClick = { dropdownOpen.value = !dropdownOpen.value },
        modifier = Modifier
            .padding(5.dp)
            .padding(top = 20.dp)
            .height(45.dp)
            .then(modifier)

    ) {
        val iconModifier = Modifier.size(24.dp)
        Crossfade(dropdownOpen.value) {
            when (it)
            {
                false -> Icon(Icons.Filled.MoreVert, "Settings", iconModifier.alpha(0.5f))
                true ->
                {
                    Icon(Icons.Filled.MoreVert, "Settings", iconModifier)
                }
            }
        }

        MoreDropdownMenu(dropdownOpen)
    }
}

@ExperimentalCoroutinesApi
@Composable
private fun PlayerControls()
{
    val model = viewModel<Model>()
    val playerRunState by model.playerRunState.observeAsState()

    Surface(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = { backend.rc.sendMessage(MessageBuilder.previous()) },
                modifier = Modifier
                    .padding(5.dp)
                    .padding(start = 8.dp)
            ) { Icon(Icons.Filled.ArrowBackIos, "Previous", Modifier.size(30.dp)) }

            IconButton(
                onClick = { backend.rc.sendMessage(MessageBuilder.playPause()) },
                modifier = Modifier
                    .padding(5.dp)

            ) {
                Icon(
                    if (playerRunState == MessagesFlow.PlayerRunState.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play toggle",
                    Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = { backend.rc.sendMessage(MessageBuilder.next()) },
                modifier = Modifier
                    .padding(5.dp)
                    .padding(start = 8.dp)
            ) { Icon(Icons.Filled.ArrowForwardIos, "Next", Modifier.size(30.dp)) }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            val repeatMode = model.repeat.observeAsState().value?.repeatMode ?: Remotecontrolmessages.RepeatMode.Repeat_Off

            IconButton(
                onClick = {
                    val newMode = when (repeatMode)
                    {
                        Remotecontrolmessages.RepeatMode.Repeat_Off -> Remotecontrolmessages.RepeatMode.Repeat_Playlist
                        Remotecontrolmessages.RepeatMode.Repeat_Playlist -> Remotecontrolmessages.RepeatMode.Repeat_Track
                        Remotecontrolmessages.RepeatMode.Repeat_Track -> Remotecontrolmessages.RepeatMode.Repeat_Album
                        Remotecontrolmessages.RepeatMode.Repeat_Album -> Remotecontrolmessages.RepeatMode.Repeat_Off
                    }
                    backend.rc.sendMessage(MessageBuilder.repeat(newMode))
                },
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {
                val iconModifier = Modifier.size(24.dp)
                Crossfade(repeatMode) {
                    when (it)
                    {
                        Remotecontrolmessages.RepeatMode.Repeat_Off -> Icon(
                            Icons.Filled.Repeat, "Repeat", iconModifier.alpha(0.5f)
                        )
                        Remotecontrolmessages.RepeatMode.Repeat_Playlist -> Icon(
                            Icons.Filled.Repeat, "Repeat", iconModifier
                        )
                        Remotecontrolmessages.RepeatMode.Repeat_Track -> Icon(
                            Icons.Filled.RepeatOne, "Repeat", iconModifier
                        )
                        Remotecontrolmessages.RepeatMode.Repeat_Album -> Icon(
                            Icons.Filled.RepeatOn, "Repeat", iconModifier
                        )
                    }
                }
            }

            val shuffleMode = model.shuffle.observeAsState().value?.shuffleMode ?: Remotecontrolmessages.ShuffleMode.Shuffle_Off

            IconButton(
                onClick = {
                    val newMode = when (shuffleMode)
                    {
                        Remotecontrolmessages.ShuffleMode.Shuffle_Off -> Remotecontrolmessages.ShuffleMode.Shuffle_All
                        Remotecontrolmessages.ShuffleMode.Shuffle_All -> Remotecontrolmessages.ShuffleMode.Shuffle_InsideAlbum
                        Remotecontrolmessages.ShuffleMode.Shuffle_InsideAlbum -> Remotecontrolmessages.ShuffleMode.Shuffle_Albums
                        Remotecontrolmessages.ShuffleMode.Shuffle_Albums -> Remotecontrolmessages.ShuffleMode.Shuffle_Off
                    }
                    backend.rc.sendMessage(MessageBuilder.shuffle(newMode))
                },
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {
                val iconModifier = Modifier.size(24.dp)
                Crossfade(shuffleMode) {
                    when (it)
                    {
                        Remotecontrolmessages.ShuffleMode.Shuffle_Off -> Icon(
                            Icons.Filled.Shuffle, "Shuffle", iconModifier.alpha(0.5f)
                        )
                        Remotecontrolmessages.ShuffleMode.Shuffle_All -> Icon(
                            Icons.Filled.Shuffle, "Shuffle", iconModifier
                        )
                        Remotecontrolmessages.ShuffleMode.Shuffle_InsideAlbum -> Icon(
                            Icons.Filled.ShuffleOn, "Shuffle", iconModifier
                        )
                        Remotecontrolmessages.ShuffleMode.Shuffle_Albums -> Icon(
                            Icons.Filled.ShuffleOn, "Shuffle", iconModifier, tint = Color.Orange
                        )
                    }
                }
            }
        }
    }
}


@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@Composable
private fun SongTimeText()
{
    val model = viewModel<Model>()
    val currentMetadata by model.currentMetadata.observeAsState()
    val updateTrackPosition by model.updateTrackPosition.observeAsState()
    val playerRunState by model.playerRunState.observeAsState()

    val maxValue = currentMetadata?.songMetadata?.length?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val value = updateTrackPosition?.position?.toFloat() ?: 0f

    val pos = if (model.timeSliderSelected) model.timeSliderSelectedPos else value

    val color by animateColorAsState(
        targetValue = MaterialTheme.colors.onPrimary.let {
            if (playerRunState == MessagesFlow.PlayerRunState.playing) it
            else it.copy(alpha = 0.5f)
        }
    )

    Text(
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        text = listOf(pos, maxValue).joinToString(" / ") { DateUtils.formatElapsedTime(it.toLong()).removePrefix("0") },
        color = color
    )
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@Composable
private fun SongTimeSlider()
{
    val model = viewModel<Model>()
    val currentMetadata by model.currentMetadata.observeAsState()
    val updateTrackPosition by model.updateTrackPosition.observeAsState()

    val maxValue = currentMetadata?.songMetadata?.length?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val value = updateTrackPosition?.position?.toFloat() ?: 0f

    val coroutineScope = rememberCoroutineScope()
    var unselectJob by remember { mutableStateOf<Job?>(null) }

    val pos = if (model.timeSliderSelected) model.timeSliderSelectedPos else value

    Slider(
        value = pos,
        valueRange = 0f..maxValue,
        onValueChange = { position ->
            unselectJob?.cancel()
            model.timeSliderSelected = true
            model.timeSliderSelectedPos = position
        },
        onValueChangeFinished = {
            backend.rc.sendMessage(MessageBuilder.requestSetTrackPosition(model.timeSliderSelectedPos.toInt()))
            unselectJob = coroutineScope.launch {
                delay(1600)
                model.timeSliderSelected = false
            }
        },
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = Color.Orange,
            activeTrackColor = Color.Orange.copy(alpha = 0.5f),
            inactiveTrackColor = Color.Orange.copy(alpha = 0.1f)
        )
    )
}

@ExperimentalAnimationApi
@Composable
private fun VolumeSlider()
{
    val model = viewModel<Model>()
    val showVolumeSlider by model.show_volume_slider.observeAsState()
    val responseVolume by model.volume.observeAsState()
    val value = responseVolume?.volume?.toFloat() ?: 100f
    val maxValue = 100f

    var selected by remember { mutableStateOf(false) }
    var selectedPos by remember { mutableStateOf(value) }

    val coroutineScope = rememberCoroutineScope()
    var unselectJob by remember { mutableStateOf<Job?>(null) }

    val pos = if (selected) selectedPos else value

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

        AnimatedVisibility(
            visible = showVolumeSlider ?: false,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)

            ) {
                Text(
                    text = pos.roundToInt().toString(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 30.dp),
                )

                Slider(
                    modifier = Modifier
                        .padding(start = (30 + 34).dp, end = 10.dp)
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    value = pos,
                    valueRange = 0f..maxValue,
                    onValueChange = { position ->
                        unselectJob?.cancel()
                        selected = true
                        selectedPos = position.roundToInt().toFloat()
                        backend.rc.sendMessage(MessageBuilder.setVolume(position.roundToInt()))
                    },
                    onValueChangeFinished = {
                        unselectJob = coroutineScope.launch {
                            delay(1600)
                            selected = false
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Orange,
                        activeTrackColor = Color.Orange.copy(alpha = 0.5f),
                        inactiveTrackColor = Color.Orange.copy(alpha = 0.1f)
                    )
                )
            }
        }

        IconButton(
            onClick = { App.preferences.SHOW_VOLUME_SLIDER = !(showVolumeSlider ?: false) },
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .align(Alignment.CenterVertically)

        ) {
            val iconModifier = Modifier.size(24.dp)
            Crossfade(showVolumeSlider) {
                when (it)
                {
                    false -> Icon(Icons.Filled.VolumeUp, "Volume", iconModifier.alpha(0.5f))
                    true ->
                    {
                        Icon(Icons.Filled.VolumeUp, "Volume", iconModifier)
                    }
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun Playlists()
{
    val model = viewModel<Model>()
    val activePlaylistId by model.activePlaylistId.observeAsState()
    val playlists by model.playlists.observeAsState()
    val playlistSongs by model.playlistSongs.observeAsState()

    val playlistsLazyRowState = rememberLazyListState()

    LazyRow(state = playlistsLazyRowState) {
        items(playlists?.playlistList ?: listOf()) {

            val buttonColor = MaterialTheme.colors.onPrimary.blend(0.05f, MaterialTheme.colors.primary)
            val normalBorderColor = MaterialTheme.colors.onPrimary.blend(0.1f, MaterialTheme.colors.primary)
            val borderColor by animateColorAsState(
                targetValue = when (it.id == activePlaylistId)
                {
                    true -> Color.Orange.copy(alpha = 0.5f)
                    false -> normalBorderColor
                },
                animationSpec = tween(durationMillis = 250)
            )

            val openDialogTitle = remember { mutableStateOf(false) }

            if (openDialogTitle.value)
                Dialog(
                    onDismissRequest = { openDialogTitle.value = false },
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .border(1.dp, normalBorderColor, MaterialTheme.shapes.large)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(bottom = 30.dp)
                                .width(200.dp)
                        ) {
                            Text(
                                text = it.name,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, borderColor, CircleShape)
                                    .padding(12.dp)

                            )
                            Text(
                                text = "Remove playlist",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .background(buttonColor)
                                    .clickable {
                                        if (playlistSongs?.requestedPlaylist?.id == it.id)
                                        {
                                            val firstPlaylist = playlists?.playlistList?.firstOrNull()
                                            if (firstPlaylist != null)
                                                backend.rc.sendMessage(MessageBuilder.requestPlaylistSongs(firstPlaylist.id))
                                        }
                                        backend.rc.sendMessage(MessageBuilder.closePlaylist(it.id))
                                        openDialogTitle.value = false
                                    }
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            )
                            Text(
                                text = "Dismiss",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .background(buttonColor)
                                    .clickable { openDialogTitle.value = false }
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }


            val backgroundButton by animateColorAsState(
                targetValue = if (playlistSongs?.requestedPlaylist?.id == it.id) buttonColor else Color.Unspecified
            )

            Text(
                text = it.name,
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .border(1.dp, borderColor, CircleShape)
                    .background(backgroundButton)
                    .combinedClickable(
                        onClick = {
                            backend.rc.sendMessage(MessageBuilder.requestPlaylistSongs(it.id))
                        },
                        onLongClick = { openDialogTitle.value = true }
                    )
                    .padding(12.dp)
            )
        }
    }

    if (model.isFirstLoadedPlaylist) LaunchedEffect(playlists == null) {
        val index = playlists?.playlistList?.indexOfFirst { it.active }
        if (index == null || index == -1) return@LaunchedEffect
        playlistsLazyRowState.animateScrollToItem(index, 0)
        model.isFirstLoadedPlaylist = false
    }
}

@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@Composable
private fun SelectedPlaylistTitle()
{
    val model = viewModel<Model>()
    val playlistSongs by model.playlistSongs.observeAsState()
    val playlists by model.playlists.observeAsState()
    // Can't get playlist name directly because it seems the server doesn't fill it
    val id = playlistSongs?.requestedPlaylist?.id
    val playlistName = playlists?.playlistList?.find { it.id == id }?.name

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = playlistName,
            transitionSpec = {
                if (targetState?.length == initialState?.length)
                    EnterTransition.None with ExitTransition.None
                else
                    fadeIn() with fadeOut()
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.Center)
        ) { targetTitle ->
            Text(
                text = targetTitle ?: "No playlist name",
                modifier = Modifier
                    .padding(5.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        Icon(
            if (model.playlistSongsSearchVisible) Icons.Filled.Close else Icons.Filled.Search,
            "Search in playlist",
            Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 5.dp)
                .clickable {
                    model.playlistSongsSearchText = ""
                    model.playlistSongsSearchVisible = !model.playlistSongsSearchVisible

                    CoroutineScope(Dispatchers.Default).launch {
                        model.playlistSongsSearchTextFlow.emit(model.playlistSongsSearchText)
                        model.playlistSongsSearchVisibleFlow.emit(model.playlistSongsSearchVisible)
                    }
                }
                .padding(horizontal = 15.dp)
                .padding(vertical = 5.dp)
                .size(30.dp)
        )
    }
}

@Composable
private fun SelectedPlaylistSong(
    songMetadata: Remotecontrolmessages.SongMetadata,
    playlistSongs: Remotecontrolmessages.ResponsePlaylistSongs,
    playingSongMetadata: Remotecontrolmessages.SongMetadata?
)
{
    val background by animateColorAsState(
        targetValue = if (playingSongMetadata != null &&
            songMetadata.id == playingSongMetadata.id &&
            songMetadata.fileSize == playingSongMetadata.fileSize &&
            songMetadata.length == playingSongMetadata.length
        ) Color.Orange.copy(alpha = 0.2f) else Color.Unspecified
    )


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable {
                backend.rc.sendMessage(
                    MessageBuilder.changeSong(
                        playlistId = playlistSongs.requestedPlaylist.id,
                        songIndex = songMetadata.index
                    )
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = songMetadata.title,
            modifier = Modifier
                .padding(10.dp)
                .weight(1f)
        )

        Text(
            text = DateUtils.formatElapsedTime(songMetadata.length.toLong()).removePrefix("0"),
            modifier = Modifier
                .padding(10.dp)
                .alpha(0.3f)
                .align(Alignment.Top)
        )
    }
}
