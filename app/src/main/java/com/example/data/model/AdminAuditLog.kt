package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest
import java.util.UUID

/**
 * Immutable Admin Audit Log entity for Zayed SalesForce and multi-tenant white-label instances.
 * Tracks all changes made to sensitive data fields (pricing, stock levels, customer statuses,
 * staff privileges, administrative PINs, and cloud backend configurations) for compliance,
 * non-repudiation, and troubleshooting.
 *
 * Each record includes a cryptographic SHA-256 hash signature computed across its immutable payload.
 */
@Entity(tableName = "admin_audit_logs")
data class AdminAuditLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tenantId: String,
    val actorId: String,
    val actorName: String,
    val actorEmail: String,
    val action: String, // e.g. "PRICE_MODIFIED", "STOCK_ADJUSTED", "CUSTOMER_ARCHIVED", "ROLE_PRIVILEGE_CHANGED", "ADMIN_PIN_CHANGED", "SUPABASE_LINKED", "SYNC_CONFLICT_RESOLVED", "FRAMEWORK_UPDATED"
    val entityType: String, // "PRODUCT", "CUSTOMER", "STAFF", "ORDER", "SETTINGS", "BACKEND", "SYSTEM"
    val entityId: String,
    val fieldName: String,
    val oldValue: String,
    val newValue: String,
    val reason: String = "Administrative operational change",
    val hashSignature: String = "",
    val isSynced: Boolean = false
) {
    /**
     * Verifies that the audit log record has not been tampered with by recalculating its SHA-256 digest.
     */
    fun isSignatureValid(): Boolean {
        val expected = computeSignature(
            id = id,
            timestamp = timestamp,
            tenantId = tenantId,
            actorId = actorId,
            action = action,
            entityId = entityId,
            fieldName = fieldName,
            oldValue = oldValue,
            newValue = newValue
        )
        return hashSignature.equals(expected, ignoreCase = true)
    }

    companion object {
        private const val AUDIT_SALT = "ZAYED_ENTERPRISE_COMPLIANCE_SALT_2026"

        fun computeSignature(
            id: String,
            timestamp: Long,
            tenantId: String,
            actorId: String,
            action: String,
            entityId: String,
            fieldName: String,
            oldValue: String,
            newValue: String
        ): String {
            val payload = "$id|$timestamp|$tenantId|$actorId|$action|$entityId|$fieldName|$oldValue|$newValue|$AUDIT_SALT"
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
                bytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                payload.hashCode().toString()
            }
        }

        fun create(
            tenantId: String,
            actorId: String,
            actorName: String,
            actorEmail: String,
            action: String,
            entityType: String,
            entityId: String,
            fieldName: String,
            oldValue: String,
            newValue: String,
            reason: String = "Standard operational change"
        ): AdminAuditLog {
            val logId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val signature = computeSignature(
                id = logId,
                timestamp = now,
                tenantId = tenantId,
                actorId = actorId,
                action = action,
                entityId = entityId,
                fieldName = fieldName,
                oldValue = oldValue,
                newValue = newValue
            )
            return AdminAuditLog(
                id = logId,
                timestamp = now,
                tenantId = tenantId,
                actorId = actorId,
                actorName = actorName,
                actorEmail = actorEmail,
                action = action,
                entityType = entityType,
                entityId = entityId,
                fieldName = fieldName,
                oldValue = oldValue,
                newValue = newValue,
                reason = reason,
                hashSignature = signature,
                isSynced = false
            )
        }
    }
}
