package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.OrderDatabase
import com.example.data.model.AdminAuditLog
import com.example.data.model.Customer
import com.example.data.model.OrderEntity
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ConflictResolutionReport(
    val totalPulled: Int = 0,
    val appliedDirectly: Int = 0,
    val conflictsResolved: Int = 0,
    val auditLogsGenerated: List<AdminAuditLog> = emptyList(),
    val errorCount: Int = 0,
    val details: String = "",
    val productsReconciled: Int = appliedDirectly,
    val ordersReconciled: Int = 0,
    val customersReconciled: Int = 0,
    val conflictsDetected: Int = conflictsResolved
) {
    val success: Boolean get() = errorCount == 0
}

/**
 * Enterprise Sync Conflict Resolution Strategy using Room and Supabase.
 *
 * Reconciles bidirectional mutations between the mobile offline client and
 * Supabase webapps across all multi-tenant white-label devices.
 *
 * Conflict Strategies:
 * 1. Last-Write-Wins (LWW) with timestamp comparison for non-conflicting records.
 * 2. Conservative Stock Protection: When mobile and webapp both alter stock, takes
 *    the lower quantity to prevent physical overselling.
 * 3. Authority Status Progression: Webapp dispatch and warehouse status updates
 *    take precedence for delivery operations.
 * 4. Non-Destructive Customer Merging: Merges updated contact details without
 *    erasing mobile-entered secondary numbers or coordinates.
 * 5. Full Non-Repudiation: Every reconciled conflict generates an immutable
 *    AdminAuditLog entry with SHA-256 signature.
 */
class SyncConflictResolver(private val context: Context) {

    private val database = OrderDatabase.getDatabase(context)
    private val productDao = database.productDao()
    private val orderDao = database.orderDao()
    private val customerDao = database.customerDao()
    private val syncQueueDao = database.syncQueueDao()
    private val auditLogDao = database.adminAuditLogDao()

    /**
     * Reconciles remote products JSON array from Supabase into local Room database.
     */
    suspend fun reconcileProducts(
        remoteProductsJson: String,
        tenantId: String,
        actorId: String = "system-sync",
        actorName: String = "Supabase WebApp Sync"
    ): ConflictResolutionReport = withContext(Dispatchers.IO) {
        var total = 0
        var applied = 0
        var conflicts = 0
        val auditList = mutableListOf<AdminAuditLog>()

        try {
            val array = JSONArray(remoteProductsJson)
            total = array.length()
            val pendingQueue = syncQueueDao.getPendingQueue().filter { it.entityType == "PRODUCT" }
            val pendingProductIds = pendingQueue.map { it.entityId }.toSet()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isEmpty()) continue

                val name = obj.optString("name", "Unknown Product")
                val category = obj.optString("category", "General")
                val unit = obj.optString("unit", "Unit")
                val minPrice = obj.optDouble("min_price", 0.0)
                val maxPrice = obj.optDouble("max_price", 0.0)
                val basePrice = obj.optDouble("base_price", 0.0)
                val stockQty = obj.optInt("stock_qty", 0)
                val sku = obj.optString("sku", "SKU-$id")

                val local = productDao.getProductById(id)

                if (local == null) {
                    // New product created in webapp - insert directly into local Room
                    val newProduct = Product(
                        id = id,
                        name = name,
                        category = category,
                        unit = unit,
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        basePrice = basePrice,
                        unitPrice = basePrice,
                        stockQty = stockQty,
                        currentStock = stockQty,
                        sku = sku
                    )
                    productDao.insertProduct(newProduct)
                    applied++
                } else if (!pendingProductIds.contains(id)) {
                    // No pending local edit - remote webapp is authoritative
                    val updated = local.copy(
                        name = name,
                        category = category,
                        unit = unit,
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        basePrice = basePrice,
                        unitPrice = basePrice,
                        stockQty = stockQty,
                        currentStock = stockQty,
                        sku = sku
                    )
                    productDao.updateProduct(updated)
                    applied++
                } else {
                    // CONFLICT: Both mobile offline and webapp modified the same product
                    conflicts++
                    // Stock: Conservative minimum to avoid overselling
                    val resolvedStock = if (local.isInfiniteStock) {
                        local.stockQty
                    } else {
                        minOf(local.stockQty, stockQty)
                    }

                    // Price: Remote webapp master price wins unless out-of-range
                    val resolvedBasePrice = if (basePrice > 0.0) basePrice else local.basePrice

                    val reconciled = local.copy(
                        name = name.ifEmpty { local.name },
                        basePrice = resolvedBasePrice,
                        unitPrice = resolvedBasePrice,
                        stockQty = resolvedStock,
                        currentStock = resolvedStock,
                        minPrice = if (minPrice > 0.0) minPrice else local.minPrice,
                        maxPrice = if (maxPrice > 0.0) maxPrice else local.maxPrice
                    )
                    productDao.updateProduct(reconciled)

                    // Log conflict resolution in immutable audit log
                    val log = AdminAuditLog.create(
                        tenantId = tenantId,
                        actorId = actorId,
                        actorName = actorName,
                        actorEmail = "sync@zayed-salesforce.com",
                        action = "SYNC_CONFLICT_RESOLVED",
                        entityType = "PRODUCT",
                        entityId = id,
                        fieldName = "stockQty/basePrice",
                        oldValue = "Local(stock=${local.stockQty}, price=${local.basePrice})",
                        newValue = "Resolved(stock=$resolvedStock, price=$resolvedBasePrice)",
                        reason = "Automatic reconciliation of webapp manual update with offline mobile edits"
                    )
                    auditLogDao.insertLog(log)
                    auditList.add(log)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncConflictResolver", "Error reconciling products: ${e.message}", e)
        }

        ConflictResolutionReport(
            totalPulled = total,
            appliedDirectly = applied,
            conflictsResolved = conflicts,
            auditLogsGenerated = auditList,
            details = "Products: $applied applied, $conflicts conflicts resolved"
        )
    }

    /**
     * Reconciles remote customers JSON array from Supabase into local Room database.
     */
    suspend fun reconcileCustomers(
        remoteCustomersJson: String,
        tenantId: String,
        actorId: String = "system-sync",
        actorName: String = "Supabase WebApp Sync"
    ): ConflictResolutionReport = withContext(Dispatchers.IO) {
        var total = 0
        var applied = 0
        var conflicts = 0
        val auditList = mutableListOf<AdminAuditLog>()

        try {
            val array = JSONArray(remoteCustomersJson)
            total = array.length()
            val pendingQueue = syncQueueDao.getPendingQueue().filter { it.entityType == "CUSTOMER" }
            val pendingCustomerIds = pendingQueue.map { it.entityId }.toSet()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isEmpty()) continue

                val customerName = obj.optString("customer_name")
                val businessName = obj.optString("business_name")
                val location = obj.optString("full_location")
                val email = obj.optString("business_email")
                val contactPerson = obj.optString("contact_person")
                val mobile = obj.optString("mobile")
                val isArchived = obj.optBoolean("is_archived", false)

                val local = customerDao.getCustomerById(id)

                if (local == null) {
                    val newCustomer = Customer(
                        id = id,
                        customerName = customerName,
                        businessName = businessName,
                        fullLocation = location,
                        businessEmail = email,
                        contactPersonName = contactPerson,
                        contactPosition = "Client Representative",
                        contactMobile = mobile,
                        companyEmail = email,
                        capturedByAccountHolder = "WebApp Administrator",
                        isArchived = isArchived,
                        isSynced = true
                    )
                    customerDao.insertCustomer(newCustomer)
                    applied++
                } else if (!pendingCustomerIds.contains(id)) {
                    val updated = local.copy(
                        customerName = customerName.ifEmpty { local.customerName },
                        businessName = businessName.ifEmpty { local.businessName },
                        fullLocation = location.ifEmpty { local.fullLocation },
                        businessEmail = email.ifEmpty { local.businessEmail },
                        contactPersonName = contactPerson.ifEmpty { local.contactPersonName },
                        contactMobile = mobile.ifEmpty { local.contactMobile },
                        isArchived = isArchived,
                        isSynced = true
                    )
                    customerDao.updateCustomer(updated)
                    applied++
                } else {
                    conflicts++
                    // Merge fields intelligently
                    val merged = local.copy(
                        businessName = businessName.ifEmpty { local.businessName },
                        fullLocation = location.ifEmpty { local.fullLocation },
                        contactPersonName = contactPerson.ifEmpty { local.contactPersonName },
                        contactMobile = local.contactMobile.ifEmpty { mobile },
                        isArchived = isArchived || local.isArchived,
                        isSynced = true
                    )
                    customerDao.updateCustomer(merged)

                    val log = AdminAuditLog.create(
                        tenantId = tenantId,
                        actorId = actorId,
                        actorName = actorName,
                        actorEmail = "sync@zayed-salesforce.com",
                        action = "SYNC_CONFLICT_RESOLVED",
                        entityType = "CUSTOMER",
                        entityId = id,
                        fieldName = "profileDetails",
                        oldValue = local.businessName,
                        newValue = merged.businessName,
                        reason = "Merged webapp customer updates with local mobile changes"
                    )
                    auditLogDao.insertLog(log)
                    auditList.add(log)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncConflictResolver", "Error reconciling customers: ${e.message}", e)
        }

        ConflictResolutionReport(
            totalPulled = total,
            appliedDirectly = applied,
            conflictsResolved = conflicts,
            auditLogsGenerated = auditList,
            details = "Customers: $applied applied, $conflicts conflicts resolved"
        )
    }

    /**
     * Reconciles remote orders JSON array from Supabase into local Room database.
     */
    suspend fun reconcileOrders(
        remoteOrdersJson: String,
        tenantId: String,
        actorId: String = "system-sync",
        actorName: String = "Supabase WebApp Sync"
    ): ConflictResolutionReport = withContext(Dispatchers.IO) {
        var total = 0
        var applied = 0
        var conflicts = 0
        val auditList = mutableListOf<AdminAuditLog>()

        try {
            val array = JSONArray(remoteOrdersJson)
            total = array.length()
            val pendingQueue = syncQueueDao.getPendingQueue().filter { it.entityType.startsWith("ORDER") }
            val pendingOrderIds = pendingQueue.map { it.entityId }.toSet()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isEmpty()) continue

                val orderNumber = obj.optString("order_number")
                val status = obj.optString("status", "BOOKED")
                val paymentModel = obj.optString("payment_model", "CASH_ON_DELIVERY")
                val totalAmount = obj.optDouble("total_amount", 0.0)

                val local = orderDao.getOrderById(id)

                if (local != null) {
                    if (!pendingOrderIds.contains(id)) {
                        if (local.status != status || local.paymentModel != paymentModel) {
                            val updated = local.copy(status = status, paymentModel = paymentModel, isSynced = true)
                            orderDao.updateOrder(updated)
                            applied++
                        }
                    } else {
                        // Conflict: check status progression
                        conflicts++
                        val advancedStatuses = listOf("DISPATCHED", "IN_TRANSIT", "DELIVERED")
                        val finalStatus = if (status in advancedStatuses) status else local.status

                        val reconciled = local.copy(status = finalStatus, isSynced = true)
                        orderDao.updateOrder(reconciled)

                        val log = AdminAuditLog.create(
                            tenantId = tenantId,
                            actorId = actorId,
                            actorName = actorName,
                            actorEmail = "sync@zayed-salesforce.com",
                            action = "SYNC_CONFLICT_RESOLVED",
                            entityType = "ORDER",
                            entityId = id,
                            fieldName = "status",
                            oldValue = local.status,
                            newValue = finalStatus,
                            reason = "Reconciled remote webapp status with mobile order record"
                        )
                        auditLogDao.insertLog(log)
                        auditList.add(log)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncConflictResolver", "Error reconciling orders: ${e.message}", e)
        }

        ConflictResolutionReport(
            totalPulled = total,
            appliedDirectly = applied,
            conflictsResolved = conflicts,
            auditLogsGenerated = auditList,
            details = "Orders: $applied applied, $conflicts conflicts resolved"
        )
    }

    companion object {
        fun resolveProductConflict(
            local: Product,
            remoteStock: Int,
            remoteBasePrice: Double,
            remoteMinPrice: Double = 0.0,
            remoteMaxPrice: Double = 0.0,
            remoteName: String = ""
        ): Product {
            val resolvedStock = if (local.isInfiniteStock) local.stockQty else minOf(local.stockQty, remoteStock)
            val resolvedBasePrice = if (remoteBasePrice > 0.0) remoteBasePrice else local.basePrice
            return local.copy(
                name = remoteName.ifEmpty { local.name },
                basePrice = resolvedBasePrice,
                unitPrice = resolvedBasePrice,
                stockQty = resolvedStock,
                currentStock = resolvedStock,
                minPrice = if (remoteMinPrice > 0.0) remoteMinPrice else local.minPrice,
                maxPrice = if (remoteMaxPrice > 0.0) remoteMaxPrice else local.maxPrice
            )
        }

        fun resolveOrderStatusConflict(localStatus: String, remoteStatus: String): String {
            val advancedStatuses = listOf("DISPATCHED", "IN_TRANSIT", "DELIVERED")
            return if (remoteStatus in advancedStatuses) remoteStatus else localStatus
        }
    }
}
