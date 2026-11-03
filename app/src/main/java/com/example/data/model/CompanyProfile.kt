package com.example.data.model

data class CompanyProfile(
    val companyName: String = "Zayed SalesForce",
    val ownerName: String = "Al Yasir Bin Zayed",
    val tagline: String = "Enterprise Route-to-Market & Regional Order Booking",
    val companyPhone: String = "+971 4 200 8888 / +254 700 888 999",
    val companyEmail: String = "dispatch@zayed-salesforce.com",
    val factoryLocation: String = "Zayed Logistics Hub, Mombasa Road Industrial Zone, Nairobi",
    val taxPin: String = "ZYD0984512X",
    val stampSealText: String = "ZAYED SALESFORCE • AL YASIR BIN ZAYED • VERIFIED & BOOKED",
    val salesRepName: String = "Tariq Al Mansoor",
    val salesRepContact: String = "+254 711 888 777",
    val salesRepEmail: String = "t.almansoor@zayed-salesforce.com",
    val adminPin: String = "22026",
    val secondaryStaffPin: String = "2026",
    val requirePinForSensitiveStaffActions: Boolean = true,
    val supabaseUrl: String = "https://kocvwcwmzaosgofydjha.supabase.co",
    val supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtvY3Z3Y3dtemFvc2dvZnlkamhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTMwODY5MjQsImV4cCI6MjEwODY2MjkyNH0.hgeecPbnr0fo7W-RiaHWz8zEy84OTN-GKnE_B7b1O1U",
    val isLowBandwidthMode: Boolean = true,
    val isDarkMode: Boolean = true,
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    // Enterprise Configuration Settings
    val defaultTaxCategory: String = "VAT (16%)",
    val defaultDeliveryUrgency: String = "Standard Dispatch",
    val autoCalculateFreight: Boolean = true,
    val tier1FreightRate: Double = 50.0,
    val tier2FreightRate: Double = 120.0,
    val tier3FreightRate: Double = 250.0,
    val defaultLowStockThreshold: Int = 15,
    val defaultCriticalStockThreshold: Int = 5,
    val allowInfiniteStock: Boolean = true,
    val allowBackorders: Boolean = true,
    val autoSyncIntervalMinutes: Int = 15,
    val syncOnlyOnWifi: Boolean = false,
    val pauseSyncOnLowBattery: Boolean = true,
    val gzipCompressionEnabled: Boolean = true,
    val sessionTimeoutMinutes: Int = 30,
    // Multi-tenant White-Label & Cloud Governance
    val tenantId: String = "tenant_zayed_enterprise",
    val isSupabaseConfigLocked: Boolean = false,
    val isWhiteLabeled: Boolean = false,
    val lastVerifiedSyncTimestamp: Long = 0L,
    val frameworkDevUrl: String = "https://ais-dev-bjuyqcsp6rf3ec5w44yq4y-973870353017.europe-west2.run.app"
)

data class RegionalPricingConfig(
    val distanceKm: Int = 15,
    val distanceBand: String = "0-25 km (Local Nairobi)",
    val regionalTier: String = "Tier 1 - Nairobi Urban",
    val deliveryUrgency: String = "Standard Dispatch",
    val taxCategory: String = "VAT (16%)",
    val tier1Rate: Double = 50.0,
    val tier2Rate: Double = 120.0,
    val tier3Rate: Double = 250.0
) {
    fun calculateFreightSurcharge(totalUnits: Int): Double {
        val baseTierRate = when {
            regionalTier.contains("Tier 1") -> tier1Rate
            regionalTier.contains("Tier 2") -> tier2Rate
            else -> tier3Rate // Tier 3
        }
        val distanceMultiplier = when {
            distanceKm <= 25 -> 1.0
            distanceKm <= 50 -> 1.5
            distanceKm <= 100 -> 2.2
            else -> 3.5
        }
        val urgencyFactor = when (deliveryUrgency) {
            "Next-Day Priority" -> 1.3
            "Weekend Express" -> 1.5
            else -> 1.0
        }
        val unitScaling = (totalUnits / 10).coerceAtLeast(1)
        return (baseTierRate * distanceMultiplier * urgencyFactor) + (unitScaling * 15.0)
    }

    fun getTaxRate(): Double {
        return when (taxCategory) {
            "VAT (16%)" -> 0.16
            "VAT (8%)" -> 0.08
            else -> 0.0
        }
    }
}
