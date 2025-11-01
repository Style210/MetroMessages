// File: HomeScreenViewModel.kt
package com.metromessages.data.local.homescreen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for HomeScreen composable.
 *
 * Responsibilities:
 * 1. Holds the list of pages for the horizontal pager.
 * 2. Prepares for future drill-in/out and hub-specific state.
 */
class HomeScreenViewModel : ViewModel() {

    // StateFlow holding the list of page identifiers or titles
    // Currently Page 1: vertical typography menu, Pages 2-3: placeholders
    private val _pages = MutableStateFlow(listOf("Page1", "Page2", "Page3"))
    val pages: StateFlow<List<String>> = _pages

    /**
     * Placeholder for future drill-in/out navigation logic
     * e.g., animate to specific hub page, load dynamic content.
     */
    fun navigateToPage(pageIndex: Int) {
        // TODO: Implement programmatic page scrolling when needed
    }
}


