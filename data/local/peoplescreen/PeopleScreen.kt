package com.metromessages.data.local.peoplescreen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.metromessages.data.local.homescreen.PanoramaCanvas
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.CoroutineScope

@Composable
fun PeopleScreen(
    onBackClick: () -> Unit,
    metroFont: MetroFont,
    accentColor: Color,
    onNavigateToConversation: (String, Long, String?, String?, Int) -> Unit
) {
    val viewModel: PeopleScreenViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? Activity

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    // Collect view model states
    val people by viewModel.people.collectAsState()
    val starredPeople by viewModel.starredPeople.collectAsState()
    val unifiedContacts by viewModel.unifiedContacts.collectAsState()
    val activeContacts by viewModel.activeContacts.collectAsState()
    val isLoading by viewModel.isLoading
    val loadingMessage by viewModel.loadingMessage

    // INDEPENDENT SCROLL STATES
    val headerScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    // SYNCHRONIZE: Header moves at 35% speed
    LaunchedEffect(contentScrollState.value) {
        val headerTarget = (contentScrollState.value * 0.35f).toInt()
        headerScrollState.scrollTo(headerTarget)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // HEADER AREA - Fixed height (matches Settings screen pattern)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onBackClick()
                    }
            ) {
                MetroHeaderCanvas(
                    text = "people",
                    scrollState = headerScrollState,
                    metroFont = metroFont,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                )
            }

            // CONTENT AREA - Takes remaining space (weight 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Transparent)
            ) {
                // Show loading state if still initializing
                if (isLoading && viewModel.shouldShowImportDialog.value) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loadingMessage,
                            style = MetroTypography.MetroBody2(metroFont).copy(
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                } else {
                    // CONTENT PANORAMA - Now fills the weighted space
                    PanoramaCanvas(
                        scrollState = contentScrollState,
                        modifier = Modifier.fillMaxSize(),
                        panoramaFactor = 3f,
                        edgePeekDp = 32.dp,
                        pageCount = 3,
                        onProgressChange = {},
                        onPageChanged = {}
                    ) { pageIndex, pageWidth, pageOpacity ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(pageWidth)
                                .alpha(pageOpacity)
                                .background(Color.Transparent)
                        ) {
                            when (pageIndex) {
                                0 -> { // "favorites" page
                                    ContactListContent(
                                        people = starredPeople,
                                        unifiedContacts = unifiedContacts,
                                        viewModel = viewModel,
                                        accentColor = accentColor,
                                        metroFont = metroFont,
                                        pageTitle = "favorites",
                                        onContactClick = { contactId ->
                                            val contact = unifiedContacts.find { it.id == contactId }
                                            val conversationId = contact?.smsConversationId ?: "fb_$contactId"
                                            onNavigateToConversation(
                                                conversationId,
                                                contactId,
                                                contact?.displayName ?: "Unknown Contact",
                                                contact?.photoUri,
                                                0
                                            )
                                        },
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                                1 -> { // "all" page
                                    ContactListContent(
                                        people = people,
                                        unifiedContacts = unifiedContacts,
                                        viewModel = viewModel,
                                        accentColor = accentColor,
                                        metroFont = metroFont,
                                        pageTitle = "all",
                                        onContactClick = { contactId ->
                                            val contact = unifiedContacts.find { it.id == contactId }
                                            val conversationId = contact?.smsConversationId ?: "fb_$contactId"
                                            onNavigateToConversation(
                                                conversationId,
                                                contactId,
                                                contact?.displayName ?: "Unknown Contact",
                                                contact?.photoUri,
                                                0
                                            )
                                        },
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                                2 -> { // "what's new" page
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(start = 24.dp)
                                            .background(Color.Transparent)
                                    ) {
                                        Text(
                                            text = "what's new",
                                            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )

                                        if (activeContacts.isNotEmpty()) {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                items(activeContacts.take(5)) { contact ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .background(accentColor),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = contact.displayName.take(1).uppercase(),
                                                                style = MetroTypography.MetroBody2(metroFont).copy(
                                                                    color = Color.Black,
                                                                    fontSize = 14.sp
                                                                )
                                                            )
                                                        }
                                                        Column(modifier = Modifier.padding(start = 12.dp)) {
                                                            Text(
                                                                text = contact.displayName,
                                                                style = MetroTypography.MetroBody2(metroFont).copy(
                                                                    color = Color.White,
                                                                    fontSize = 14.sp
                                                                )
                                                            )
                                                            Text(
                                                                text = contact.lastMessage ?: "New activity",
                                                                style = MetroTypography.MetroBody2(metroFont).copy(
                                                                    color = Color.White.copy(alpha = 0.7f),
                                                                    fontSize = 12.sp
                                                                ),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No updates yet",
                                                    style = MetroTypography.MetroBody2(metroFont).copy(color = Color.White),
                                                    modifier = Modifier.padding(vertical = 8.dp)
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
    }
}

@Composable
private fun ContactListContent(
    people: List<PersonWithDetails>,
    unifiedContacts: List<com.metromessages.data.repository.UnifiedContact>,
    viewModel: PeopleScreenViewModel,
    accentColor: Color,
    metroFont: MetroFont,
    pageTitle: String,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedContacts = people.groupBy { it.person.displayName.uppercase().first() }
    val sortedLetters = groupedContacts.keys.sorted()
    val listState = rememberLazyListState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Simple subheader
        Text(
            text = pageTitle,
            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Content
        if (people.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No contacts yet",
                    style = MetroTypography.MetroBody2(metroFont).copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        } else {
            when (pageTitle) {
                "favorites" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Transparent)
                    ) {
                        PeopleHubLiveTilesGrid(
                            favoriteContacts = people,
                            viewModel = viewModel,
                            accentColor = accentColor,
                            onContactClick = onContactClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Transparent)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Transparent)
                    ) {
                        sortedLetters.forEach { letter ->
                            val contactsInGroup = groupedContacts[letter] ?: emptyList()

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letter.toString(),
                                            style = MetroTypography.MetroBody1(metroFont).copy(
                                                color = Color.Black
                                            )
                                        )
                                    }
                                    Text(
                                        text = letter.toString(),
                                        style = MetroTypography.MetroBody1(metroFont).copy(
                                            color = Color.White
                                        ),
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }

                            items(contactsInGroup) { person ->
                                val unifiedContact = unifiedContacts.find { it.id == person.person.id }
                                val contactPhotoUri = unifiedContact?.photoUri ?: person.person.photoUri
                                val displayName = unifiedContact?.displayName ?: person.person.displayName

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clickable { onContactClick(person.person.id) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        if (contactPhotoUri != null) {
                                            SubcomposeAsyncImage(
                                                model = contactPhotoUri,
                                                contentDescription = displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    PhotoFallbackContent(displayName, accentColor, metroFont)
                                                },
                                                error = {
                                                    PhotoFallbackContent(displayName, accentColor, metroFont)
                                                }
                                            )
                                        } else {
                                            PhotoFallbackContent(displayName, accentColor, metroFont)
                                        }
                                    }
                                    Text(
                                        text = displayName,
                                        style = MetroTypography.MetroBody1(metroFont).copy(
                                            color = Color.White
                                        ),
                                        modifier = Modifier.padding(start = 16.dp)
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

@Composable
private fun PhotoFallbackContent(
    displayName: String,
    accentColor: Color,
    metroFont: MetroFont
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(accentColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.take(2).uppercase(),
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        )
    }
}