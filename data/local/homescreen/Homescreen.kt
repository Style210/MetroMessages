// File: data/local/homescreen/HomeScreen.kt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.metromessages.data.local.peoplescreen.PeopleScreenViewModel
import com.metromessages.data.local.peoplescreen.PersonWithDetails
import com.metromessages.data.repository.UnifiedContact
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.navigation.MetroDestinations
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography

@Composable
fun HomeScreen(
    viewModel: PeopleScreenViewModel = hiltViewModel(),
    navController: NavController,
    metroFont: MetroFont
) {
    val context = LocalContext.current
    val activity = context as? Activity
    rememberCoroutineScope()



    // Collect favorites data from PeopleScreenViewModel
    val starredPeople by viewModel.starredPeople.collectAsState()
    val unifiedContacts by viewModel.unifiedContacts.collectAsState()
    val activeContacts by viewModel.activeContacts.collectAsState()

    val headerScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    LaunchedEffect(contentScrollState.value) {
        val headerTarget = (contentScrollState.value * 0.35f).toInt()
        headerScrollState.scrollTo(headerTarget)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header area (fixed height) - UPDATED: Make entire header clickable to close app
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // ✅ CHANGED: Close the app when tapping home header
                        activity?.finish()
                    }
            ) {
                // ✅ UPDATED: Added accentColor for gradient
                MetroHeaderCanvas(
                    text = "home",
                    scrollState = headerScrollState,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                    metroFont = metroFont,

                )
            }

            // Content area
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
                                            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
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
                                            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
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
                                            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
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
                                            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
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
                                            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        1 -> { // Favorites page with real data
                            Text(
                                text = "favorites",
                                style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            if (starredPeople.isEmpty()) {
                                Text(
                                    text = "No favorites yet",
                                    style = MetroTypography.MetroBody2(metroFont).copy(color = Color.White),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(starredPeople) { person ->
                                        FavoriteContactItem(
                                            person = person,
                                            unifiedContacts = unifiedContacts,
                                            onClick = { contactId ->
                                                // Use SmsConversation (which uses Facebook data structures)
                                                val contact = unifiedContacts.find { it.id == contactId }
                                                val conversationId = contact?.smsConversationId ?: "fb_$contactId"
                                                val contactName = contact?.displayName ?: "Unknown Contact"
                                                val contactPhotoUrl = contact?.photoUri

                                                navController.navigate(
                                                    MetroDestinations.SmsConversation.createRoute(
                                                        conversationId = conversationId,
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
                        2 -> { // What's New page with real data
                            Text(
                                text = "what's new",
                                style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            if (activeContacts.isEmpty()) {
                                Text(
                                    text = "Nothing new right now",
                                    style = MetroTypography.MetroBody2(metroFont).copy(color = Color.White),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    items(activeContacts.take(5)) { contact ->
                                        ActiveContactItem(
                                            contact = contact,
                                            onClick = { contactId ->
                                                // Use SmsConversation (which uses Facebook data structures)
                                                val conversationId = contact.smsConversationId ?: "fb_$contactId"
                                                val contactName = contact.displayName
                                                val contactPhotoUrl = contact.photoUri

                                                navController.navigate(
                                                    MetroDestinations.SmsConversation.createRoute(
                                                        conversationId = conversationId,
                                                        contactName = contactName,
                                                        contactPhotoUrl = contactPhotoUrl,
                                                        initialTab = 1
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
                    }
                }
            }
        }
    }
}

// Composable for favorite contact items
@Composable
private fun FavoriteContactItem(
    person: PersonWithDetails,
    unifiedContacts: List<UnifiedContact>,
    onClick: (Long) -> Unit,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    val unifiedContact = unifiedContacts.find { it.id == person.person.id }
    val displayName = unifiedContact?.displayName ?: person.person.displayName
    val photoUri = unifiedContact?.photoUri ?: person.person.photoUri
    val lastActivity = unifiedContact?.lastMessage ?: "No recent activity"

    Row(
        modifier = modifier
            .clickable { onClick(person.person.id) }
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
                text = lastActivity,
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

// Composable for active contact items
@Composable
private fun ActiveContactItem(
    contact: UnifiedContact,
    onClick: (Long) -> Unit,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onClick(contact.id) }
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
            if (contact.photoUri != null) {
                SubcomposeAsyncImage(
                    model = contact.photoUri,
                    contentDescription = contact.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        PhotoFallbackContent(contact.displayName, Color(0xFF1A73E8), metroFont, small = true)
                    },
                    error = {
                        PhotoFallbackContent(contact.displayName, Color(0xFF1A73E8), metroFont, small = true)
                    }
                )
            } else {
                PhotoFallbackContent(contact.displayName, Color(0xFF1A73E8), metroFont, small = true)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = contact.displayName,
                style = MetroTypography.MetroBody2(metroFont).copy(
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.lastMessage ?: "New activity",
                style = MetroTypography.MetroCaption(metroFont).copy(
                    color = Color.White.copy(alpha = 0.7f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Photo fallback for when no photo is available
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
