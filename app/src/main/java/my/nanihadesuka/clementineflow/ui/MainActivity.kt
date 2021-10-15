package my.nanihadesuka.clementineflow.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import my.nanihadesuka.clementineflow.backend.backend
import my.nanihadesuka.clementineflow.backend.MessageBuilder
import my.nanihadesuka.clementineflow.ui.theme.AppTheme

class MainActivity : ComponentActivity()
{
    val model by viewModels<Model>()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode)
    {
        KeyEvent.KEYCODE_VOLUME_UP ->
        {
            model.volume.value?.let {
                backend.rc.sendMessage(MessageBuilder.setVolume((it.volume + 10).coerceIn(0, 100)))
            }
            true
        }
        KeyEvent.KEYCODE_VOLUME_DOWN ->
        {
            val model by viewModels<Model>()
            model.volume.value?.let {
                backend.rc.sendMessage(MessageBuilder.setVolume((it.volume - 10).coerceIn(0, 100)))
            }
            true
        }
        KeyEvent.KEYCODE_VOLUME_MUTE ->
        {
            backend.rc.sendMessage(MessageBuilder.setVolume(0))
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    @ExperimentalComposeUiApi
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(Modifier.fillMaxSize()) {
                    PlayerInterface()
                    AnimatedVisibility(
                        visible = model.connectionInterfaceShow,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ){
                        ConnectInterface()
                    }
                }
            }
        }
    }
}