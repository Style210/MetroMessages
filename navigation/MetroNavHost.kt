package com.metromessages.ui.navigation

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metromessages.data.local.homescreen.HomeScreen
import com.metromessages.data.local.peoplescreen.PeopleScreen
import com.metromessages.data.local.peoplescreen.PeopleScreenViewModel
import com.metromessages.data.model.facebook.ConversationThreadScreen
import com.metromessages.data.settingsscreen.MetroSettingsScreen
import com.metromessages.data.settingsscreen.SettingsViewModel
import com.metromessages.ui.screens.SocialThreadScreen
import com.metromessages.ui.screens.TabbedConversationScreen
import com.metromessages.viewmodel.ConversationViewModel
import com.metromessages.viewmodel.FacebookViewModel
import com.metromessages.voicerecorder.VoiceMemoViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MetroNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val peopleViewModel: PeopleScreenViewModel = hiltViewModel()
    val facebookViewModel: FacebookViewModel = hiltViewModel()
    val voiceMemoViewModel: VoiceMemoViewModel = hiltViewModel()
    val conversationViewModel: ConversationViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val metroFont by settingsViewModel.currentFont.collectAsStateWithLifecycle()
    val accentColor by settingsViewModel.currentAccentColor.collectAsStateWithLifecycle()

    // Parallax bounce easing curve (subtle overshoot, like Windows Phone)
    val parallaxEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

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
                    navController.navigate(
                        MetroDestinations.SmsConversation.createRoute(
                            conversationId = conversationId,
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
                facebookViewModel = facebookViewModel,
                navController = navController,
                metroFont = metroFont,
                onNavigateToConversation = { conversationId, contactName, contactPhotoUrl ->
                    navController.navigate(
                        MetroDestinations.SmsConversation.createRoute(
                            conversationId = conversationId,
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
            val contactName = params.contactName
            val contactPhotoUrl = params.contactPhotoUrl
            val initialTab = params.initialTab

            var contactId by remember { mutableStateOf(0L) }

            LaunchedEffect(conversationId) {
                conversationViewModel.refreshUnifiedData()
                contactId = facebookViewModel.resolveContactIdFromConversation(conversationId)
                facebookViewModel.currentConversationId.value = conversationId
            }

            ConversationThreadScreen(
                conversationId = conversationId,
                contactId = contactId,
                conversationViewModel = conversationViewModel,
                facebookViewModel = facebookViewModel,
                voiceMemoViewModel = voiceMemoViewModel,
                onSendText = { text ->
                    facebookViewModel.sendMessage(conversationId, text)
                    coroutineScope.launch {
                        conversationViewModel.refreshUnifiedData()
                        facebookViewModel.refreshAllConversations()
                    }
                },
                onSendAudio = { audioPath ->
                    facebookViewModel.sendAudio(conversationId, audioPath)
                    coroutineScope.launch {
                        conversationViewModel.refreshUnifiedData()
                        facebookViewModel.refreshAllConversations()
                    }
                },
                onSendMedia = { mediaUris: List<Uri> ->
                    coroutineScope.launch {
                        facebookViewModel.addMediaAttachments(mediaUris)
                    }
                },
                onBackClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
                initialTab = initialTab
            )
        }

        composable(route = MetroDestinations.FacebookConversation.route) { backStackEntry ->
            val params = MetroDestinations.parseConversationParams(backStackEntry)
            Box(modifier = Modifier.fillMaxSize()) {
                Text("Facebook conversations coming soon")
            }
        }
    }
}