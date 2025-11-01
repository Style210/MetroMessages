// File: ui/components/DeviceContactsImportDialog.kt
package com.metromessages.data.local.peoplescreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography

@Composable
fun DeviceContactsImportDialog(
    viewModel: PeopleScreenViewModel,
    modifier: Modifier = Modifier
) {
    val importState = viewModel.importState.value
    val shouldShowDialog = viewModel.shouldShowImportDialog.value

    if (!shouldShowDialog) return

    Dialog(onDismissRequest = { /* Don't allow dismiss during import */ }) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF888888), RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (importState) {
                is PeopleScreenViewModel.ImportState.Idle,
                is PeopleScreenViewModel.ImportState.Checking -> {
                    ImportPromptDialog(viewModel)
                }
                is PeopleScreenViewModel.ImportState.Importing -> {
                    ImportingDialog("Preparing import...")
                }
                is PeopleScreenViewModel.ImportState.Progress -> {
                    ImportingDialog("Importing... ${importState.current}/${importState.total}")
                }
                is PeopleScreenViewModel.ImportState.Completed -> {
                    ImportCompleteDialog(importState.count, viewModel)
                }
                is PeopleScreenViewModel.ImportState.Error -> {
                    ImportErrorDialog(importState.message, viewModel)
                }
            }
        }
    }
}

@Composable
private fun ImportPromptDialog(viewModel: PeopleScreenViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Import Your Contacts",
            style = MetroTypography.MetroBody1(MetroFont.Segoe),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "We found your device contacts. Would you like to import them?",
            style = MetroTypography.MetroBody2(MetroFont.Segoe),
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.skipDeviceImport() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = Modifier.border(1.dp, Color(0xFF888888), RoundedCornerShape(4.dp))
            ) {
                Text("Use Sample Data")
            }

            Button(
                onClick = { viewModel.startDeviceContactsImport() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0063B1),
                    contentColor = Color.White
                )
            ) {
                Text("Import Contacts")
            }
        }
    }
}

@Composable
private fun ImportingDialog(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = Color(0xFF0063B1))
        Text(
            text = message,
            style = MetroTypography.MetroBody2(MetroFont.Segoe),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ImportCompleteDialog(count: Int, viewModel: PeopleScreenViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "✅",
            style = MetroTypography.MetroBody1(MetroFont.Segoe),
            fontSize = 24.sp
        )

        Text(
            text = "Imported $count contacts!",
            style = MetroTypography.MetroBody1(MetroFont.Segoe),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { /* Dialog auto-closes */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0063B1),
                contentColor = Color.White
            )
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun ImportErrorDialog(message: String, viewModel: PeopleScreenViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "❌",
            style = MetroTypography.MetroBody1(MetroFont.Segoe),
            fontSize = 24.sp
        )

        Text(
            text = "Import Failed",
            style = MetroTypography.MetroBody1(MetroFont.Segoe),
            color = Color.White
        )

        Text(
            text = message,
            style = MetroTypography.MetroBody2(MetroFont.Segoe),
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { viewModel.skipDeviceImport() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0063B1),
                contentColor = Color.White
            )
        ) {
            Text("Use Sample Data")
        }
    }
}
