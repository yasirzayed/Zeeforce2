package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AdminAuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminAuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AdminAuditLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AdminAuditLog>)

    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AdminAuditLog>>

    @Query("SELECT * FROM admin_audit_logs WHERE tenantId = :tenantId ORDER BY timestamp DESC")
    fun getLogsByTenantFlow(tenantId: String): Flow<List<AdminAuditLog>>

    @Query("SELECT * FROM admin_audit_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLogs(): List<AdminAuditLog>

    @Query("UPDATE admin_audit_logs SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM admin_audit_logs")
    suspend fun getLogCount(): Int
}
