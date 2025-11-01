// File: data/local/peoplescreen/MetroAlphabetScrollIndicator.kt
package com.metromessages.data.local.peoplescreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metromessages.ui.components.MetroFont
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MetroAlphabetScrollIndicator(
    letters: List<Char>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    groupedContacts: Map<Char, List<PersonWithDetails>>,
    accentColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Drag state
    var isDragging by remember { mutableStateOf(false) }
    var currentDragLetter by remember { mutableStateOf<Char?>(null) }
    var dragStartY by remember { mutableFloatStateOf(0f) }

    // Floating preview
    var floatingLetter by remember { mutableStateOf<Char?>(null) }
    var floatingPosition by remember { mutableStateOf(DpOffset.Zero) }

    // Container measurements
    var containerHeight by remember { mutableStateOf(0.dp) }
    var letterPositions by remember { mutableStateOf(emptyMap<Char, Float>()) }

    // Bounce animation
    val bounceOffset = remember { Animatable(0f) }

    // Calculate letter positions when container size changes
    LaunchedEffect(containerHeight, letters) {
        if (containerHeight > 0.dp && letters.isNotEmpty()) {
            val heightPx = with(density) { containerHeight.toPx() }
            val letterHeight = heightPx / letters.size
            val newPositions = letters.mapIndexed { index, letter ->
                letter to (index * letterHeight + letterHeight / 2)
            }.toMap()
            letterPositions = newPositions
        }
    }

    Box(modifier = modifier) {
        // Main alphabet column
        Column(
            modifier = Modifier
                .width(36.dp)
                .onSizeChanged { size ->
                    containerHeight = with(density) { size.height.toDp() }
                }
                .background(Color.Transparent),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(4.dp))

            letters.forEach { letter ->
                val isActive = currentDragLetter == letter

                AlphabetLetter(
                    letter = letter,
                    isActive = isActive,
                    accentColor = accentColor,
                    metroFont = metroFont,
                    onTap = {
                        coroutineScope.launch {
                            jumpToLetter(letter, groupedContacts, listState)
                        }
                    },
                    onDragStart = { offset ->
                        isDragging = true
                        currentDragLetter = letter
                        floatingLetter = letter
                        dragStartY = offset.y

                        // Position floating preview to the left of the indicator
                        floatingPosition = DpOffset(
                            x = (-80).dp,
                            y = with(density) { offset.y.toDp() - 30.dp }
                        )
                    },
                    onDrag = { change, _ ->
                        val newY = change.position.y

                        // Find closest letter to current finger position
                        val targetLetter = letterPositions.entries
                            .minByOrNull { (_, pos) -> abs(pos - newY) }?.key

                        targetLetter?.let { target ->
                            if (currentDragLetter != target) {
                                currentDragLetter = target
                                floatingLetter = target

                                // iOS-like immediate scroll
                                coroutineScope.launch {
                                    jumpToLetter(target, groupedContacts, listState)
                                }

                                // Update floating preview position
                                floatingPosition = DpOffset(
                                    x = (-80).dp,
                                    y = with(density) { newY.toDp() - 30.dp }
                                )
                            }
                        }

                        // Bounce effect at boundaries
                        if (targetLetter == letters.first() && newY < letterPositions[letters.first()]!! - 50) {
                            coroutineScope.launch {
                                bounceOffset.animateTo(-15f, spring(dampingRatio = 0.7f))
                                bounceOffset.animateTo(0f)
                            }
                        } else if (targetLetter == letters.last() && newY > letterPositions[letters.last()]!! + 50) {
                            coroutineScope.launch {
                                bounceOffset.animateTo(15f, spring(dampingRatio = 0.7f))
                                bounceOffset.animateTo(0f)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        currentDragLetter = null
                        floatingLetter = null
                    }
                )
            }

            Spacer(modifier = Modifier.size(4.dp))
        }

        // Floating letter preview
        AnimatedVisibility(
            visible = floatingLetter != null,
            enter = fadeIn(animationSpec = tween(150)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(150))
        ) {
            floatingLetter?.let { letter ->
                Box(
                    modifier = Modifier
                        .offset(floatingPosition.x, floatingPosition.y)
                        .size(80.dp)
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphabetLetter(
    letter: Char,
    isActive: Boolean,
    accentColor: Color,
    metroFont: MetroFont,
    onTap: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val backgroundColor = if (isActive) accentColor.copy(alpha = 0.3f) else Color.Transparent
    val textColor = if (isActive) accentColor else Color.White.copy(alpha = 0.7f)
    val fontSize = if (isActive) 16.sp else 14.sp
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

    Text(
        text = letter.toString(),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = fontWeight,
            fontSize = fontSize
        ),
        modifier = Modifier
            .size(24.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(letter) {
                detectTapGestures(
                    onTap = { onTap() }
                )
                detectDragGestures(
                    onDragStart = { offset -> onDragStart(offset) },
                    onDrag = { change, dragAmount -> onDrag(change, dragAmount) },
                    onDragEnd = { onDragEnd() }
                )
            },
        maxLines = 1
    )
}

private suspend fun jumpToLetter(
    letter: Char,
    groupedContacts: Map<Char, List<PersonWithDetails>>,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val sortedLetters = groupedContacts.keys.sorted()
    val targetIndex = sortedLetters.indexOf(letter)

    if (targetIndex != -1) {
        var cumulativeIndex = 0
        for (i in 0 until targetIndex) {
            cumulativeIndex += (groupedContacts[sortedLetters[i]]?.size ?: 0) + 1 // +1 for section header
        }
        listState.animateScrollToItem(cumulativeIndex)
    }
}