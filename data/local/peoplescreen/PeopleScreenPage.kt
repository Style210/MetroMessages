// File: PeopleScreenPage.kt
package com.metromessages.data.local.peoplescreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metromessages.ui.theme.MetroParallax
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.ui.components.MetroFont // ← ADD IMPORT

@Composable
fun PeopleScreenPage(
    contactNames: List<String>,
    pageOffset: Float = 0f,
    onContactClick: (String) -> Unit = {},
    topPadding: Dp = 0.dp,
    metroFont: MetroFont // ← ADD metroFont parameter
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = topPadding, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        contactNames.forEach { contactName ->
            Text(
                text = contactName,
                style = MetroTypography.MetroSubhead(metroFont), // ← PASS metroFont
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = MetroParallax.calculateOffset(
                            rawProgress = pageOffset,
                            speedRatio = 0.6f,
                            weight = 0.5f
                        )
                    }
                    .clickable { onContactClick(contactName) }
            )
        }
    }
}

