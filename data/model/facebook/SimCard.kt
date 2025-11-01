package com.metromessages.data.model.facebook

data class SimCard(
    val slotIndex: Int,
    val displayName: String,
    val isEnabled: Boolean,
    val phoneNumber: String? = null,
    val carrierName: String? = null,
    val subscriptionId: Int? = null
) {
    companion object {
        fun createMockSimCards(): List<SimCard> {
            return listOf(
                SimCard(
                    slotIndex = 0,
                    displayName = "SIM 1",
                    isEnabled = true,
                    phoneNumber = "+1-555-0100",
                    carrierName = "Carrier A"
                ),
                SimCard(
                    slotIndex = 1,
                    displayName = "SIM 2",
                    isEnabled = true,
                    phoneNumber = "+1-555-0101",
                    carrierName = "Carrier B"
                )
            )
        }
    }
}