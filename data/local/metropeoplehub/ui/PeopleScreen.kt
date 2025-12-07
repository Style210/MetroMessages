// File: data/local/peoplescreen/PeopleScreen.kt
package com.metromessages.data.local.metropeoplehub.ui

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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.metromessages.data.local.homescreen.PanoramaCanvas
import com.metromessages.data.local.metropeoplehub.MetroContact
import com.metromessages.data.local.metropeoplehub.MetroPeopleHubViewModel
import com.metromessages.data.local.metropeoplehub.components.PeopleHubLiveTilesGrid
import com.metromessages.data.local.metropeoplehub.components.WP81AlphabetList
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PeopleScreen(
    onBackClick: () -> Unit,
    metroFont: MetroFont,
    accentColor: Color,
    onNavigateToConversation: (String, Long, String?, String?, Int) -> Unit
) {
    val viewModel: MetroPeopleHubViewModel = hiltViewModel()

    println("🚀 ========== PEOPLE SCREEN START ==========")

    val contacts by viewModel.contacts.collectAsState()
    val starredContacts by viewModel.starredContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMessage by viewModel.loadingMessage.collectAsState()

    // ✅ CRITICAL FIX: Add this LaunchedEffect to LOAD contacts when PeopleScreen opens
    LaunchedEffect(Unit) {
        println("👥 PeopleScreen: Checking if we need to load contacts...")

        // Check if we need to load contacts
        if (contacts.isEmpty() && !isLoading) {
            println("📞 PeopleScreen: Loading all contacts...")
            viewModel.loadContacts()
        } else if (contacts.isNotEmpty()) {
            println("✅ PeopleScreen: Already have ${contacts.size} contacts")
        } else if (isLoading) {
            println("🔄 PeopleScreen: Contacts already loading...")
        }
    }

    // ✅ Keep the existing LaunchedEffect for logging
    LaunchedEffect(contacts, starredContacts) {
        println("🔍 ========== FOSSIFY BACKEND DATA ==========")
        println("📊 CONTACTS LOADED: ${contacts.size}")
        println("⭐ STARRED CONTACTS: ${starredContacts.size}")
        println("🔄 LOADING STATE: $isLoading")
        println("💬 LOADING MESSAGE: $loadingMessage")

        contacts.take(3).forEach { contact ->
            println("   👤 ${contact.displayName} (ID: ${contact.id}) - ${contact.primaryPhone ?: "No phone"}")
        }
        println("🔚 ========== END DATA DEBUG ==========")
    }

    val headerScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        println("⬅️ PeopleScreen back button clicked")
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Transparent)
            ) {
                if (isLoading) {
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
                                0 -> {
                                    println("⭐ RENDERING FAVORITES: ${starredContacts.size} contacts")
                                    ContactListContent(
                                        contacts = starredContacts,
                                        viewModel = viewModel,
                                        accentColor = accentColor,
                                        metroFont = metroFont,
                                        pageTitle = "favorites",
                                        onContactClick = { contactId ->
                                            handleContactClick(
                                                contactId = contactId,
                                                contacts = contacts,
                                                onNavigateToConversation = onNavigateToConversation,
                                                source = "favorites"
                                            )
                                        },
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                                1 -> {
                                    println("👥 RENDERING ALL: ${contacts.size} contacts")
                                    // UPDATED: Use WP81AlphabetList for "all" tab only
                                    AllContactsTabContent(
                                        contacts = contacts,
                                        accentColor = accentColor,
                                        metroFont = metroFont,
                                        onContactClick = { contactId ->
                                            handleContactClick(
                                                contactId = contactId,
                                                contacts = contacts,
                                                onNavigateToConversation = onNavigateToConversation,
                                                source = "all"
                                            )
                                        },
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                                2 -> {
                                    println("🆕 RENDERING WHAT'S NEW")
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

                                        val recentContacts = contacts.take(5)
                                        if (recentContacts.isNotEmpty()) {
                                            LazyColumn(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                items(recentContacts) { contact ->
                                                    RecentContactItem(
                                                        contact = contact,
                                                        accentColor = accentColor,
                                                        metroFont = metroFont
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No contacts yet",
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

    println("🏁 ========== PEOPLE SCREEN END ==========")
}

// NEW: Separate composable for All Contacts tab with WP8.1 alphabet navigation
@Composable
private fun AllContactsTabContent(
    contacts: List<MetroContact>,
    accentColor: Color,
    metroFont: MetroFont,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Text(
            text = "all",
            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (contacts.isEmpty()) {
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
            WP81AlphabetList(
                contacts = contacts,
                onContactClick = { contact -> onContactClick(contact.id) },
                onLetterSelected = { letter ->
                    // Scroll to the selected letter
                    listState.scrollToLetter(letter, contacts)
                },
                metroFont = metroFont,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listState = listState
            )
        }
    }
}

// Helper function for scrolling to letters
private fun LazyListState.scrollToLetter(
    letter: Char,
    contacts: List<MetroContact>
) {
    // Launch coroutine to call suspend function
    CoroutineScope(Dispatchers.Main).launch {
        val sections = contacts.groupBy { contact ->
            when (val firstChar = contact.displayName.uppercase().first()) {
                in 'A'..'Z' -> firstChar
                in '0'..'9' -> '#'
                else -> '#'
            }
        }

        val sortedLetters = sections.keys.sorted()
        val targetIndex = sortedLetters.indexOf(letter)
        if (targetIndex == -1) return@launch

        var position = 0
        for (i in 0 until targetIndex) {
            position += 1 // header
            position += sections[sortedLetters[i]]?.size ?: 0
        }

        scrollToItem(position)
    }
}

// Keep the rest of your existing functions unchanged
@Composable
private fun ContactListContent(
    contacts: List<MetroContact>,
    viewModel: MetroPeopleHubViewModel,
    accentColor: Color,
    metroFont: MetroFont,
    pageTitle: String,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedContacts = contacts.groupBy { it.displayName.uppercase().first() }
    val sortedLetters = groupedContacts.keys.sorted()
    val listState = rememberLazyListState()

    println("📋 CONTACT LIST: $pageTitle - ${contacts.size} contacts")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Text(
            text = pageTitle,
            style = MetroTypography.MetroSubhead(metroFont).copy(color = Color.White),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (contacts.isEmpty()) {
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
                            favoriteContacts = contacts,
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

                            items(contactsInGroup) { contact ->
                                LaunchedEffect(contact.id) {
                                    println("👤 DEBUG Contact Item:")
                                    println("   - Contact ID: ${contact.id}")
                                    println("   - Display Name: ${contact.displayName}")
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .clickable {
                                            println("🖱️ DEBUG: Clicking contact: ${contact.displayName} (ID: ${contact.id})")
                                            onContactClick(contact.id)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        if (contact.photoUri != null) {
                                            SubcomposeAsyncImage(
                                                model = contact.photoUri,
                                                contentDescription = contact.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    PhotoFallbackContent(contact.displayName, accentColor, metroFont)
                                                },
                                                error = {
                                                    PhotoFallbackContent(contact.displayName, accentColor, metroFont)
                                                }
                                            )
                                        } else {
                                            PhotoFallbackContent(contact.displayName, accentColor, metroFont)
                                        }
                                    }
                                    Column(modifier = Modifier.padding(start = 16.dp)) {
                                        Text(
                                            text = contact.displayName,
                                            style = MetroTypography.MetroBody1(metroFont).copy(
                                                color = Color.White
                                            )
                                        )
                                        contact.primaryPhone?.let { phone ->
                                            Text(
                                                text = phone,
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
                        }
                    }
                }
            }
        }
    }
}

private fun handleContactClick(
    contactId: Long,
    contacts: List<MetroContact>,
    onNavigateToConversation: (String, Long, String?, String?, Int) -> Unit,
    source: String
) {
    println("🖱️ CONTACT CLICK: ID=$contactId, Source=$source")

    if (contactId == 0L) {
        println("❌ ERROR: contactId is 0!")
        return
    }

    val contact = contacts.find { it.id == contactId }

    if (contact == null) {
        println("❌ CONTACT NOT FOUND: $contactId")
        return
    }

    val conversationId = "sms_${contactId}"

    println("🚀 NAVIGATING: ${contact.displayName} -> $conversationId")

    onNavigateToConversation(
        conversationId,
        contactId,
        contact.displayName,
        contact.photoUri,
        0
    )

    println("✅ NAVIGATION SUCCESS")
}

@Composable
private fun RecentContactItem(
    contact: MetroContact,
    accentColor: Color,
    metroFont: MetroFont
) {
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
                text = contact.primaryPhone ?: "No phone number",
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
