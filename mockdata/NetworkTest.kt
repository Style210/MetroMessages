package com.metromessages.mockdata

// File: NetworkTest.kt


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun NetworkTest() {
    Column {
        Text("Test 1: Direct PNG from DiceBear")
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://api.dicebear.com/7.x/initials/png?seed=Test%20User&size=100&radius=0")
                .build(),
            contentDescription = "Test Avatar",
            modifier = Modifier.size(50.dp)
        )

        Text("Test 2: Simple PNG from web")
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("https://picsum.photos/100")
                .build(),
            contentDescription = "Test Image",
            modifier = Modifier.size(50.dp)
        )
    }
}