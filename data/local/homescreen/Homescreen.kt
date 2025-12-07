// File: data/local/homescreen/HomeScreen.kt - COMPLETE OPTIMIZED VERSION
package com.metromessages.data.local.homescreen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.metromessages.data.local.metropeoplehub.MetroContact
import com.metromessages.data.local.metropeoplehub.MetroPeopleHubViewModel
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.navigation.MetroDestinations
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.coroutineScope

@Composable
fun HomeScreen(
    viewModel: MetroPeopleHubViewModel = hiltViewModel(),
    navController: NavController,
    metroFont: MetroFont
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // ✅ PERFORMANCE: Add timing logs
    println("⏱️ [HomeScreen] Composition START: ${System.currentTimeMillis()}")

    // ✅ OPTIMIZED: Collect data with initial value
    val starredContacts by viewModel.starredContacts.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val loadingMessage by viewModel.loadingMessage.collectAsState(initial = "")

    println("⏱️ [HomeScreen] After collectAsState: ${System.currentTimeMillis()}")

    // ✅ OPTIMIZED: Use derived state
    val shouldLoadFavorites by remember(starredContacts, isLoading) {
        derivedStateOf {
            starredContacts.isEmpty() && !isLoading
        }
    }

    LaunchedEffect(shouldLoadFavorites) {
        if (shouldLoadFavorites) {
            println("🏠 HomeScreen: Loading starred contacts only...")
            viewModel.loadStarredContactsOnly()
        } else if (starredContacts.isNotEmpty()) {
            println("✅ HomeScreen: Already have ${starredContacts.size} favorites")
        }
    }

    // ✅ OPTIMIZED: Share interaction source
    val sharedInteractionSource = remember { MutableInteractionSource() }

    // ✅ OPTIMIZED: Pre-calculate typography styles
    val metroSubheadStyle = remember(metroFont) {
        MetroTypography.MetroSubhead(metroFont).copy(color = Color.White)
    }

    val metroBody2Style = remember(metroFont) {
        MetroTypography.MetroBody2(metroFont).copy(color = Color.White)
    }

    val headerScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    LaunchedEffect(contentScrollState.value) {
        val headerTarget = (contentScrollState.value * 0.35f).toInt()
        headerScrollState.scrollTo(headerTarget)
    }

    println("⏱️ [HomeScreen] Before main layout: ${System.currentTimeMillis()}")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = sharedInteractionSource,
                        indication = null
                    ) {
                        activity?.finish()
                    }
            ) {
                MetroHeaderCanvas(
                    text = "home",
                    scrollState = headerScrollState,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                    metroFont = metroFont,
                )
            }

            // Content area - Using your existing PanoramaCanvas
            PanoramaCanvas(
                scrollState = contentScrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                panoramaFactor = 3f,
                edgePeekDp = 32.dp,
                pageCount = 3,
                onProgressChange = {},
                onPageChanged = {}
            ) { pageIndex, pageWidth, pageOpacity ->
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(pageWidth)
                        .alpha(pageOpacity)
                        .padding(start = if (pageIndex == 0) 24.dp else 0.dp)
                ) {
                    when (pageIndex) {
                        0 -> {
                            val hubPaths = listOf("phone", "people", "messages", "social", "settings")
                            hubPaths.forEach { path ->
                                when (path) {
                                    "settings" -> {
                                        Text(
                                            text = path,
                                            style = metroSubheadStyle,
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = sharedInteractionSource,
                                                    indication = null,
                                                    onClick = {
                                                        navController.navigate(MetroDestinations.Settings.route)
                                                    }
                                                )
                                        )
                                    }
                                    "people" -> {
                                        Text(
                                            text = path,
                                            style = metroSubheadStyle,
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = sharedInteractionSource,
                                                    indication = null,
                                                    onClick = {
                                                        navController.navigate(MetroDestinations.People.route)
                                                    }
                                                )
                                        )
                                    }
                                    "social" -> {
                                        Text(
                                            text = path,
                                            style = metroSubheadStyle,
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = sharedInteractionSource,
                                                    indication = null,
                                                    onClick = {
                                                        navController.navigate(MetroDestinations.Social.route)
                                                    }
                                                )
                                                .semantics {
                                                    testTag = "social_text_transition"
                                                }
                                        )
                                    }
                                    "messages" -> {
                                        Text(
                                            text = path,
                                            style = metroSubheadStyle,
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = sharedInteractionSource,
                                                    indication = null,
                                                    onClick = {
                                                        navController.navigate(MetroDestinations.Messages.route)
                                                    }
                                                )
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = path,
                                            style = metroSubheadStyle,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            Text(
                                text = "favorites",
                                style = metroSubheadStyle,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            if (isLoading && starredContacts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = loadingMessage,
                                        style = metroBody2Style
                                    )
                                }
                            } else if (starredContacts.isEmpty()) {
                                Text(
                                    text = "No favorites yet",
                                    style = metroBody2Style,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(
                                        items = starredContacts,
                                        key = { it.id }
                                    ) { contact ->
                                        FavoriteContactItem(
                                            contact = contact,
                                            onClick = { contactId ->
                                                val conversationId = "sms_$contactId"
                                                val contactName = contact.displayName
                                                val contactPhotoUrl = contact.photoUri

                                                navController.navigate(
                                                    MetroDestinations.SmsConversation.createRoute(
                                                        conversationId = conversationId,
                                                        contactId = contactId,
                                                        contactName = contactName,
                                                        contactPhotoUrl = contactPhotoUrl,
                                                        initialTab = 0
                                                    )
                                                )
                                            },
                                            metroFont = metroFont,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "what's new",
                                style = metroSubheadStyle,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Text(
                                text = "Nothing new right now",
                                style = metroBody2Style,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Performance logging
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            println("⏱️ [HomeScreen] Composition COMPLETE: ${System.currentTimeMillis()}")
        }
    }
}

@Composable
private fun FavoriteContactItem(
    contact: MetroContact,
    onClick: (Long) -> Unit,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    val displayName = contact.displayName
    val photoUri = contact.photoUri
    val primaryPhone = contact.primaryPhone ?: "No phone number"

    Row(
        modifier = modifier
            .clickable {
                onClick(contact.id)
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contact photo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
        ) {
            if (photoUri != null) {
                SubcomposeAsyncImage(
                    model = photoUri,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        PhotoFallbackContent(displayName, Color(0xFF1A73E8), metroFont)
                    },
                    error = {
                        PhotoFallbackContent(displayName, Color(0xFF1A73E8), metroFont)
                    }
                )
            } else {
                PhotoFallbackContent(displayName, Color(0xFF1A73E8), metroFont)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                style = MetroTypography.MetroBody1(metroFont).copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = primaryPhone,
                style = MetroTypography.MetroBody2(metroFont).copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Star indicator
        Text(
            text = "★",
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = Color(0xFFFFD700)
            )
        )
    }
}

@Composable
private fun ActiveContactItem(
    contact: MetroContact,
    onClick: (Long) -> Unit,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    val displayName = contact.displayName
    val photoUri = contact.photoUri
    val primaryPhone = contact.primaryPhone ?: "No phone number"

    Row(
        modifier = modifier
            .clickable {
                onClick(contact.id)
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Activity indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFF00FF00))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Contact photo
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
        ) {
            if (photoUri != null) {
                SubcomposeAsyncImage(
                    model = photoUri,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        PhotoFallbackContent(displayName, Color(0xFF1A73E8), metroFont, small = true)
                    },
                    error = {
                        PhotoFallbackContent(displayName, Color(0xFF1A73E8), metroFont, small = true)
                    }
                )
            } else {
                PhotoFallbackContent(displayName, Color(0xFF1A73E8), metroFont, small = true)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                style = MetroTypography.MetroBody2(metroFont).copy(
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = primaryPhone,
                style = MetroTypography.MetroCaption(metroFont).copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PhotoFallbackContent(
    displayName: String,
    accentColor: Color,
    metroFont: MetroFont,
    small: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(accentColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.take(2).uppercase(),
            style = if (small) {
                MetroTypography.MetroCaption(metroFont).copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            } else {
                MetroTypography.MetroBody2(metroFont).copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        )
    }
}

// NOTE: PanoramaCanvas is assumed to be defined elsewhere
// If you need me to optimize PanoramaCanvas too, please share that file


