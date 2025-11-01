// File: ArchiveTab.kt
package com.metromessages.ui.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.metromessages.ui.components.MetroFont
import com.metromessages.viewmodel.FacebookViewModel

@Composable
fun ArchiveTab(
    facebookViewModel: FacebookViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    metroFont: MetroFont,
    onConversationClick: (String, String?, String?) -> Unit,
    isAuthenticated: Boolean = false, // Will come from fingerprint auth
    onRequestAuthentication: () -> Unit // Callback to trigger fingerprint auth
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "Archive",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        when {
            !isAuthenticated -> {
                // Authentication required state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_lock_lock),
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Archive Locked",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Authenticate to view archived conversations",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap to unlock",
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clickable { onRequestAuthentication() }
                    )
                }
            }

            else -> {
                // Empty archive state (no conversations loaded)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "No archived conversations",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Swipe left on conversations to archive them",
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}