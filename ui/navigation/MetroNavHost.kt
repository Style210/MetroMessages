package com.metromessages.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metromessages.data.local.homescreen.HomeScreen
import com.metromessages.data.local.metromessagehub.MetroMessagesViewModel
import com.metromessages.data.local.metropeoplehub.MetroPeopleHubViewModel
import com.metromessages.data.local.metropeoplehub.ui.PeopleScreen
import com.metromessages.data.settingsscreen.MetroSettingsScreen
import com.metromessages.data.settingsscreen.SettingsViewModel
import com.metromessages.ui.screens.ConversationThreadScreen
import com.metromessages.ui.screens.SocialThreadScreen
import com.metromessages.ui.screens.TabbedConversationScreen
import com.metromessages.voicerecorder.VoiceMemoViewModel



@Composable
fun MetroNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    // ✅ METROMESSAGES ARCHITECTURE - REPLACED LEGACY VIEWMODELS
    val metroMessagesViewModel: MetroMessagesViewModel = hiltViewModel()
    val metroPeopleHubViewModel: MetroPeopleHubViewModel = hiltViewModel()
    val voiceMemoViewModel: VoiceMemoViewModel = hiltViewModel()


    // 🗑️ REMOVED: Legacy Facebook architecture
    // val facebookViewModel: FacebookViewModel = hiltViewModel()
    // val conversationViewModel: ConversationViewModel = hiltViewModel()

    val metroFont by settingsViewModel.currentFont.collectAsStateWithLifecycle()
    val accentColor by settingsViewModel.currentAccentColor.collectAsStateWithLifecycle()

    // Parallax bounce easing curve (subtle overshoot, like Windows Phone)
    val parallaxEasing = CubicBezierEasing(0.7f, 0.3f, 0.1f, 1f)

    NavHost(
        navController = navController,
        startDestination = MetroDestinations.Home.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = parallaxEasing
                ),
                initialOffsetX = { it }
            ) + fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = parallaxEasing
                ),
                targetOffsetX = { -it }
            ) + fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = parallaxEasing
                ),
                initialOffsetX = { -it }
            ) + fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = parallaxEasing
                ),
                targetOffsetX = { it }
            ) + fadeOut(animationSpec = tween(500))
        }
    ) {
        composable(route = MetroDestinations.Home.route) {
            HomeScreen(
                navController = navController,
                metroFont = metroFont
            )
        }

        composable(route = MetroDestinations.Settings.route) {
            MetroSettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = MetroDestinations.People.route) {
            PeopleScreen(
                onBackClick = { navController.popBackStack() },
                metroFont = metroFont,
                accentColor = accentColor,
                onNavigateToConversation = { conversationId, contactId, contactName, contactPhotoUrl, initialTab ->
                    println("🧭 DEBUG PeopleScreen Navigation Call:")
                    println("   - conversationId: $conversationId")
                    println("   - contactId: $contactId")
                    println("   - contactName: $contactName")
                    println("   - contactPhotoUrl: $contactPhotoUrl")
                    println("   - initialTab: $initialTab")

                    // ✅ CORRECTED: Use the contact ID provided by PeopleScreen
                    // PeopleScreen already has proper contact resolution, so we trust its contactId
                    val resolvedContactId = if (contactId != 0L) contactId else 0L

                    navController.navigate(
                        MetroDestinations.SmsConversation.createRoute(
                            conversationId = conversationId,
                            contactId = resolvedContactId,
                            contactName = contactName ?: "Unknown Contact",
                            contactPhotoUrl = contactPhotoUrl,
                            initialTab = initialTab
                        )
                    )
                }
            )
        }

        composable(route = MetroDestinations.Social.route) {
            SocialThreadScreen(
                navController = navController,
                metroFont = metroFont
            )
        }

        composable(route = MetroDestinations.Messages.route) {
            TabbedConversationScreen(
                metroMessagesViewModel = metroMessagesViewModel, // ✅ METROMESSAGES
                navController = navController,
                metroFont = metroFont,
                onNavigateToConversation = { conversationId, contactName, contactPhotoUrl ->
                    // ✅ FOSSIFY-STYLE: Let ConversationThreadScreen handle contact resolution
                    // This follows Fossify's pattern where the conversation screen
                    // extracts phone numbers from messages and resolves contacts

                    navController.navigate(
                        MetroDestinations.SmsConversation.createRoute(
                            conversationId = conversationId,
                            contactId = 0L, // Will be resolved in ConversationThreadScreen
                            contactName = contactName ?: "Unknown Contact",
                            contactPhotoUrl = contactPhotoUrl,
                            initialTab = 1
                        )
                    )
                }
            )
        }

        composable(route = MetroDestinations.SmsConversation.route) { backStackEntry ->
            val params = MetroDestinations.parseConversationParams(backStackEntry)
            val conversationId = params.conversationId
            val contactId = params.contactId
            val contactName = params.contactName
            val contactPhotoUrl = params.contactPhotoUrl
            val initialTab = params.initialTab

            println("🧭 DEBUG NavHost SmsConversation Screen:")
            println("   - conversationId: $conversationId")
            println("   - contactId: $contactId")
            println("   - contactName: $contactName")
            println("   - contactPhotoUrl: $contactPhotoUrl")
            println("   - initialTab: $initialTab")

            // ✅ METROMESSAGES: Load conversation data
            LaunchedEffect(conversationId) {
                println("🔄 DEBUG: Loading MetroMessages for conversation: $conversationId")
                conversationId.toLongOrNull()?.let { threadId ->
                    metroMessagesViewModel.loadMessages(threadId)
                }

                // ✅ FOSSIFY-STYLE: Load contact if available
                if (contactId != 0L) {
                    metroPeopleHubViewModel.loadContactForEditing(contactId)
                }
                // If contactId is 0, ConversationThreadScreen will handle display name resolution
                // using the existing pattern that extracts phone numbers from messages
            }

            ConversationThreadScreen(
                conversationId = conversationId,
                contactId = contactId.toString(), // Convert to String for compatibility
                metroMessagesViewModel = metroMessagesViewModel, // ✅ METROMESSAGES
                metroPeopleHubViewModel = metroPeopleHubViewModel, // ✅ METROMESSAGES
                voiceMemoViewModel = voiceMemoViewModel,
                onBackClick = {
                    println("⬅️ DEBUG: Back button clicked in ConversationThreadScreen")

                    // ✅ METROMESSAGES: Cleanup
                    metroMessagesViewModel.clearCurrentConversation()

                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize(),
                initialTab = initialTab
            )
        }

        composable(route = MetroDestinations.FacebookConversation.route) { backStackEntry ->
            MetroDestinations.parseConversationParams(backStackEntry)
            Box(modifier = Modifier.fillMaxSize()) {
                Text("Facebook conversations coming soon")
            }
        }
    }
}
