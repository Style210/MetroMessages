// File: ui/contacts/ContactInfoPage.kt
package com.metromessages.data.local.metropeoplehub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.metromessages.data.local.metropeoplehub.ContactEdits
import com.metromessages.data.local.metropeoplehub.MetroContact
import com.metromessages.data.local.metropeoplehub.MetroPeopleHubViewModel
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography

@Composable
fun ContactInfoPage(
    contactId: String,
    accentColor: Color,
    metroFont: MetroFont,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MetroPeopleHubViewModel = hiltViewModel()
    val currentContact by viewModel.currentContact.collectAsState()
    val editState by viewModel.editState.collectAsState()

    // ✅ Convert String to Long for ViewModel
    val contactIdLong = contactId.toLongOrNull()

    // Load contact when page opens or contactId changes
    LaunchedEffect(contactIdLong) {
        if (contactIdLong != null) {
            println("🔍 ContactInfoPage: Loading contact $contactId")
            viewModel.loadContactForEditing(contactIdLong)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) // ✅ TRANSPARENT BACKGROUND
    ) {
        // ✅ NO HEADER - removed completely

        // ✅ CONTENT AREA - Left-aligned, transparent
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 32.dp)
        ) {
            if (currentContact != null) {
                ContactInfoContent(
                    contact = currentContact!!,
                    viewModel = viewModel,
                    accentColor = accentColor,
                    metroFont = metroFont
                )
            } else {
                // Loading or error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        contactIdLong == null -> {
                            Text(
                                text = "Invalid contact",
                                style = MetroTypography.MetroBody1(metroFont).copy(color = Color.Red)
                            )
                        }
                        editState is MetroPeopleHubViewModel.ContactEditState.Loading -> {
                            Text(
                                text = "Loading...",
                                style = MetroTypography.MetroBody1(metroFont).copy(color = Color.White)
                            )
                        }
                        editState is MetroPeopleHubViewModel.ContactEditState.Error -> {
                            Text(
                                text = "Error loading",
                                style = MetroTypography.MetroBody1(metroFont).copy(color = Color.Red)
                            )
                        }
                        else -> {
                            Text(
                                text = "Contact not found",
                                style = MetroTypography.MetroBody1(metroFont).copy(color = Color.White)
                            )
                        }
                    }
                }
            }
        }

        // ✅ 3-DOT MENU - Bottom-right corner (Metro design)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Edit contact",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactInfoContent(
    contact: MetroContact,
    viewModel: MetroPeopleHubViewModel,
    accentColor: Color,
    metroFont: MetroFont
) {
    val contactEdits = remember(contact) {
        ContactEdits.fromMetroContact(contact)
    }

    Column {
        // ✅ PROFILE SECTION - Large left-focused photo with favorite star overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 16.dp, bottom = 32.dp)
        ) {
            // Large Profile Photo - Left-focused
            if (contact.photoUri != null) {
                Box(
                    modifier = Modifier.size(240.dp)
                ) {
                    AsyncImage(
                        model = contact.photoUri,
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(0.dp))
                    )

                    // ✅ FAVORITE STAR - Top-right corner overlay
                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(contact.id, !contact.starred)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(44.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (contact.starred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (contact.starred) "Unstar" else "Star",
                            tint = if (contact.starred) accentColor else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.displayName.take(2).uppercase(),
                            style = MetroTypography.MetroBody1(metroFont).copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp
                            )
                        )
                    }

                    // ✅ FAVORITE STAR - Top-right corner overlay
                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(contact.id, !contact.starred)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .size(44.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (contact.starred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (contact.starred) "Unstar" else "Star",
                            tint = if (contact.starred) accentColor else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ✅ CONTACT DETAILS - Left-aligned with photo
        Column(
            modifier = Modifier.padding(start = 32.dp)
        ) {
            // Phone Numbers Section
            val phoneItems = listOf(
                "Mobile" to contactEdits.cellPhone,
                "Home" to contactEdits.homePhone,
                "Work" to contactEdits.workPhone,
                "Other" to contactEdits.otherPhone
            ).filter { it.second.isNotBlank() }

            if (phoneItems.isNotEmpty()) {
                Text(
                    text = "Phone Numbers",
                    style = MetroTypography.MetroBody1(metroFont).copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                phoneItems.forEach { (label, number) ->
                    ContactInfoItem(
                        label = label,
                        value = number,
                        metroFont = metroFont
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Email Addresses Section
            val emailItems = listOf(
                "Work Email" to contactEdits.workEmail,
                "Personal Email" to contactEdits.privateEmail
            ).filter { it.second.isNotBlank() }

            if (emailItems.isNotEmpty()) {
                Text(
                    text = "Email Addresses",
                    style = MetroTypography.MetroBody1(metroFont).copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                emailItems.forEach { (label, email) ->
                    ContactInfoItem(
                        label = label,
                        value = email,
                        metroFont = metroFont
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Notes Section
            if (contactEdits.notes.isNotBlank()) {
                Text(
                    text = "Notes",
                    style = MetroTypography.MetroBody1(metroFont).copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ContactInfoItem(
                    label = "Additional Information",
                    value = contactEdits.notes,
                    metroFont = metroFont
                )
            }
        }
    }
}

@Composable
private fun ContactInfoItem(
    label: String,
    value: String,
    metroFont: MetroFont
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = Color.White.copy(alpha = 0.7f)
            )
        )
        Text(
            text = value,
            style = MetroTypography.MetroBody1(metroFont).copy(
                color = Color.White
            )
        )
    }
}