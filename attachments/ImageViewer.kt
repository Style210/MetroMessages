package com.metromessages.attachments

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ImageViewer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = modifier.size(300.dp) // centered by overlay Box
    )
}
