package com.metromessages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor() {
    // ✅ REMOVED: All repository dependencies - no data loading at startup!

    fun initialize(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {  // ✅ Run in background thread
            println("🚀 AppInitializer: Starting lightweight initialization...")

            // ✅ ONLY perform CRITICAL setup that MUST happen at app start
            // Examples: Register broadcast receivers, setup notifications, etc.
            // ❌ NO data loading (contacts, messages, conversations)

            try {
                // Example of critical startup tasks (add your actual critical tasks here):
                // 1. Setup notification channels
                // 2. Register SMS/MMS receivers
                // 3. Initialize essential system services
                // 4. Check for pending intents

                println("📱 AppInitializer: Performing critical system setup...")

                // Simulate minimal work (replace with your actual critical tasks)
                kotlinx.coroutines.delay(100) // Small delay for demonstration

                println("✅ AppInitializer: Critical setup complete")

            } catch (e: Exception) {
                println("⚠️ AppInitializer: Non-critical error during setup: ${e.message}")
                // Don't crash the app - these are non-critical initializations
            }

            println("✅ AppInitializer: Lightweight initialization complete")
        }
    }

    // 🗑️ REMOVED: initializeMetroMessages() - Messages should load when Messages screen opens
    // 🗑️ REMOVED: initializeMetroContacts() - Contacts should load when People screen opens
}