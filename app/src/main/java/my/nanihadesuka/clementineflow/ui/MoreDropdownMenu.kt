package my.nanihadesuka.clementineflow.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import my.nanihadesuka.clementineflow.App
import my.nanihadesuka.clementineflow.THEME_FOLLOW_SYSTEM
import my.nanihadesuka.clementineflow.THEME_TYPE
import my.nanihadesuka.clementineflow.backend.MessageBuilder
import my.nanihadesuka.clementineflow.backend.MessageBuilder.message
import my.nanihadesuka.clementineflow.backend.MessagesFlow
import my.nanihadesuka.clementineflow.backend.backend
import my.nanihadesuka.clementineflow.backend.pb.Remotecontrolmessages
import my.nanihadesuka.clementineflow.ui.theme.Orange


@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalAnimationApi
@Composable
fun MoreDropdownMenu(dropdownOpen : MutableState<Boolean>)
{
    val model = viewModel<Model>()

    val themeFollowSystem by model.theme_follow_system.observeAsState()
    val themeType by model.theme_type.observeAsState()
    val clementineConnectionState by model.clementineConnectionState.observeAsState()
    val socketConnectionState by model.socketConnectionState.observeAsState()

    DropdownMenu(
        expanded = dropdownOpen.value,
        onDismissRequest = { dropdownOpen.value = false }
    ) {
        DropdownMenuItem(onClick = {
            model.connectionInterfaceShow = true
            model.connectionInterfaceAllowDismiss = true
            dropdownOpen.value = false
        }) {
            Text("Open connect dialog")
        }

        DropdownMenuItem(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                backend.rc.cancel()
                backend.rc = backend.createDefaultRemoteConnection()
            }
        }) {
            Text("Try reconnect")
        }

        DropdownMenuItem(onClick = {
            backend.rc.sendMessage(MessageBuilder.disconnect())
            CoroutineScope(Dispatchers.IO).launch {
                backend.messagesFlow.pipeMessage(
                    Remotecontrolmessages.MsgType.DISCONNECT.message({ responseDisconnectBuilder }) {
                        reasonDisconnect = Remotecontrolmessages.ReasonDisconnect.Download_Forbidden
                    }
                )
                backend.rc.cancel()
            }
        }) {
            Text("Disconnect")
        }

        val connectionState = when
        {
            socketConnectionState == MessagesFlow.SocketConnectionState.CONNECTING -> "Connecting"
            clementineConnectionState == MessagesFlow.ClementineConnectionState.ALIVE -> "Connected"
            else -> "Disconnected"
        }

        AnimatedContent(
            targetState = connectionState,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { state ->
            Text(
                text = state,
                modifier = Modifier
                    .padding(10.dp)
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .alpha(0.5f),
                textAlign = TextAlign.Center
            )
        }

        Divider()

        DropdownMenuItem(onClick = { App.preferences.THEME_TYPE = if (themeType != THEME_TYPE.LIGHT) THEME_TYPE.LIGHT else THEME_TYPE.BLACK }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Light theme",
                    modifier = Modifier.padding(end = 6.dp)
                )
                Switch(
                    checked = themeType == THEME_TYPE.LIGHT,
                    onCheckedChange = { App.preferences.THEME_TYPE = if (it) THEME_TYPE.LIGHT else THEME_TYPE.BLACK },
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colors.onPrimary,
                        checkedThumbColor = Color.Orange
                    )
                )
            }
        }

        DropdownMenuItem(onClick = { App.preferences.THEME_FOLLOW_SYSTEM = !(themeFollowSystem == true) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Follow system",
                    modifier = Modifier.padding(end = 6.dp)
                )
                Checkbox(
                    checked = themeFollowSystem == true,
                    onCheckedChange = { it -> App.preferences.THEME_FOLLOW_SYSTEM = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.Orange
                    )
                )
            }
        }

        Divider()

        var aboutDialogShow by remember { mutableStateOf(false) }
        if (aboutDialogShow)
            AlertDialog(
                onDismissRequest = { aboutDialogShow = !aboutDialogShow },
                buttons = {
                    val context = LocalContext.current

                    @Composable
                    fun TextCentered(text:String, modifier: Modifier = Modifier, fontWeight: FontWeight = FontWeight.Normal)
                    {
                        Text(
                            text = text,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().then(modifier),
                            fontWeight = fontWeight
                        )
                    }

                    Column(Modifier.padding(16.dp)) {
                        TextCentered("Clementine flow",fontWeight = FontWeight.Bold)
                        TextCentered("")
                        TextCentered("Remote controller for the clementine player.")
                        TextCentered("")
                        TextCentered("Made by nani.")
                        TextCentered("")
                        TextCentered("Thanks to the official clementine devs too :)")
                        TextCentered("")
                        TextCentered("This software is FOSS")
                        TextCentered("This software is GPL-3")
                        TextCentered("")
                        TextCentered("Github repository")

                        val url = "https://github.com/nanihadesuka/ClementineFlow"
                        Text(
                            text = url,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colors.onPrimary.copy(0.1f), CircleShape)
                                .background(MaterialTheme.colors.onPrimary.copy(0.05f))
                                .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                .padding(12.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )

        DropdownMenuItem(onClick = { aboutDialogShow = !aboutDialogShow }) {
            Text("About")
        }
    }
}