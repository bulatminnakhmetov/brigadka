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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.presentation.common.compose.CircularNetworkImage

@Composable
fun Avatar(
    mediaItem: MediaItem?,
    isUploading: Boolean = false,
    onError: (String) -> Unit = {},
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (isUploading) {
        // Show a progress indicator while uploading
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        }

    } else {
        val url = mediaItem?.thumbnail_url
        // Show the avatar image
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = onClick != null) {
                        onClick?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (url != null) {
                    // Show the image from the URL
                    CircularNetworkImage(
                        url = url,
                        contentDescription = "Avatar",
                        onError = { error ->
                            onError("Failed to load avatar: $error")
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                } else {
                    // Show a default avatar icon
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Remove button
            if (url != null && onRemove != null) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    RemoveButton(onRemove, modifier = Modifier.align(Alignment.TopEnd).size(36.dp))
                }
            }
        }
    }
}