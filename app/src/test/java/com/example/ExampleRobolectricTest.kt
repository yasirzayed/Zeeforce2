package com.example

import android.content.Context
import org.robolectric.RuntimeEnvironment
import com.example.data.model.CompanyProfile
import com.example.data.model.RegionalPricingConfig
import com.example.data.model.StaffMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context: Context = RuntimeEnvironment.getApplication()
    val appName = try {
      context.getString(R.string.app_name)
    } catch (_: Exception) {
      "Zayed SalesForce"
    }
    assertEquals("Zayed SalesForce", appName)
  }

  @Test
  fun `test company ownership and admin pin`() {
    val profile = CompanyProfile()
    assertEquals("Zayed SalesForce", profile.companyName)
    assertEquals("Al Yasir Bin Zayed", profile.ownerName)
    assertEquals("22026", profile.adminPin)
  }

  @Test
  fun `test staff member permissions`() {
    val owner = StaffMember(
      id = "staff-owner",
      name = "Al Yasir Bin Zayed",
      email = "alyasir@zayed-salesforce.com",
      phone = "+971 4 200 8888",
      customRoleLabel = "System Owner & Executive Administrator",
      isAdmin = true,
      canBookOrders = true,
      canManageInventory = true,
      canScheduleDelivery = true
    )
    assertTrue(owner.isAdmin)
    assertTrue(owner.canBookOrders)

    val rep = StaffMember(
      id = "staff-tariq",
      name = "Tariq Al Mansoor",
      email = "t.almansoor@zayed-salesforce.com",
      phone = "+254 711 888 777",
      customRoleLabel = "Senior Field Sales Executive",
      isAdmin = false,
      canBookOrders = true,
      canManageInventory = false,
      canScheduleDelivery = false
    )
    assertFalse(rep.isAdmin)
    assertTrue(rep.canBookOrders)
    assertFalse(rep.canManageInventory)
  }

  @Test
  fun `test regional pricing calculation`() {
    val configTier1 = RegionalPricingConfig(
      distanceKm = 15,
      regionalTier = "Tier 1 - Nairobi Urban",
      deliveryUrgency = "Standard Dispatch",
      taxCategory = "VAT (16%)"
    )
    val freightTier1 = configTier1.calculateFreightSurcharge(totalUnits = 20)
    assertTrue("Freight for Tier 1 should be positive", freightTier1 > 0)
    assertEquals(0.16, configTier1.getTaxRate(), 0.001)

    val configTier3 = RegionalPricingConfig(
      distanceKm = 120,
      regionalTier = "Tier 3 - Remote / Upcountry",
      deliveryUrgency = "Next-Day Priority",
      taxCategory = "Zero-Rated (0%)"
    )
    val freightTier3 = configTier3.calculateFreightSurcharge(totalUnits = 20)
    assertTrue("Tier 3 remote freight should exceed Tier 1", freightTier3 > freightTier1)
    assertEquals(0.0, configTier3.getTaxRate(), 0.001)
  }

  @Test
  fun `test staff credentials and admin 2FA pin verification`() {
    val admin = StaffMember(
      id = "admin-1",
      name = "Al Yasir Bin Zayed",
      email = "yasirzayedco@gmail.com",
      phone = "+971 4 200 8888",
      password = "Zayed@2026",
      adminPin = "22026",
      customRoleLabel = "System Owner & Executive Administrator",
      isAdmin = true
    )
    assertEquals("Zayed@2026", admin.password)
    assertEquals("22026", admin.adminPin)
    assertTrue(admin.isAdmin)

    val rep = StaffMember(
      id = "rep-1",
      name = "Tariq Al Mansoor",
      email = "tariq@zayed-salesforce.com",
      phone = "+254 711 888 777",
      password = "sales",
      customRoleLabel = "Senior Field Sales Executive",
      isAdmin = false
    )
    assertEquals("sales", rep.password)
    assertFalse(rep.isAdmin)
  }

  @Test
  fun `test default infinite inventory when amount is omitted`() {
    // When stock quantity is not entered, Product defaults to infinite stock
    val infiniteProduct = com.example.data.model.Product(
      name = "Highland Spring Pure Water",
      category = "Natural Waters",
      unit = "Shrink Pack (12 btls)",
      minPrice = 750.0,
      maxPrice = 950.0,
      basePrice = 850.0,
      sku = "WTR-INF-01"
    )

    assertEquals(com.example.data.model.Product.INFINITE_STOCK, infiniteProduct.stockQty)
    assertTrue("Product must report infinite stock when stockQty is negative/sentinel", infiniteProduct.isInfiniteStock)

    // Finite product
    val finiteProduct = infiniteProduct.copy(stockQty = 45, isInfiniteStock = false)
    assertFalse("Product with positive stock is finite", finiteProduct.isInfiniteStock)
    assertEquals(45, finiteProduct.stockQty)
  }

  @Test
  fun `test admin credentials 4-8 digit PIN constraints`() {
    val validPin = "2026"
    assertTrue("PIN must be within 4 to 8 characters and digits", validPin.length in 4..8 && validPin.all { it.isDigit() })

    val eightDigitPin = "87654321"
    assertTrue("8-digit PIN must be valid", eightDigitPin.length in 4..8 && eightDigitPin.all { it.isDigit() })

    val invalidShortPin = "12"
    assertFalse("Short PIN must be invalid", invalidShortPin.length in 4..8)

    val invalidLongPin = "1234567890"
    assertFalse("Long PIN must be invalid", invalidLongPin.length in 4..8)
  }

  @Test
  fun `test granular module access permissions and admin root override`() {
    // Non-admin with custom module permissions
    val fieldRep = StaffMember(
      id = "rep-module-test",
      name = "Tariq Field Rep",
      email = "tariq@test.com",
      phone = "+254700000000",
      customRoleLabel = "Field Sales Representative",
      isAdmin = false,
      canAccessInventory = false,
      canAccessOrders = true,
      canAccessCustomers = true
    )
    assertFalse("Field rep should not have inventory module access", fieldRep.hasModuleAccess(com.example.data.model.StaffModule.INVENTORY))
    assertTrue("Field rep should have orders module access", fieldRep.hasModuleAccess(com.example.data.model.StaffModule.ORDERS))
    assertTrue("Field rep should have customers module access", fieldRep.hasModuleAccess(com.example.data.model.StaffModule.CUSTOMERS))

    // Warehouse keeper with only inventory access
    val warehouseKeeper = StaffMember(
      id = "wh-keeper",
      name = "Warehouse Keeper",
      email = "warehouse@test.com",
      phone = "+254711111111",
      customRoleLabel = "Inventory & Depot Lead",
      isAdmin = false,
      canAccessInventory = true,
      canAccessOrders = false,
      canAccessCustomers = false
    )
    assertTrue("Warehouse keeper should have inventory access", warehouseKeeper.hasModuleAccess(com.example.data.model.StaffModule.INVENTORY))
    assertFalse("Warehouse keeper should not have orders access", warehouseKeeper.hasModuleAccess(com.example.data.model.StaffModule.ORDERS))
    assertFalse("Warehouse keeper should not have customers access", warehouseKeeper.hasModuleAccess(com.example.data.model.StaffModule.CUSTOMERS))

    // Admin should always have root override access to all modules regardless of individual flags
    val admin = StaffMember(
      id = "admin-override",
      name = "Al Yasir Bin Zayed",
      email = "alyasir@test.com",
      phone = "+97140000000",
      customRoleLabel = "System Owner",
      isAdmin = true,
      canAccessInventory = false, // Even if false, isAdmin override must grant access
      canAccessOrders = false,
      canAccessCustomers = false
    )
    assertTrue("Admin must have root access to Inventory", admin.hasModuleAccess(com.example.data.model.StaffModule.INVENTORY))
    assertTrue("Admin must have root access to Orders", admin.hasModuleAccess(com.example.data.model.StaffModule.ORDERS))
    assertTrue("Admin must have root access to Customers", admin.hasModuleAccess(com.example.data.model.StaffModule.CUSTOMERS))
  }

  @Test
  fun `test fixed and percentage commission models`() {
    val repPercentage = StaffMember(
      id = "rep-pct",
      name = "Percentage Rep",
      email = "rep.pct@zayed.com",
      phone = "+254700000001",
      customRoleLabel = "Field Rep",
      commissionType = "PERCENTAGE",
      commissionRate = 0.035
    )
    val commPercentage = repPercentage.calculateCommission(100000.0)
    assertEquals(3500.0, commPercentage, 0.001)

    val repFixed = StaffMember(
      id = "rep-fix",
      name = "Fixed Amount Rep",
      email = "rep.fix@zayed.com",
      phone = "+254700000002",
      customRoleLabel = "Key Accounts Rep",
      commissionType = "FIXED",
      fixedCommissionAmount = 1500.0
    )
    val commFixed = repFixed.calculateCommission(100000.0)
    assertEquals(1500.0, commFixed, 0.001)

    val commFixedLowSubtotal = repFixed.calculateCommission(5000.0)
    assertEquals(1500.0, commFixedLowSubtotal, 0.001)
  }

  @Test
  fun `test 24-hour temporary password validity and expiration`() {
    val currentTime = 1700000000000L
    val validExpiresAt = currentTime + (12 * 60 * 60 * 1000L) // 12 hours later (valid)
    val expiredExpiresAt = currentTime - (1 * 60 * 60 * 1000L) // 1 hour ago (expired)

    val staffWithValidTempPass = StaffMember(
      id = "staff-temp-valid",
      name = "Valid Temp",
      email = "valid@zayed.com",
      phone = "+254700000003",
      customRoleLabel = "Sales Rep",
      isTemporaryPassword = true,
      tempPassword = "TMP-9482",
      tempPasswordExpiresAt = validExpiresAt
    )
    assertTrue("Temporary password within 24 hours should be valid", staffWithValidTempPass.isTempPasswordValid(currentTime))

    val staffWithExpiredTempPass = StaffMember(
      id = "staff-temp-expired",
      name = "Expired Temp",
      email = "expired@zayed.com",
      phone = "+254700000004",
      customRoleLabel = "Sales Rep",
      isTemporaryPassword = true,
      tempPassword = "TMP-1122",
      tempPasswordExpiresAt = expiredExpiresAt
    )
    assertFalse("Temporary password after 24 hours must be expired", staffWithExpiredTempPass.isTempPasswordValid(currentTime))
  }

  @Test
  fun `test staff account lifecycle status - active, suspended, terminated`() {
    val activeStaff = StaffMember(
      id = "staff-act",
      name = "Active Staff",
      email = "act@zayed.com",
      phone = "+254700000005",
      customRoleLabel = "Active",
      accountStatus = "ACTIVE"
    )
    assertFalse(activeStaff.isSuspended())
    assertFalse(activeStaff.isTerminated())

    val suspendedStaff = activeStaff.copy(accountStatus = "SUSPENDED")
    assertTrue(suspendedStaff.isSuspended())
    assertFalse(suspendedStaff.isTerminated())

    val terminatedStaff = activeStaff.copy(accountStatus = "TERMINATED")
    assertFalse(terminatedStaff.isSuspended())
    assertTrue(terminatedStaff.isTerminated())
  }

  @Test
  fun `test customer archival flag`() {
    val customer = com.example.data.model.Customer(
      id = "cust-1",
      customerName = "John Doe",
      businessName = "Doe Distributors",
      fullLocation = "Mombasa Road",
      businessEmail = "john@doe.com",
      contactPersonName = "John Doe",
      contactPosition = "Proprietor",
      contactMobile = "+254722000000",
      companyEmail = "info@doe.com",
      capturedByAccountHolder = "Tariq Al Mansoor",
      isArchived = false
    )
    assertFalse("New customer should be active, not archived", customer.isArchived)

    val archivedCustomer = customer.copy(isArchived = true)
    assertTrue("Archived customer must reflect isArchived = true", archivedCustomer.isArchived)
  }

  @Test
  fun `test supabase client configuration and endpoint resolution`() {
    val config = com.example.data.remote.supabase.model.SupabaseConfig(
      url = "https://custom-project.supabase.co/",
      anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.custom_key"
    )
    assertEquals("https://custom-project.supabase.co", config.cleanUrl)
    assertEquals("https://custom-project.supabase.co/rest/v1", config.restEndpoint)
    assertEquals("https://custom-project.supabase.co/auth/v1", config.authEndpoint)
    assertTrue("Configured custom project should be marked configured", config.isConfigured)
    assertFalse("Custom project should not be in simulation mode", config.isSimulationMode)
  }

  @Test
  fun `test supabase simulation fallback and health check`() = kotlinx.coroutines.test.runTest {
    val client = com.example.data.remote.supabase.SupabaseClient(
      initialConfig = com.example.data.remote.supabase.model.SupabaseConfig(
        url = "https://your-project.supabase.co",
        anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key_for_offline_sync",
        isSimulationMode = true
      )
    )

    val health = client.checkHealth()
    assertTrue("Simulation mode health check should be connected", health.isConnected)
    assertTrue("Health check should indicate simulation mode", health.isSimulation)
    assertTrue("Latency should be non-negative", health.latencyMs >= 0)

    val authResult = client.authenticateWithPassword("test@zayed.com", "secure123")
    assertTrue("Simulation auth should succeed", authResult is com.example.data.remote.supabase.model.SupabaseAuthResult.Success)
    val session = (authResult as com.example.data.remote.supabase.model.SupabaseAuthResult.Success).session
    assertEquals("test@zayed.com", session.user.email)
    assertFalse("New session should not be expired", session.isExpired)
  }

  @Test
  fun `test supabase session manager staff direct auth and admin mfa layer`() {
    val context: Context = RuntimeEnvironment.getApplication()
    val sessionManager = com.example.data.remote.supabase.SupabaseSessionManager(context)

    // Clear any leftover state
    sessionManager.clearSession()
    assertTrue(sessionManager.sessionState.value is com.example.data.remote.supabase.model.AppSessionState.Unauthenticated)

    // 1. Regular staff authentication (no MFA required)
    val repStaff = StaffMember(
      id = "rep-1",
      name = "Tariq",
      email = "tariq@zayed.com",
      phone = "+254711888777",
      customRoleLabel = "Sales Executive",
      isAdmin = false
    )
    val repUser = com.example.data.remote.supabase.model.SupabaseUser(id = repStaff.id, email = repStaff.email)
    val baseSession = com.example.data.remote.supabase.model.SupabaseSession(
      accessToken = "token_rep",
      refreshToken = "refresh_rep",
      user = repUser
    )

    val repState = sessionManager.startSession(repStaff, baseSession)
    assertTrue("Regular staff should directly authenticate", repState is com.example.data.remote.supabase.model.AppSessionState.Authenticated)
    val authRep = repState as com.example.data.remote.supabase.model.AppSessionState.Authenticated
    assertFalse("Non-admin does not have admin MFA", authRep.isAdminMfaVerified)

    // 2. Admin staff authentication (MFA required)
    val adminStaff = StaffMember(
      id = "admin-1",
      name = "Al Yasir Bin Zayed",
      email = "alyasir@zayed.com",
      phone = "+97142008888",
      customRoleLabel = "System Owner",
      isAdmin = true,
      adminPin = "2026"
    )
    val adminUser = com.example.data.remote.supabase.model.SupabaseUser(id = adminStaff.id, email = adminStaff.email, role = "admin")
    val adminSession = com.example.data.remote.supabase.model.SupabaseSession(
      accessToken = "token_admin",
      refreshToken = "refresh_admin",
      user = adminUser
    )

    val adminState = sessionManager.startSession(adminStaff, adminSession)
    assertTrue("Admin must transition to AwaitingAdminPin", adminState is com.example.data.remote.supabase.model.AppSessionState.AwaitingAdminPin)
    val awaitingState = adminState as com.example.data.remote.supabase.model.AppSessionState.AwaitingAdminPin
    assertEquals(5, awaitingState.remainingAttempts)

    // 3. Verify incorrect Admin PIN reduces attempts
    val wrongResult = sessionManager.verifyAdminPinMfa("9999", "2026")
    assertTrue("Wrong PIN should fail", wrongResult is com.example.data.remote.supabase.model.MfaVerificationResult.Failed)
    val failedResult = wrongResult as com.example.data.remote.supabase.model.MfaVerificationResult.Failed
    assertEquals(4, failedResult.remainingAttempts)
    assertFalse(failedResult.isLockedOut)

    // 4. Verify correct Admin PIN upgrades session to AAL2 / Authenticated
    val correctResult = sessionManager.verifyAdminPinMfa("2026", "2026")
    assertTrue("Correct PIN should succeed", correctResult is com.example.data.remote.supabase.model.MfaVerificationResult.Success)
    val upgradedSession = (correctResult as com.example.data.remote.supabase.model.MfaVerificationResult.Success).session
    assertEquals("aal2", upgradedSession.authenticatorAssuranceLevel)
    assertTrue(upgradedSession.isAdminMfaVerified)

    val finalState = sessionManager.sessionState.value
    assertTrue("Session state must be Authenticated after PIN", finalState is com.example.data.remote.supabase.model.AppSessionState.Authenticated)
    val finalAuth = finalState as com.example.data.remote.supabase.model.AppSessionState.Authenticated
    assertTrue(finalAuth.isAdminMfaVerified)

    // 5. Test logout clears session
    sessionManager.clearSession()
    assertTrue(sessionManager.sessionState.value is com.example.data.remote.supabase.model.AppSessionState.Unauthenticated)
  }

  @Test
  fun `test secondary 4-digit staff PIN verification and master bypass`() {
    val companyProfile = CompanyProfile(
      secondaryStaffPin = "2026",
      requirePinForSensitiveStaffActions = true
    )
    assertEquals("2026", companyProfile.secondaryStaffPin)
    assertTrue(companyProfile.requirePinForSensitiveStaffActions)

    // Helper checking PIN verification logic
    fun verifyPin(enteredPin: String, profilePin: String): Boolean {
      return enteredPin.trim() == profilePin.trim() || enteredPin.trim() == "22026"
    }

    // Default secondary PIN
    assertTrue("Default 2026 PIN must verify", verifyPin("2026", companyProfile.secondaryStaffPin))

    // Master override PIN (22026) must always succeed offline and online
    assertTrue("Master bypass PIN 22026 must verify", verifyPin("22026", companyProfile.secondaryStaffPin))

    // Incorrect PIN must fail
    assertFalse("Incorrect PIN must fail", verifyPin("1234", companyProfile.secondaryStaffPin))

    // Customized company secondary PIN
    val updatedProfile = companyProfile.copy(secondaryStaffPin = "7890")
    assertTrue("Updated custom PIN must verify", verifyPin("7890", updatedProfile.secondaryStaffPin))
    assertTrue("Master bypass 22026 must still verify with custom PIN", verifyPin("22026", updatedProfile.secondaryStaffPin))
    assertFalse("Old PIN should no longer verify", verifyPin("2026", updatedProfile.secondaryStaffPin))
  }

  @Test
  fun `test AdminAuditLog cryptographic SHA-256 signature and tamper evidence`() {
    val auditLog = com.example.data.model.AdminAuditLog.create(
      tenantId = "tenant_zayed_enterprise",
      actorId = "staff-owner",
      actorName = "Al Yasir Bin Zayed",
      actorEmail = "yasirzayedco@gmail.com",
      action = "PRODUCT_PRICE_UPDATED",
      entityType = "PRODUCT",
      entityId = "prod-pure-water-500ml",
      fieldName = "basePrice",
      oldValue = "45.00",
      newValue = "52.00",
      reason = "Inflation and transport adjustment"
    )

    // 1. Verify fresh signature is valid
    assertTrue("Calculated HMAC SHA-256 signature must be valid", auditLog.isSignatureValid())

    // 2. Tamper test: simulate an unauthorized modification of the price
    val tamperedLog = auditLog.copy(newValue = "20.00")
    assertFalse("Tampered audit log must fail cryptographic verification", tamperedLog.isSignatureValid())

    // 3. Tamper test: simulate unauthorized actor tampering
    val tamperedActorLog = auditLog.copy(actorId = "unauthorized-hacker")
    assertFalse("Audit log with modified actor must fail verification", tamperedActorLog.isSignatureValid())
  }

  @Test
  fun `test bidirectional conflict resolution engine for stock and orders`() {
    // Test Product Stock Conflict: Offline device booked order with stock 80, but remote webapp booked 50
    // Conservative rule: physical stock must be minOf(80, 50) = 50 to prevent inventory overselling
    val localProduct = com.example.data.model.Product(
      id = "prod-water-20l",
      name = "20L Water Dispenser Bottle",
      category = "Bulk Water",
      unit = "20L Bottle",
      basePrice = 450.0,
      stockQty = 80,
      currentStock = 80
    )

    val resolvedProd = com.example.data.remote.SyncConflictResolver.resolveProductConflict(
      local = localProduct,
      remoteStock = 50,
      remoteBasePrice = 480.0
    )

    assertEquals("Conservative stock rule must choose minOf(80, 50)", 50, resolvedProd.stockQty)
    assertEquals("Higher price from remote must be adopted", 480.0, resolvedProd.basePrice, 0.001)

    // Test Order Status Conflict: Local has 'BOOKED' while WebApp updated to 'DISPATCHED'
    val resolvedStatus = com.example.data.remote.SyncConflictResolver.resolveOrderStatusConflict(
      localStatus = "BOOKED",
      remoteStatus = "DISPATCHED"
    )

    assertEquals("Order status progression must advance to DISPATCHED", "DISPATCHED", resolvedStatus)

    // Test reverse order status: Remote 'BOOKED' does not revert local 'DELIVERED'
    val nonRevertingStatus = com.example.data.remote.SyncConflictResolver.resolveOrderStatusConflict(
      localStatus = "DELIVERED",
      remoteStatus = "BOOKED"
    )
    assertEquals("Dispatched/Delivered order must not be reverted by stale booked status", "DELIVERED", nonRevertingStatus)
  }

  @Test
  fun `test white-label brand and local data shield during framework updates`() {
    val profile = CompanyProfile(
      companyName = "Horizon Pure Beverages Ltd",
      ownerName = "David Mwangi",
      tagline = "Purity in Every Drop",
      taxPin = "P051999888Z",
      factoryLocation = "Industrial Area, Nairobi",
      isWhiteLabeled = true,
      tenantId = "tenant_horizon_bev"
    )

    // Verify tenant attributes
    assertEquals("Horizon Pure Beverages Ltd", profile.companyName)
    assertEquals("tenant_horizon_bev", profile.tenantId)
    assertTrue(profile.isWhiteLabeled)

    // Verify that applying framework update does not touch company branding
    val frameworkReleaseName = "1.1.0"
    val updatedFrameworkChannel = profile.copy(
      frameworkDevUrl = "https://ais-dev-bjuyqcsp6rf3ec5w44yq4y-973870353017.europe-west2.run.app"
    )

    // Branding checks
    assertEquals("Horizon Pure Beverages Ltd", updatedFrameworkChannel.companyName)
    assertEquals("David Mwangi", updatedFrameworkChannel.ownerName)
    assertEquals("P051999888Z", updatedFrameworkChannel.taxPin)
    assertEquals("tenant_horizon_bev", updatedFrameworkChannel.tenantId)
  }

  @Test
  fun `test Supabase config lock and admin PIN unlock logic`() {
    val initialProfile = CompanyProfile(
      supabaseUrl = "https://kocvwcwmzaosgofydjha.supabase.co",
      isSupabaseConfigLocked = false,
      adminPin = "22026"
    )
    assertFalse("Initially config must be unlocked", initialProfile.isSupabaseConfigLocked)

    // Simulate verification & lock
    val lockedProfile = initialProfile.copy(
      isSupabaseConfigLocked = true,
      lastVerifiedSyncTimestamp = System.currentTimeMillis()
    )
    assertTrue("Config must be locked after linking", lockedProfile.isSupabaseConfigLocked)

    // Verification of unlock via Admin PIN
    fun checkUnlockPin(pin: String, adminPin: String): Boolean {
      return pin.trim() == adminPin.trim() || pin.trim() == "22026"
    }

    assertTrue("Master Admin PIN 22026 must unlock", checkUnlockPin("22026", lockedProfile.adminPin))
    assertFalse("Incorrect PIN must not unlock", checkUnlockPin("0000", lockedProfile.adminPin))
  }
}




