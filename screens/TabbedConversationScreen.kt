package com.metromessages.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.metromessages.ui.tab.FacebookTab
import com.metromessages.ui.tab.SmsTab
import com.metromessages.viewmodel.FacebookViewModel
import com.metromessages.viewmodel.SmsViewModel
import kotlinx.coroutines.launch

@Composable
fun TabbedConversationScreen(
    onConversationClick: (String, Boolean, Any?, Any?) -> Unit,
    smsViewModel: SmsViewModel = hiltViewModel(),
    facebookViewModel: FacebookViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(initialPage = 500, pageCount = { 1000 })
    val coroutineScope = rememberCoroutineScope()

    val scrollPosition by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }

    val realPage by remember { derivedStateOf { pagerState.currentPage % 2 } }

    // Max offset range to animate between
    val tabTravel = 200.dp

    // X offset math (purely positional)
    val smsX by remember {
        derivedStateOf {
            val offset = scrollPosition % 2f
            when {
                offset < 1f -> (offset * -tabTravel.value).dp       // active, leaving left
                else -> ((2 - offset) * tabTravel.value).dp         // entering from right
            }
        }
    }

    val fbX by remember {
        derivedStateOf {
            val offset = scrollPosition % 2f
            when {
                offset < 1f -> ((1 - offset) * tabTravel.value).dp  // entering from right
                else -> ((offset - 1) * -tabTravel.value).dp        // active, leaving left
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "conversations",
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 22.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-1).sp,
                color = Color.White
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = "messages",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = smsX)
                    .alpha(if (realPage == 0) 1f else 0.4f)
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
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

            Text(
                text = "facebook",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = fbX)
                    .alpha(if (realPage == 1) 1f else 0.4f)
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page % 2) {
                0 -> SmsTab(
                    smsViewModel = smsViewModel,
                    onConversationClick = { id -> onConversationClick(id, false, null, null) }
                )
                1 -> FacebookTab(
                    facebookViewModel = facebookViewModel,
                    onConversationClick = { id -> onConversationClick(id, true, null, null) }
                )
            }
        }
    }
}

