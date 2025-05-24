package com.brigadka.app.presentation.profile.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.presentation.common.compose.NetworkImage


@Composable
fun VideoThumbnail(
    mediaItem: MediaItem?,
    isUploading: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val videoUrl = mediaItem?.url ?: ""
    var showFullscreenPlayer by remember { mutableStateOf(false) }

    // Fullscreen player dialog
    if (showFullscreenPlayer && videoUrl.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showFullscreenPlayer = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false // Makes dialog fullscreen
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val controller = rememberVideoPlayerController(videoUrl)

                DisposableEffect(Unit) {
                    onDispose {
                        controller.release()
                    }
                }

                VideoPlayer(
                    url = videoUrl,
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )

                // Close button
                IconButton(
                    onClick = { showFullscreenPlayer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close player",
                        tint = Color.White
                    )
                }

                // Auto-play when entering fullscreen
                LaunchedEffect(Unit) {
                    controller.play()
                }
            }
        }
    }

    // Thumbnail view
    Box(
        modifier = modifier.aspectRatio(16/9f)
    ) {
        Box(modifier = Modifier.padding(top = 4.dp, end = 4.dp).clip(RoundedCornerShape(8.dp))) {
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Thumbnail image
                NetworkImage(
                    url = mediaItem?.thumbnail_url ?: "",
                    contentDescription = "Video thumbnail",
                    onError = { error ->
                        onError?.invoke("Failed to get video thumbnail: $error")
                    },
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {}
                    }
                )

                // Play button overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (videoUrl.isNotEmpty()) {
                                showFullscreenPlayer = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(48.dp).background(color = Color.Black.copy(alpha = 0.1f), shape = CircleShape).padding(8.dp)
                    )
                }
            }
        }


        // Remove button
        if (onRemove != null) {
            RemoveButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
            )
        }
    }
}