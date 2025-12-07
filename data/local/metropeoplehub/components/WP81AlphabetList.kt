// File: ui/components/peoplehub/WP81AlphabetList.kt
package com.metromessages.data.local.metropeoplehub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.metromessages.data.local.metropeoplehub.MetroContact
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.launch

@Composable
fun WP81AlphabetList(
    contacts: List<MetroContact>,
    onContactClick: (MetroContact) -> Unit,
    onLetterSelected: (Char) -> Unit,
    metroFont: MetroFont,
    accentColor: Color,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contactItem: @Composable (MetroContact) -> Unit = { contact ->
        DefaultMetroContactItem(contact, onContactClick, metroFont, accentColor)
    }
) {
    var showAlphabetOverlay by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Define header height (should match MetroSectionHeader height)
    val headerHeight = 72.dp
    val headerHeightPx = with(density) { headerHeight.toPx() }

    val sections by remember(contacts) {
        mutableStateOf(contacts.groupBy { contact ->
            when (val firstChar = contact.displayName.uppercase().first()) {
                in 'A'..'Z' -> firstChar
                in '0'..'9' -> '#'
                else -> '#'
            }
        })
    }

    val allLetters = listOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '#'
    )

    val availableLetters by remember(sections) {
        mutableStateOf(sections.keys.filter { sections[it]?.isNotEmpty() == true }.toSet())
    }

    // Get the current section letter based on scroll position
    val currentSectionLetter by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                'A'
            } else {
                // Find which section is at the top
                val firstVisibleIndex = layoutInfo.visibleItemsInfo.first().index

                // Skip the initial spacer (index 0)
                if (firstVisibleIndex == 0) {
                    // We're at the spacer, so no section is visible yet
                    return@derivedStateOf 'A'
                }

                // Track position in the list
                var itemCount = 1 // Start after the spacer

                allLetters.forEach { letter ->
                    val letterContacts = sections[letter] ?: emptyList()
                    if (letterContacts.isNotEmpty()) {
                        // Check if we're at this header
                        if (firstVisibleIndex == itemCount) {
                            return@derivedStateOf letter
                        }
                        itemCount++ // Move past header

                        // Check if we're in the contacts of this section
                        if (firstVisibleIndex < itemCount + letterContacts.size) {
                            return@derivedStateOf letter
                        }

                        itemCount += letterContacts.size // Move past contacts
                    }
                }
                'A' // Fallback
            }
        }
    }

    // Manual scroll clamping - prevent scrolling above the first item
    LaunchedEffect(listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 0) {
            // If trying to scroll above top, snap back
            listState.scrollToItem(0, 0)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            allLetters.forEach { letter ->
                val letterContacts = sections[letter] ?: emptyList()
                if (letterContacts.isNotEmpty()) {
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color.Black)
                        ) {
                            MetroSectionHeader(
                                letter = letter.toString(),
                                onClick = { showAlphabetOverlay = true },
                                metroFont = metroFont,
                                accentColor = accentColor,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                    }

                    items(letterContacts) { contact ->
                        contactItem(contact)
                    }
                }
            }
        }

        // Fixed header that's always at the top (replaces sticky functionality)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(Color.Black) // Or your preferred background color
        ) {
            MetroSectionHeader(
                letter = currentSectionLetter.toString(),
                onClick = { showAlphabetOverlay = true },
                metroFont = metroFont,
                accentColor = accentColor,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        if (showAlphabetOverlay) {
            // Use rememberCoroutineScope to handle the scroll in a coroutine
            val coroutineScope = rememberCoroutineScope()

            AlphabetOverlay(
                letters = allLetters,
                availableLetters = availableLetters,
                onLetterSelected = { selectedLetter ->
                    onLetterSelected(selectedLetter)
                    showAlphabetOverlay = false

                    // Scroll to the selected letter using coroutine scope
                    coroutineScope.launch {
                        // Calculate which item to scroll to
                        var scrollIndex = 1 // Start after the spacer (index 0)

                        allLetters.forEach { letter ->
                            if (letter == selectedLetter) {
                                val letterContacts = sections[letter] ?: emptyList()
                                if (letterContacts.isNotEmpty()) {
                                    listState.scrollToItem(scrollIndex)
                                    return@launch
                                }
                            }

                            val letterContacts = sections[letter] ?: emptyList()
                            if (letterContacts.isNotEmpty()) {
                                scrollIndex += 1 + letterContacts.size // Header + items
                            }
                        }
                    }
                },
                onDismiss = { showAlphabetOverlay = false },
                metroFont = metroFont,
                accentColor = accentColor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// The rest of your existing code remains the same...
@Composable
private fun DefaultMetroContactItem(
    contact: MetroContact,
    onContactClick: (MetroContact) -> Unit,
    metroFont: MetroFont,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onContactClick(contact) }
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
