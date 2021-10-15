package my.nanihadesuka.clementineflow.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import my.nanihadesuka.clementineflow.*
import my.nanihadesuka.clementineflow.backend.MessagesFlow
import my.nanihadesuka.clementineflow.backend.backend
import my.nanihadesuka.clementineflow.ui.utils.blend
import my.nanihadesuka.clementineflow.ui.utils.doIf


@ExperimentalAnimationApi
@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalComposeUiApi
@Composable
fun ConnectInterface()
{
    val model = viewModel<Model>()
    val clementineConnectionState by model.clementineConnectionState.observeAsState()
    val clementineConnectError by model.clementineConnectError.observeAsState()
    val socketConnectionState by model.socketConnectionState.observeAsState()
    val socketConnectionError by model.socketConnectionError.observeAsState()
    val focusRequester = object
    {
        val ip = remember { FocusRequester() }
        val port = remember { FocusRequester() }
        val password = remember { FocusRequester() }
    }

    var ip by remember { mutableStateOf(App.preferences.REMOTE_IP) }
    var port by remember { mutableStateOf(App.preferences.REMOTE_PORT) }
    var needsPassword by remember { mutableStateOf(App.preferences.REMOTE_NEEDS_AUTHCODE) }
    var password by remember {
        mutableStateOf(if (needsPassword && App.preferences.REMOTE_AUTHCODE >= 0) App.preferences.REMOTE_AUTHCODE else null)
    }

    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        modifier = Modifier
    ) {
        val buttonColor = MaterialTheme.colors.onPrimary.blend(0.05f, MaterialTheme.colors.primary)
        val normalBorderColor = MaterialTheme.colors.onPrimary.blend(0.1f, MaterialTheme.colors.primary)

        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .wrapContentHeight(Alignment.Top)
                .padding(20.dp)
                .border(
                    2.dp,
                    normalBorderColor,
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 20.dp, bottom = 30.dp)
            ) {

                OutlinedTextField(
                    value = ip,
                    onValueChange = {
                        ip = it
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester.ip)
                        .padding(10.dp)
                        .padding(horizontal = 30.dp)
                        .focusOrder(focusRequester.ip) { focusRequester.port.requestFocus() },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    maxLines = 1,
                    shape = CircleShape,
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = MaterialTheme.colors.onPrimary,
                        focusedIndicatorColor = Color.Gray,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colors.onPrimary
                    ),
                    label = { Text("IP") },
                    trailingIcon = {
                        IconButton(
                            onClick = { ip = "192.168.0." },
                            modifier = Modifier
                                .padding(5.dp)
                                .padding(start = 8.dp)
                        ) { Icon(Icons.Filled.RestartAlt, "Reset", Modifier.size(30.dp)) }
                    }
                )

                OutlinedTextField(
                    value = port.toString(),
                    onValueChange = {
                        val value = it.toIntOrNull()
                        if (value != null)
                            port = value
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester.port)
                        .padding(10.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .focusOrder(focusRequester.port) { focusRequester.password.requestFocus() },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    maxLines = 1,
                    shape = CircleShape,
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = MaterialTheme.colors.onPrimary,
                        focusedIndicatorColor = Color.Gray,
                        unfocusedIndicatorColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colors.onPrimary
                    ),
                    label = { Text("Port") },
                    trailingIcon = {
                        IconButton(
                            onClick = { port = 5500 },
                            modifier = Modifier
                                .padding(5.dp)
                                .padding(start = 8.dp)
                        ) { Icon(Icons.Filled.RestartAlt, "Reset", Modifier.size(30.dp)) }
                    }
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {

                    OutlinedTextField(
                        value = password?.toString() ?: "",
                        onValueChange = {
                            if (it.isBlank())
                                password = null
                            val value = it.toIntOrNull()
                            if (value != null)
                                password = value
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .focusRequester(focusRequester.password)
                            .padding(10.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                            .focusOrder(focusRequester.password) { focusRequester.ip.requestFocus() },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = needsPassword,
                        singleLine = true,
                        maxLines = 1,
                        shape = CircleShape,
                        colors = TextFieldDefaults.textFieldColors(
                            cursorColor = MaterialTheme.colors.onPrimary,
                            focusedIndicatorColor = Color.Gray,
                            unfocusedIndicatorColor = Color.Gray,
                            focusedLabelColor = MaterialTheme.colors.onPrimary
                        ),
                        label = { Text("Password") }
                    )

                    Checkbox(
                        checked = needsPassword,
                        onCheckedChange = { needsPassword = it },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(top = 10.dp, end = 10.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colors.onPrimary
                        )
                    )
                }

                val validInput = ip.isNotBlank() && (if (needsPassword) password != null else true)

                var errorText = ""

                if (socketConnectionError == MessagesFlow.SocketConnectionError.ERROR)
                    errorText += "Unable to reach clementine server.\nCheck IP, port are correct.\n"
                if (ip.isBlank())
                    errorText += "IP field can't be empty.\n"
                if (needsPassword && password == null)
                    errorText += "Password field can't be empty.\n"
                if (clementineConnectError == MessagesFlow.ClementineConnectError.NEEDS_PASSWORD)
                    errorText += "Connection needs password.\n"
                if (clementineConnectError == MessagesFlow.ClementineConnectError.WRONG_PASSWORD)
                    errorText += "Wrong password.\n"

                Text(
                    text = errorText,
                    color = Color.Red
                )

                AnimatedVisibility(
                    visible = socketConnectionState == MessagesFlow.SocketConnectionState.CONNECTING,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ){
                    CircularProgressIndicator()
                }

                var clickedConnect by remember { mutableStateOf(false) }

                Text(
                    text = "Connect",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .background(buttonColor)
                        .doIf(validInput) {
                            clickable {
                                App.preferences.REMOTE_IP = ip
                                App.preferences.REMOTE_PORT = port
                                App.preferences.REMOTE_AUTHCODE = password ?: -1
                                App.preferences.REMOTE_NEEDS_AUTHCODE = needsPassword
                                CoroutineScope(Dispatchers.IO).launch {
                                    backend.rc.cancel()
                                    backend.rc = backend.createDefaultRemoteConnection()
                                    clickedConnect = true
                                }
                            }
                        }
                        .padding(12.dp)
                        .fillMaxWidth()
                )

                if(clickedConnect && clementineConnectionState == MessagesFlow.ClementineConnectionState.ALIVE)
                    model.connectionInterfaceShow = false

                if (model.connectionInterfaceAllowDismiss)
                    Text(
                        text = "Dismiss",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .background(buttonColor)
                            .clickable {
                                model.connectionInterfaceAllowDismiss = false
                                model.connectionInterfaceShow = false
                            }
                            .padding(12.dp)
                            .fillMaxWidth()
                    )

                LaunchedEffect(true) {
                    delay(200)
                    focusRequester.ip.requestFocus()
                }
            }
        }
    }
}
