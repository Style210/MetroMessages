// File: ui/components/CollapsibleHeaderScreen.kt
package com.metromessages.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleHeaderScreen(
    headerContent: @Composable (() -> Unit)? = null,
    tabContent: @Composable (() -> Unit)? = null,
    mainContent: @Composable () -> Unit,
    optionsContent: @Composable (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    rememberCoroutineScope()

    // Bottom sheet state for the options panel
    val bottomSheetState = rememberModalBottomSheetState()
    var showOptions by remember { mutableStateOf(false) }

    // Auto-show/hide when state changes
    LaunchedEffect(showOptions) {
        if (showOptions) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    // Close options when sheet is dismissed by user
    LaunchedEffect(bottomSheetState.currentValue) {
        if (bottomSheetState.currentValue == SheetValue.Hidden) {
            showOptions = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // HEADER - Always visible
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            headerContent?.invoke()
        }

        // SUBHEADER/TAB CONTENT - Now acts as the drag handle for options
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Swipe DOWN to reveal options (like notification shade)
                        if (dragAmount > 20f && !showOptions) {
                            showOptions = true
                        }
                    }
                }
        ) {
            tabContent?.invoke()
        }

        // MAIN CONTENT AREA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            mainContent()
        }
    }

    // OPTIONS PANEL - Slides up like notification shade
    if (showOptions && optionsContent != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            sheetState = bottomSheetState,
            containerColor = Color.Black.copy(alpha = 0.95f), // MetroUI style
            dragHandle = {
                // Minimal drag handle that matches Metro aesthetic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(40.dp)
                            .background(Transparent))

                }
            }
        ) {
            // Your options content with Metro styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                optionsContent()
            }
        }
    }
}
