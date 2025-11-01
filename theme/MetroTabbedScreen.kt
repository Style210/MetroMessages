// File: MetroTabbedScreen.kt
package com.metromessages.ui.theme


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.metromessages.data.settingsscreen.SettingsPreferences
import com.metromessages.ui.components.MetroPulsingDivider
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Composable
fun MetroTabbedScreen(
    tabs: List<MetroTab>,
    screenTitle: String,
    onScreenTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val accentColor by settingsPrefs.accentColorFlow.collectAsState(
        initial = Color(0xFF0063B1) // Your default Metro accent
    )

    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val pageOffset by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    val tabTravel = 200.dp
    val tabOffsets = remember(tabs.size) {
        List(tabs.size) { index ->
            derivedStateOf {
                calculateTabPosition(currentPage + pageOffset, index, tabs.size, tabTravel.value)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        // Screen title with back navigation
        Text(
            text = screenTitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onScreenTitleClick
                ),
            style = TextStyle(
                fontSize = 22.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1).sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        )

        // Tab headers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Text(
                    text = tab.title,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = tabOffsets[index].value)
                        .alpha(if (currentPage == index) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    style = TextStyle(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )
            }

            MetroPulsingDivider(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = (-4).dp),
                initialWidth = 100.dp,
                maxTravel = 8.dp
            )
        }

        // Content pager
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            tabs[page].content()
        }
    }
}

// Helper function for tab positioning
private fun calculateTabPosition(offset: Float, tabIndex: Int, totalTabs: Int, tabTravel: Float): Dp {
    return when {
        offset < tabIndex -> ((tabIndex - offset) * tabTravel).dp
        offset > tabIndex + 1 -> ((totalTabs - offset + tabIndex) * -tabTravel).dp
        else -> ((tabIndex - offset) * tabTravel).dp
    }
}

// Data class remains the same
data class MetroTab(
    val title: String,
    val content: @Composable () -> Unit
)
