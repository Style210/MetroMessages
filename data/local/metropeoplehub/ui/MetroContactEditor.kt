// File: ui/contacts/MetroContactEditor.kt
package com.metromessages.data.local.metropeoplehub.ui

import android.provider.ContactsContract
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
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.metromessages.attachments.MetroMediaPickerSheet
import com.metromessages.data.local.metropeoplehub.MetroPeopleHubViewModel
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography

@Composable
fun MetroContactEditor(
    contactId: Long? = null, // ✅ FIXED: Changed from String? to Long? - null for new contact
    accentColor: Color,
    metroFont: MetroFont,
    onSaveComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: MetroPeopleHubViewModel = hiltViewModel()
    val currentContact by viewModel.currentContact.collectAsState()
    val editState by viewModel.editState.collectAsState()

    // ✅ ACTUALLY USING: ContactEdits with helper methods
    var editedContact by remember {
        mutableStateOf(
            if (contactId != null && currentContact != null) {
                com.metromessages.data.local.metropeoplehub.ContactEdits.fromMetroContact(currentContact!!)
            } else {
                com.metromessages.data.local.metropeoplehub.ContactEdits(displayName = "")
            }
        )
    }

    var showPhotoPicker by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // Load contact for editing when contactId is provided
    LaunchedEffect(contactId) {
        if (contactId != null) {
            println("🔍 MetroContactEditor: Loading contact $contactId for editing")
            viewModel.loadContactForEditing(contactId) // ✅ FIXED: Now passing Long instead of String
        } else {
            // New contact - reset state
            editedContact = com.metromessages.data.local.metropeoplehub.ContactEdits(displayName = "")
            viewModel.clearEditState()
            println("🆕 MetroContactEditor: Creating new contact")
        }
    }

    // Update edited contact when currentContact changes
    LaunchedEffect(currentContact) {
        if (contactId != null && currentContact != null) {
            println("🔄 MetroContactEditor: Updating edited contact from currentContact")
            // ✅ ACTUALLY USING: fromMetroContact helper
            editedContact = com.metromessages.data.local.metropeoplehub.ContactEdits.fromMetroContact(currentContact!!)
        }
    }

    // Handle save completion and errors
    LaunchedEffect(editState) {
        when (editState) {
            is MetroPeopleHubViewModel.ContactEditState.Success -> {
                println("✅ MetroContactEditor: Save successful, completing...")
                onSaveComplete()
            }
            is MetroPeopleHubViewModel.ContactEditState.Error -> {
                saveError = (editState as MetroPeopleHubViewModel.ContactEditState.Error).message
                println("❌ MetroContactEditor: Save error: $saveError")
            }
            else -> {
                // Other states
            }
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        text = if (contactId == null) "new contact" else "edit contact",
                        modifier = Modifier
                            .offset(x = 5.dp, y = 40.dp)
                            .clickable { onCancel() },
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
                    // PROFILE PHOTO SECTION
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
                                    .size(120.dp)
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

                                // Edit badge overlay
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

                            // Photo action buttons
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
                                    backgroundColor = accentColor
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

                    // ✅ ACTUALLY USING: Helper properties and methods
                    // Home Phone
                    EditField(
                        label = "home phone",
                        value = editedContact.homePhone,
                        onValueChange = { newValue ->
                            // ✅ ACTUALLY USING: updatePhone helper method
                            editedContact = editedContact.updatePhone(
                                ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
                                newValue,
                                "Home"
                            )
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cell Phone
                    EditField(
                        label = "cell phone",
                        value = editedContact.cellPhone,
                        onValueChange = { newValue ->
                            // ✅ ACTUALLY USING: updatePhone helper method
                            editedContact = editedContact.updatePhone(
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                                newValue,
                                "Mobile"
                            )
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Work Phone
                    EditField(
                        label = "work phone",
                        value = editedContact.workPhone,
                        onValueChange = { newValue ->
                            // ✅ ACTUALLY USING: updatePhone helper method
                            editedContact = editedContact.updatePhone(
                                ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
                                newValue,
                                "Work"
                            )
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Other Phone
                    EditField(
                        label = "other phone",
                        value = editedContact.otherPhone,
                        onValueChange = { newValue ->
                            // ✅ ACTUALLY USING: updatePhone helper method
                            editedContact = editedContact.updatePhone(
                                ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
                                newValue,
                                "Other"
                            )
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

                    // ✅ ACTUALLY USING: Helper properties and methods
                    // Work Email
                    EditField(
                        label = "work email",
                        value = editedContact.workEmail,
                        onValueChange = { newValue ->
                            // ✅ ACTUALLY USING: updateEmail helper method
                            editedContact = editedContact.updateEmail(
                                ContactsContract.CommonDataKinds.Email.TYPE_WORK,
                                newValue,
                                "Work"
                            )
                        },
                        metroFont = metroFont,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Private Email
                    EditField(
                        label = "private email",
                        value = editedContact.privateEmail,
                        onValueChange = { newValue ->
                            // ✅ ACTUALLY USING: updateEmail helper method
                            editedContact = editedContact.updateEmail(
                                ContactsContract.CommonDataKinds.Email.TYPE_HOME,
                                newValue,
                                "Personal"
                            )
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

                    // Save error display
                    saveError?.let { error ->
                        Text(
                            text = error,
                            style = MetroTypography.MetroBody2(metroFont).copy(color = Color.Red),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }

                    // ACTION BUTTONS - Sharp corners
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel Button
                        MetroOutlineBox(
                            title = "cancel",
                            onClick = onCancel,
                            metroFont = metroFont,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            backgroundColor = Color(0xFF2A2A2A)
                        )

                        // Save Button
                        MetroOutlineBox(
                            title = if (editState is MetroPeopleHubViewModel.ContactEditState.Saving) "saving..." else "save",
                            onClick = {
                                if (editedContact.displayName.isBlank()) {
                                    saveError = "Display name is required"
                                    return@MetroOutlineBox
                                }
                                saveError = null

                                // ✅ FIXED: Pass ContactEdits directly to ViewModel
                                if (contactId != null) {
                                    viewModel.updateContact(contactId, editedContact) // ✅ FIXED: Now passing Long instead of String
                                } else {
                                    // For new contacts, convert to ContactData for repository
                                    val contactData = editedContact.toContactData()
                                    viewModel.createContact(contactData)
                                }
                            },
                            metroFont = metroFont,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            backgroundColor = if (editState is MetroPeopleHubViewModel.ContactEditState.Saving) Color.White.copy(alpha = 0.5f) else Color.White,
                            enabled = editState !is MetroPeopleHubViewModel.ContactEditState.Saving
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Save progress overlay
            if (editState is MetroPeopleHubViewModel.ContactEditState.Saving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Saving Contact...",
                            style = MetroTypography.MetroBody1(metroFont),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (contactId == null) "Creating new contact" else "Updating Android Contacts",
                            style = MetroTypography.MetroBody2(metroFont),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Photo picker sheet
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
                accentColor = accentColor,
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
                .border(2.dp, Color.White, RoundedCornerShape(0.dp))
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
                    fontSize = 20.sp,
                    fontFamily = metroFont.fontFamily,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.25).sp
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
    backgroundColor: Color = Color(0xFF444444),
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .border(2.dp, Color.White, RoundedCornerShape(0.dp))
            .background(backgroundColor, RoundedCornerShape(0.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
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
