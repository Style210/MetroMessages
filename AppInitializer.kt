package com.metromessages

import com.metromessages.data.repository.UnifiedContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor(
    private val unifiedContactRepository: UnifiedContactRepository
) {
    fun initialize(scope: CoroutineScope) {
        scope.launch {
            println("🚀 AppInitializer: Starting comprehensive app initialization...")

            // ✅ Initialize unified contacts (people + conversations sync)
            unifiedContactRepository.initializeContactsOnce()

            // ✅ FUTURE: Add more initialization here as you build new features
            // unifiedContactRepository.ensureSmsDataLoaded()
            // unifiedContactRepository.ensureDialerDataLoaded()
            // unifiedContactRepository.ensureCallLogDataLoaded()

            println("✅ AppInitializer: Comprehensive initialization complete")
        }
    }
}