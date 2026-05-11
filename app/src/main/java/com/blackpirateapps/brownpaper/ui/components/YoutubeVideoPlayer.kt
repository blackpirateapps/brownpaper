package com.blackpirateapps.brownpaper.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YoutubeVideoPlayer(
    videoId: String,
    startSeconds: Float,
    onPositionChanged: (Float) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentPosition by remember { mutableFloatStateOf(startSeconds) }

    DisposableEffect(videoId) {
        onDispose {
            onPositionChanged(currentPosition)
        }
    }

    AndroidView(
        factory = { context ->
            YouTubePlayerView(context).apply {
                val iFramePlayerOptions = com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions.Builder()
                    .controls(1)
                    .fullscreen(1)
                    .build()
                
                enableAutomaticInitialization = false
                initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, startSeconds)
                    }
                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        currentPosition = second
                    }
                }, iFramePlayerOptions)
                
                lifecycleOwner.lifecycle.addObserver(this)
            }
        },
        modifier = modifier
    )
}
