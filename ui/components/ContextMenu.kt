// ContextMenu.kt - With parallax bounce
package com.metromessages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowsPhoneContextMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Parallax bounce state
    var parallaxOffset by remember { mutableStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    // Auto-show/hide when visibility changes
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Start parallax bounce animation when opening
            isAnimating = true
            parallaxOffset = 50f // Start off-screen
            sheetState.show()

            // Animate parallax bounce
            repeat(3) { // 3 bounces for Metro style
                parallaxOffset = when (it) {
                    0 -> -15f // First bounce up
                    1 -> -5f  // Second smaller bounce
                    else -> 0f // Settle
                }
                kotlinx.coroutines.delay(80) // Metro-style quick bounces
            }
            isAnimating = false
        } else {
            // Exit animation when closing
            isAnimating = true
            parallaxOffset = -20f // Quick slide up
            kotlinx.coroutines.delay(50)
            sheetState.hide()
            isAnimating = false
        }
    }

    // Close when sheet is dismissed by user
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden && isVisible) {
            onDismiss()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.Black,
            dragHandle = {
                // Minimal Windows Phone style drag handle with parallax
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(vertical = 8.dp)
                        .offset(y = parallaxOffset.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(32.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                            .align(Alignment.Center)
                    )
                }
            },
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .offset(y = parallaxOffset.dp), // Apply parallax to content too
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    pressedColor: Color = Color(0xFF1A1A1A)
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 160.dp, minHeight = 48.dp)
            .background(if (isPressed) pressedColor else backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.CenterStart),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// Simplified state management
class ContextMenuState {
    var isVisible by mutableStateOf(false)

    fun show() {
        this.isVisible = true
    }

    fun hide() {
        this.isVisible = false
    }
}

@Composable
fun rememberContextMenuState(): ContextMenuState {
    return remember { ContextMenuState() }
}