// File: ui/components/PeopleHubLiveTile.kt
package com.metromessages.data.local.peoplescreen

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlin.random.Random

// ✅ SIMPLE, RELIABLE TIMING
private object LiveTileTiming {
    const val ANIMATION_INTERVAL_MIN = 20000L    // 20 seconds
    const val ANIMATION_INTERVAL_MAX = 180000L   // 3 minutes
    const val COOLDOWN_PERIOD = 30000L           // 30 seconds cooldown
    const val ANIMATION_DURATION = 2200          // 2.2 seconds - smooth and reliable
}

@Composable
fun PeopleHubLiveTile(
    contact: PersonWithDetails,
    viewModel: PeopleScreenViewModel = hiltViewModel(),
    accentColor: Color,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    // ✅ GET UNIFIED CONTACT DATA
    val unifiedContacts by viewModel.unifiedContacts.collectAsState()
    val unifiedContact = unifiedContacts.find { it.id == contact.person.id }
    val contactPhotoUri = unifiedContact?.photoUri ?: contact.person.photoUri
    val displayName = unifiedContact?.displayName ?: contact.person.displayName

    // ✅ SIMPLE STATE MANAGEMENT - Like your original build
    var isAnimating by remember { mutableStateOf(false) }
    var showNamePanel by remember { mutableStateOf(false) }
    var lastAnimationTime by remember { mutableLongStateOf(0L) }

    // ✅ SMOOTH, SIMPLE ANIMATIONS - No complex keyframes
    val panelHeight = 80.dp // Push photo down 50% of 160dp tile

    val photoOffset by animateDpAsState(
        targetValue = if (isAnimating) panelHeight else 0.dp,
        animationSpec = tween(
            durationMillis = LiveTileTiming.ANIMATION_DURATION,
            easing = FastOutSlowInEasing // Smooth and reliable
        ),
        label = "photoPush"
    )

    val panelReveal by animateDpAsState(
        targetValue = if (isAnimating) panelHeight else 0.dp,
        animationSpec = tween(
            durationMillis = LiveTileTiming.ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        ),
        label = "panelReveal"
    )

    val nameAlpha by animateFloatAsState(
        targetValue = if (isAnimating && showNamePanel) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 400, // Appears shortly after animation starts
            easing = FastOutSlowInEasing
        ),
        label = "nameFade"
    )

    // ✅ SIMPLE, RELIABLE ANIMATION ORCHESTRATION
    LaunchedEffect(displayName) {
        while (true) {
            val now = System.currentTimeMillis()
            val timeSinceLastAnimation = now - lastAnimationTime

            if (timeSinceLastAnimation >= LiveTileTiming.COOLDOWN_PERIOD) {
                val delayTime = Random.nextLong(
                    LiveTileTiming.ANIMATION_INTERVAL_MIN,
                    LiveTileTiming.ANIMATION_INTERVAL_MAX
                )
                delay(delayTime)

                // Start animation sequence
                isAnimating = true
                lastAnimationTime = System.currentTimeMillis()

                // Show name panel after short delay
                delay(400)
                showNamePanel = true

                // Hold for the remaining animation time minus fade out time
                delay(LiveTileTiming.ANIMATION_DURATION - 1000L)

                // Hide and reset
                showNamePanel = false
                delay(600) // Wait for fade out
                isAnimating = false

                // Cooldown
                delay(LiveTileTiming.COOLDOWN_PERIOD)
            } else {
                val remainingCooldown = LiveTileTiming.COOLDOWN_PERIOD - timeSinceLastAnimation
                delay(remainingCooldown + 1000)
            }
        }
    }

    // ✅ CLEAN, SMOOTH LAYOUT
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(0.dp),
                clip = false
            )
            .clip(RoundedCornerShape(0.dp))
            .clickable(onClick = onClick)
    ) {
        // 1. NAME PANEL - Smooth reveal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelReveal)
                .align(Alignment.TopStart)
                .background(accentColor)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .align(Alignment.Center)
                    .alpha(nameAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // 2. CONTACT PHOTO - Smooth push down
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { androidx.compose.ui.unit.IntOffset(0, photoOffset.roundToPx()) }
        ) {
            SubcomposeAsyncImage(
                model = contactPhotoUri,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    FallbackContent(displayName, accentColor)
                },
                error = {
                    FallbackContent(displayName, accentColor)
                }
            )
        }
    }
}

@Composable
private fun FallbackContent(displayName: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(accentColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.take(2).uppercase(),
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun PeopleHubLiveTilesGrid(
    favoriteContacts: List<PersonWithDetails>,
    viewModel: PeopleScreenViewModel = hiltViewModel(),
    accentColor: Color,
    onContactClick: (Long) -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    // ✅ FIXED: Transparent background for gradient
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // Fixed 2 columns
        modifier = modifier.background(Color.Transparent), // ✅ CHANGED: Transparent
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favoriteContacts, key = { it.person.id }) { contact ->
            PeopleHubLiveTile(
                contact = contact,
                viewModel = viewModel,
                accentColor = accentColor,
                onClick = { onContactClick(contact.person.id) },
                modifier = Modifier.aspectRatio(1f) // Ensures perfect squares
            )
        }
    }
}
