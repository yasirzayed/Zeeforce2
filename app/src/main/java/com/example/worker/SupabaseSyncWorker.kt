package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.OrderDatabase
import com.example.data.remote.SupabaseSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = OrderDatabase.getDatabase(applicationContext)
            val syncQueueDao = database.syncQueueDao()
            val customerDao = database.customerDao()
            val orderDao = database.orderDao()
            val commissionDao = database.commissionDao()
            val syncService = SupabaseSyncService(applicationContext)
            val config = syncService.supabaseClient.config.value

            val pendingItems = syncQueueDao.getPendingQueue()
            if (pendingItems.isEmpty()) {
                return@withContext Result.success()
            }

            for (item in pendingItems) {
                val table = when (item.entityType) {
                    "CUSTOMER" -> "customers"
                    "ORDER" -> "orders"
                    "COMMISSION" -> "commissions"
                    "ACTIVITY" -> "staff_activities"
                    "ORDER_STATUS" -> "orders" // Patch status on orders table
                    "STAFF" -> "profiles" // Synchronize staff/profiles
                    "PRODUCT" -> "products"
                    "SUPPLIER" -> "suppliers"
                    "SUPPLIER_PRODUCT" -> "supplier_products"
                    "SUPPLIER_ORDER" -> "supplier_orders"
                    "SUPPLIER_ORDER_ITEM" -> "supplier_order_items"
                    "AUDIT_LOG" -> "admin_audit_logs"
                    else -> "records"
                }
                
                val syncCall = syncService.syncPayload(
                    supabaseUrl = config.url,
                    supabaseAnonKey = config.anonKey,
                    table = table,
                    payloadJson = item.payloadJson
                )

                if (syncCall.isSuccess) {
                    syncQueueDao.markSynced(item.id)
                    when (item.entityType) {
                        "CUSTOMER" -> customerDao.markAsSynced(item.entityId)
                        "ORDER" -> orderDao.markAsSynced(item.entityId)
                        "ORDER_STATUS" -> orderDao.markAsSynced(item.entityId)
                        "COMMISSION" -> commissionDao.markSynced(item.entityId)
                        "AUDIT_LOG" -> database.adminAuditLogDao().markSynced(item.entityId)
                    }
                } else {
                    syncQueueDao.markFailed(item.id, syncCall.exceptionOrNull()?.message ?: "Sync error")
                }
            }

            syncQueueDao.cleanSyncedItems()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "supabase_background_sync_work"
        const val EXPEDITED_WORK_NAME = "supabase_expedited_sync_work"
        const val PERIODIC_WORK_NAME = "supabase_periodic_sync_work"

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncWorkRequest = OneTimeWorkRequestBuilder<SupabaseSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncWorkRequest
                )
            } catch (e: Exception) {
                android.util.Log.w("SupabaseSyncWorker", "WorkManager not available: ${e.message}")
            }
        }

        fun scheduleExpedited(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncWorkRequest = OneTimeWorkRequestBuilder<SupabaseSyncWorker>()
                    .setConstraints(constraints)
                    .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    EXPEDITED_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    syncWorkRequest
                )
            } catch (_: Exception) {
                schedule(context)
            }
        }

        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicRequest = androidx.work.PeriodicWorkRequestBuilder<SupabaseSyncWorker>(
                    15, java.util.concurrent.TimeUnit.MINUTES
                ).setConstraints(constraints).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            } catch (e: Exception) {
                android.util.Log.w("SupabaseSyncWorker", "WorkManager not available: ${e.message}")
            }
        }
    }
}
