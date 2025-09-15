/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.nxweb.jellyfin_wearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import com.nxweb.jellyfin_wearos.R
import com.nxweb.jellyfin_wearos.presentation.theme.JellyfinwearosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto

class MainActivity : ComponentActivity() {
    lateinit var player: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        val jellyfin = Jellyfin(this)
        player = MediaPlayer(this)

        setContent {
            WearApp(jellyfin, player)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@Composable
fun WearApp(jellyfin: Jellyfin, player: MediaPlayer) {
    JellyfinwearosTheme {
        val navController = rememberSwipeDismissableNavController()
        val isLoggedIn = jellyfin.checkCredentials()
        val startDestination = if (isLoggedIn) "Libraries" else "login"

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("login") {
                Login{ hostname, username, password ->
                    jellyfin.saveCredentials(hostname, username, password)

                    navController.navigate("Libraries")
                }
            }
            composable("Libraries") {
                Libraries(jellyfin, player, navController)
            }
            composable("PlayerScreen"){
                PlayerScreen(player)
            }
        }

    }
}

@Composable
fun Libraries(jellyfin: Jellyfin, player: MediaPlayer, navController: NavHostController) {
    val scalingLazyListState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            delay(1500)
            items = jellyfin.getLibraries()
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Loading()
    } else {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scalingLazyListState,
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            items(items.size) { index ->
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                val songs = jellyfin.getItems(items[index].id)
                                player.setShuffleQueue(songs.content.items, jellyfin, 0)
                                navController.navigate("PlayerScreen")
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = items[index].name ?: "Unknown",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...")
    }
}
@Composable
fun Login(onLogin: (String, String, String) -> Unit) {
    var hostname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scalingLazyListState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = scalingLazyListState,
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item {
            Text(
                text = "Jellyfin Login",
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            OutlinedTextField(
                value = hostname,
                onValueChange = { hostname = it },
                label = { Text("Hostname") },
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        item {
            Button(
                onClick = { onLogin(hostname, username, password) },
                enabled = hostname.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Login")
            }
        }
    }
}

@Composable
fun PlayerScreen(player: MediaPlayer) {
    var currentSong by remember { mutableStateOf(player.getCurrentSong()) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                currentSong = player.getCurrentSong()
            }
        }
        player.exoPlayer.addListener(listener)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(),
            autoCentering = AutoCenteringParams(itemIndex = 1)
        ) {
            item {
                Text(
                    text = currentSong?.name ?: "No Song",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 2
                )
            }

            item {
                Text(
                    text = currentSong?.albumArtist ?: "Unknown Artist",
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            item {
                Button(
                    onClick = {
                        if (isPlaying) {
                            player.pause()
                        } else {
                            player.playQueue()
                        }
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { player.previous() },
                        modifier = Modifier.weight(1f).padding(4.dp)
                    ) {
                        Text("Previous")
                    }

                    Button(
                        onClick = { player.next() },
                        modifier = Modifier.weight(1f).padding(4.dp)
                    ) {
                        Text("Next")
                    }
                }
            }

            item {
                Button(
                    onClick = { player.stop() },
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    colors = androidx.wear.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error
                    )
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    JellyfinwearosTheme {
    }
}