// File: data/local/peoplescreen/MetroContactEditor.kt
package com.metromessages.data.local.peoplescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.metromessages.attachments.MetroMediaPickerSheet
import com.metromessages.data.repository.UnifiedContact
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography

@Composable
fun MetroContactEditor(
    contact: UnifiedContact,
    detailedContact: PersonWithDetails?,
    metroFont: MetroFont,
    onSave: (ContactEdits) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedContact by remember {
        mutableStateOf(ContactEdits.from(contact, detailedContact))
    }

    // ✅ ADD: State for showing photo picker
    var showPhotoPicker by remember { mutableStateOf(false) }

    // Reset edited contact when contact changes
    LaunchedEffect(contact, detailedContact) {
        editedContact = ContactEdits.from(contact, detailedContact)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // HEADER - MetroUI style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        text = "edit contact",
                        modifier = Modifier
                            .offset(x = 5.dp, y = 40.dp)
                            .clickable { onDismiss() },
                        color = Color.White,
                        style = MetroTypography.MetroSubhead(metroFont)
                    )
                }

                // CONTENT
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // PROFILE PHOTO SECTION - Enhanced with your photo picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Profile Photo - Square with sharp corners
                            Box(
                                modifier = Modifier
                                    .size(120.dp) // ✅ Larger for better visibility
                                    .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                                    .clickable {
                                        showPhotoPicker = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (editedContact.photoUri != null) {
                                    AsyncImage(
                                        model = editedContact.photoUri,
                                        contentDescription = "Profile photo",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Add photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                // ✅ ADD: Edit badge overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ✅ ENHANCED: Photo action buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Photo Picker button
                                MetroOutlineBox(
                                    title = "choose photo",
                                    onClick = { showPhotoPicker = true },
                                    metroFont = metroFont,
                                    modifier = Modifier
                                        .height(40.dp)
                                        .weight(1f),
                                    backgroundColor = Color(0xFF1A73E8) // Accent blue
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Remove photo option (only show if there's a photo)
                            if (editedContact.photoUri != null) {
                                Text(
                                    text = "remove photo",
                                    style = MetroTypography.MetroBody2(metroFont).copy(
                                        color = Color.Red.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            editedContact = editedContact.copy(photoUri = null)
                                            println("🗑️ Removed contact photo")
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }

                    // DISPLAY NAME
                    EditField(
                        label = "name",
                        value = editedContact.displayName,
                        onValueChange = { editedContact = editedContact.copy(displayName = it) },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PHONE NUMBERS SECTION
                    Text(
                        text = "phones",
                        style = MetroTypography.MetroBody1(metroFont).copy(
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Home Phone
                    EditField(
                        label = "home phone",
                        value = editedContact.homePhone,
                        onValueChange = { newValue ->
                            editedContact = editedContact.updatePhone(2, newValue, "Home")
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cell Phone
                    EditField(
                        label = "cell phone",
                        value = editedContact.cellPhone,
                        onValueChange = { newValue ->
                            editedContact = editedContact.updatePhone(1, newValue, "Mobile")
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Work Phone
                    EditField(
                        label = "work phone",
                        value = editedContact.workPhone,
                        onValueChange = { newValue ->
                            editedContact = editedContact.updatePhone(3, newValue, "Work")
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Other Phone
                    EditField(
                        label = "other phone",
                        value = editedContact.otherPhone,
                        onValueChange = { newValue ->
                            editedContact = editedContact.updatePhone(0, newValue, "Other")
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // EMAIL ADDRESSES SECTION
                    Text(
                        text = "emails",
                        style = MetroTypography.MetroBody1(metroFont).copy(
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Work Email
                    EditField(
                        label = "work email",
                        value = editedContact.workEmail,
                        onValueChange = { newValue ->
                            editedContact = editedContact.updateEmail(2, newValue, "Work")
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Private Email
                    EditField(
                        label = "private email",
                        value = editedContact.privateEmail,
                        onValueChange = { newValue ->
                            editedContact = editedContact.updateEmail(1, newValue, "Personal")
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ADDITIONAL INFORMATION
                    Text(
                        text = "additional info",
                        style = MetroTypography.MetroBody1(metroFont).copy(
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Birthday
                    EditField(
                        label = "birthday",
                        value = editedContact.birthday,
                        onValueChange = { editedContact = editedContact.copy(birthday = it) },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Address
                    EditField(
                        label = "address",
                        value = editedContact.address,
                        onValueChange = { editedContact = editedContact.copy(address = it) },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth(),
                        isMultiline = true
                    )

                    // Notes
                    EditField(
                        label = "notes",
                        value = editedContact.notes,
                        onValueChange = { editedContact = editedContact.copy(notes = it) },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth(),
                        isMultiline = true
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ACTION BUTTONS - Sharp corners
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel Button
                        MetroOutlineBox(
                            title = "cancel",
                            onClick = onDismiss,
                            metroFont = metroFont,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            backgroundColor = Color(0xFF2A2A2A)
                        )

                        // Save Button
                        MetroOutlineBox(
                            title = "save",
                            onClick = {
                                println("💾 MetroContactEditor: Saving contact with photo: ${editedContact.photoUri}")
                                onSave(editedContact)
                            },
                            metroFont = metroFont,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            backgroundColor = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // ✅ ADD: Your existing photo picker sheet
        if (showPhotoPicker) {
            MetroMediaPickerSheet(
                onDismiss = { showPhotoPicker = false },
                onMediaSelected = { uris ->
                    // Take only the first selected photo for contact photo
                    uris.firstOrNull()?.let { selectedUri ->
                        editedContact = editedContact.copy(photoUri = selectedUri.toString())
                        println("📸 Selected new contact photo: $selectedUri")
                    }
                    showPhotoPicker = false
                },
                accentColor = Color(0xFF1A73E8), // Use a nice blue accent
                metroFont = metroFont
            )
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    metroFont: MetroFont,
    modifier: Modifier = Modifier,
    isMultiline: Boolean = false
) {
    var textState by remember { mutableStateOf(TextFieldValue(value)) }
    val focusRequester = remember { FocusRequester() }

    // Update text state when value changes externally
    LaunchedEffect(value) {
        if (textState.text != value) {
            textState = TextFieldValue(value)
        }
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Label - MetroBody2
        Text(
            text = label,
            style = MetroTypography.MetroBody2(metroFont).copy(
                color = Color.White
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Input field - Sharp corners, white border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.White, RoundedCornerShape(0.dp)) // Sharp corners
                .clickable { focusRequester.requestFocus() }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = textState,
                onValueChange = {
                    textState = it
                    onValueChange(it.text)
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 20.sp, // MetroBody2 size
                    fontFamily = metroFont.fontFamily,
                    lineHeight = 24.sp, // MetroBody2 line height
                    letterSpacing = (-0.25).sp // MetroBody2 letter spacing
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = !isMultiline,
                maxLines = if (isMultiline) 3 else 1
            )

            // Show placeholder when empty - MetroBody2 style
            if (textState.text.isEmpty()) {
                Text(
                    text = "enter $label",
                    style = MetroTypography.MetroBody2(metroFont).copy(
                        color = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
private fun MetroOutlineBox(
    title: String,
    onClick: () -> Unit,
    metroFont: MetroFont,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF444444)
) {
    Box(
        modifier = modifier
            .border(2.dp, Color.White, RoundedCornerShape(0.dp)) // Sharp corners
            .background(backgroundColor, RoundedCornerShape(0.dp)) // Sharp corners
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MetroTypography.MetroBody1(metroFont).copy(
                color = if (backgroundColor == Color.White) Color.Black else Color.White
            )
        )
    }
}

// Enhanced Data classes for editing - MAINTAINS COMPATIBILITY with your ViewModel
data class ContactEdits(
    val displayName: String,
    val photoUri: String? = null,

    // Keep the original list structure for ViewModel compatibility
    val phones: List<PhoneEdit> = emptyList(),
    val emails: List<EmailEdit> = emptyList(),

    // Additional fields
    val birthday: String = "",
    val address: String = "",
    val notes: String = ""
) {
    // Helper properties for individual field access in UI
    val homePhone: String get() = phones.find { it.type == 2 }?.number ?: ""
    val cellPhone: String get() = phones.find { it.type == 1 }?.number ?: ""
    val workPhone: String get() = phones.find { it.type == 3 }?.number ?: ""
    val otherPhone: String get() = phones.find { it.type == 0 }?.number ?: ""
    val workEmail: String get() = emails.find { it.type == 2 }?.address ?: ""
    val privateEmail: String get() = emails.find { it.type == 1 }?.address ?: ""

    // Helper methods to update individual fields while maintaining list structure
    fun updatePhone(type: Int, number: String, label: String): ContactEdits {
        val updatedPhones = phones.filterNot { it.type == type } +
                if (number.isNotBlank()) PhoneEdit(number, type, label) else null
        return copy(phones = updatedPhones.filterNotNull())
    }

    fun updateEmail(type: Int, address: String, label: String): ContactEdits {
        val updatedEmails = emails.filterNot { it.type == type } +
                if (address.isNotBlank()) EmailEdit(address, type, label) else null
        return copy(emails = updatedEmails.filterNotNull())
    }

    companion object {
        fun from(contact: UnifiedContact, detailedContact: PersonWithDetails?): ContactEdits {
            val phones = if (detailedContact?.phones != null) {
                // Use detailed contact phones if available
                detailedContact.phones.map { phone ->
                    PhoneEdit(
                        number = phone.number,
                        type = phone.type,
                        label = phone.label
                    )
                }
            } else {
                // Fallback: Create phone from UnifiedContact's phoneNumber
                if (contact.phoneNumber != null) {
                    listOf(PhoneEdit(
                        number = contact.phoneNumber,
                        type = 1, // Default to mobile
                        label = "Mobile"
                    ))
                } else {
                    emptyList()
                }
            }

            val emails = detailedContact?.emails?.map { email ->
                EmailEdit(
                    address = email.address,
                    type = email.type,
                    label = email.label
                )
            } ?: emptyList()

            return ContactEdits(
                displayName = contact.displayName,
                photoUri = contact.photoUri,
                phones = phones,
                emails = emails,
                birthday = "",
                address = "",
                notes = ""
            )
        }
    }

    // Helper methods to check if fields are empty (for display logic)
    fun hasPhoneNumbers(): Boolean = phones.isNotEmpty()
    fun hasEmails(): Boolean = emails.isNotEmpty()
    fun hasAdditionalInfo(): Boolean =
        birthday.isNotBlank() || address.isNotBlank() || notes.isNotBlank()
}

// Keep the original data classes for compatibility
data class PhoneEdit(
    val number: String,
    val type: Int,
    val label: String?
)

data class EmailEdit(
    val address: String,
    val type: Int,
    val label: String?
)
