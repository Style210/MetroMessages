// File: ui/screens/SocialThreadScreen.kt
package com.metromessages.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.metromessages.data.settingsscreen.SettingsPreferences
import com.metromessages.ui.components.CollapsibleHeaderScreen
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.components.MetroPulsingDivider
import com.metromessages.ui.theme.MetroHeaderCanvas
import com.metromessages.ui.theme.MetroTypography
import kotlinx.coroutines.launch

@SuppressLint("UseOfNonLambdaOffsetOverload")
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun SocialThreadScreen(
    navController: NavHostController,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ✅ ADDED: Get accent color from settings
    remember { SettingsPreferences(context) }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    // INDEPENDENT SCROLL STATES FOR METRO HEADER
    val headerScrollState = rememberScrollState()

    // ✅ ADDED: Sync header with pager scroll (35% parallax effect)
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        val totalScroll = (pagerState.currentPage + pagerState.currentPageOffsetFraction) * 1000f
        val headerTarget = (totalScroll * 0.35f).toInt()
        headerScrollState.scrollTo(headerTarget)
    }

    // ✅ ADDED: Proper state derivation like other screens
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val pageOffset by remember { derivedStateOf { pagerState.currentPageOffsetFraction } }

    val tabTravel = 200.dp

    // ✅ ADDED: Derived state for tab positions like other screens
    val facebookX by remember {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 0, 3, tabTravel.value)
        }
    }

    val whatsappX by remember {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 1, 3, tabTravel.value)
        }
    }

    val telegramX by remember {
        derivedStateOf {
            calculateTabPosition(currentPage + pageOffset, 2, 3, tabTravel.value)
        }
    }

    // Use the NEW CollapsibleHeaderScreen with bottom sheet behavior
    CollapsibleHeaderScreen(
        // Header content - EXACTLY matches your original header
        headerContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        navController.popBackStack()
                    }
            ) {
                MetroHeaderCanvas(
                    text = "social",
                    scrollState = headerScrollState,
                    metroFont = metroFont,
                    modifier = Modifier.fillMaxSize(),
                    textColor = Color.White,
                    opacity = 1f,
                )
            }
        },
        // Tab content - EXACTLY matches your original tab section, now acts as drag handle!
        tabContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                // Facebook tab
                Text(
                    text = "facebook",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = facebookX.dp)
                        .alpha(if (currentPage == 0) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                    style = MetroTypography.MetroSubhead(metroFont).copy(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )

                // WhatsApp tab
                Text(
                    text = "whatsapp",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = whatsappX.dp)
                        .alpha(if (currentPage == 1) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                    style = MetroTypography.MetroSubhead(metroFont).copy(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )

                // Telegram tab
                Text(
                    text = "telegram",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = telegramX.dp)
                        .alpha(if (currentPage == 2) 1f else 0.6f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                    style = MetroTypography.MetroSubhead(metroFont).copy(
                        fontSize = 44.sp,
                        lineHeight = 53.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White
                    )
                )

                MetroPulsingDivider(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = (-4).dp),
                    initialWidth = 100.dp,
                    maxTravel = 8.dp
                )
            }
        },
        // Main content - EXACTLY matches your original pager content
        mainContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (page) {
                                0 -> "Facebook conversations coming soon"
                                1 -> "WhatsApp conversations coming soon"
                                2 -> "Telegram conversations coming soon"
                                else -> "Coming soon"
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            style = MetroTypography.MetroBody2(metroFont)
                        )
                    }
                }
            }
        },
        // Options content - Now appears in the smooth bottom sheet when user swipes down on tabs
        optionsContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Social Actions",
                    style = MetroTypography.MetroBody2(metroFont).copy(
                        color = Color.White,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "• Refresh  • Mark all read  • Settings",
                    style = MetroTypography.MetroBody2(metroFont).copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        modifier = modifier
    )
}

// Helper function for tab positioning
private fun calculateTabPosition(offset: Float, tabIndex: Int, totalTabs: Int, tabTravel: Float): Float =
    when {
        offset < tabIndex -> ((tabIndex - offset) * tabTravel)
        offset > tabIndex + 1 -> ((totalTabs - offset + tabIndex) * -tabTravel)
        else -> ((tabIndex - offset) * tabTravel)
    }
