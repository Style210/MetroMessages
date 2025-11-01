// File: ui/components/MetroContactCard.kt
package com.metromessages.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.metromessages.data.local.peoplescreen.ContactEdits
import com.metromessages.data.local.peoplescreen.MetroContactEditor
import com.metromessages.data.local.peoplescreen.PersonWithDetails
import com.metromessages.ui.theme.MetroTypography

@Composable
fun MetroContactCard(
    contact: com.metromessages.data.repository.UnifiedContact,
    detailedContact: PersonWithDetails? = null,
    accentColor: Color,
    metroFont: MetroFont,
    onFavoriteToggle: (Long, Boolean) -> Unit = { _, _ -> },
    onContactUpdated: (ContactEdits) -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showEditor by remember { mutableStateOf(false) }
    var currentContact by remember { mutableStateOf(contact) }

    // Update local state when contact changes
    LaunchedEffect(contact) {
        currentContact = contact
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) { // ✅ CHANGED: Transparent background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color.Transparent) // ✅ CHANGED: Transparent background
        ) {
            // HEADER
            Text(
                text = "contact information",
                style = MetroTypography.MetroBody1(metroFont).copy(
                    color = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // PHOTO AND QUICK INFO ROW
            PhotoAndQuickInfoSection(
                contact = currentContact,
                detailedContact = detailedContact,
                accentColor = accentColor,
                metroFont = metroFont,
                onFavoriteToggle = { starred ->
                    onFavoriteToggle(currentContact.id, starred)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // CONTACT NAME (below photo)
            Text(
                text = currentContact.displayName,
                style = MetroTypography.MetroBody2(metroFont).copy(
                    color = Color.White,
                    fontSize = 24.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // CONTACT DETAILS LIST
            ContactDetailsSection(
                contact = currentContact,
                detailedContact = detailedContact,
                accentColor = accentColor,
                metroFont = metroFont,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // BOTTOM SPACER FOR 3-DOT MENU
            Spacer(modifier = Modifier.height(60.dp))
        }

        // 3-DOT MENU (positioned in bottom-right corner)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = "•••",
                style = MetroTypography.MetroBody2(metroFont).copy(
                    color = accentColor
                ),
                modifier = Modifier
                    .clickable { showEditor = true }
                    .padding(16.dp)
            )
        }

        // CONTACT EDITOR DIALOG
        if (showEditor) {
            MetroContactEditor(
                contact = currentContact,
                detailedContact = detailedContact,
                metroFont = metroFont,
                onSave = { editedContact ->
                    onContactUpdated(editedContact)
                    showEditor = false

                    // Update local state immediately for better UX
                    currentContact = currentContact.copy(
                        displayName = editedContact.displayName,
                        photoUri = editedContact.photoUri
                    )
                },
                onDismiss = { showEditor = false }
            )
        }
    }
}

@Composable
private fun PhotoAndQuickInfoSection(
    contact: com.metromessages.data.repository.UnifiedContact,
    detailedContact: PersonWithDetails?,
    accentColor: Color,
    metroFont: MetroFont,
    onFavoriteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // PHOTO WITH FAVORITE STAR
        Box(
            modifier = Modifier
                .size(220.dp)
        ) {
            // CONTACT PHOTO
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photoUri != null) {
                    SubcomposeAsyncImage(
                        model = contact.photoUri,
                        contentDescription = "Contact photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            PhotoInitialsPlaceholder(contact.displayName, accentColor, metroFont)
                        },
                        error = {
                            PhotoInitialsPlaceholder(contact.displayName, accentColor, metroFont)
                        }
                    )
                } else {
                    PhotoInitialsPlaceholder(contact.displayName, accentColor, metroFont)
                }
            }

            // FAVORITE STAR (top-right corner of photo)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { onFavoriteToggle(!contact.starred) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contact.starred) "★" else "☆",
                    style = MetroTypography.MetroBody2(metroFont).copy(
                        color = if (contact.starred) accentColor else Color.White,
                        fontSize = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // QUICK INFO (Birthday, Address, Notes)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            QuickInfoRow(
                label = "Birthday",
                value = "Not set",
                accentColor = accentColor,
                metroFont = metroFont
            )

            QuickInfoRow(
                label = "Address",
                value = "Not set",
                accentColor = accentColor,
                metroFont = metroFont
            )

            QuickInfoRow(
                label = "Notes",
                value = "No notes",
                accentColor = accentColor,
                metroFont = metroFont
            )
        }
    }
}

@Composable
private fun QuickInfoRow(
    label: String,
    value: String,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = accentColor.copy(alpha = 0.8f)
            )
        )
        Text(
            text = value,
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = Color.White
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContactDetailsSection(
    contact: com.metromessages.data.repository.UnifiedContact,
    detailedContact: PersonWithDetails?,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // PHONE NUMBERS
        val phoneNumbers = buildList {
            // Primary phone from unified contact
            contact.phoneNumber?.let { add(ContactDetail("call mobile", it)) }

            // Additional phones from detailed contact
            detailedContact?.phones?.forEach { phone ->
                val label = when (phone.type) {
                    1 -> "call mobile"
                    2 -> "call home"
                    3 -> "call work"
                    else -> "call"
                }
                add(ContactDetail(label, phone.number))
            }
        }

        // Only show phone section if we have phones
        if (phoneNumbers.isNotEmpty()) {
            phoneNumbers.forEach { phoneDetail ->
                ContactDetailRow(
                    label = phoneDetail.label,
                    value = phoneDetail.value,
                    accentColor = accentColor,
                    metroFont = metroFont
                )
            }
        }

        // EMAIL ADDRESSES - Only show if we have emails
        val emails = detailedContact?.emails ?: emptyList()
        if (emails.isNotEmpty()) {
            emails.forEach { email ->
                ContactDetailRow(
                    label = "email",
                    value = email.address,
                    accentColor = accentColor,
                    metroFont = metroFont
                )
            }
        }

        // SMS AVAILABILITY - Only show if contact has SMS activity
        if (contact.hasSmsActivity()) {
            ContactDetailRow(
                label = "available on",
                value = "SMS",
                accentColor = accentColor,
                metroFont = metroFont
            )
        }

        // LAST ACTIVITY - Always show
        ContactDetailRow(
            label = "last activity",
            value = formatLastActivity(contact.lastActivity),
            accentColor = accentColor,
            metroFont = metroFont
        )
    }
}

@Composable
private fun ContactDetailRow(
    label: String,
    value: String,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MetroTypography.MetroBody1(metroFont).copy(
                color = Color.White
            )
        )
        Text(
            text = value,
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = accentColor
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PhotoInitialsPlaceholder(
    displayName: String,
    accentColor: Color,
    metroFont: MetroFont
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.take(2).uppercase(),
            style = MetroTypography.MetroSubhead(metroFont).copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            )
        )
    }
}

// Helper data class
private data class ContactDetail(
    val label: String,
    val value: String
)

// Helper function for last activity
private fun formatLastActivity(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000} minutes ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        else -> "${diff / 86400000} days ago"
    }
}