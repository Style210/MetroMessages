package com.metromessages.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.navigation.*

@Stable
class NavHistoryTracker {
    private var lastRoute: String? = null
    private val routeStack = mutableListOf<String>()

    fun add(destination: NavDestination, arguments: Bundle?) {
        val route = destination.route ?: return
        val routeKey = "$route:${arguments?.toString() ?: ""}"

        if (routeStack.lastOrNull() == routeKey) return

        if (routeStack.contains(routeKey)) {
            // Pop back to the existing route
            while (routeStack.lastOrNull() != routeKey) {
                routeStack.removeAt(routeStack.lastIndex)
            }
        } else {
            routeStack.add(routeKey)
        }

        lastRoute = routeKey
    }

    fun isForwardNavigation(destination: NavDestination, arguments: Bundle?): Boolean {
        val route = destination.route ?: return true
        val routeKey = "$route:${arguments?.toString() ?: ""}"

        val currentIndex = routeStack.indexOf(routeKey)
        val lastIndex = routeStack.indexOfLast { it == lastRoute }

        return currentIndex >= lastIndex
    }

    fun reset() {
        routeStack.clear()
        lastRoute = null
    }
}

@Composable
fun rememberNavHistory(navController: NavHostController): NavHistoryTracker {
    val history = remember { NavHistoryTracker() }

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            history.add(destination, arguments)
        }
    }

    return history
}

