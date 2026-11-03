package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItemsById(orderId: String): OrderWithItems?

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<OrderWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun insertCompleteOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET scheduledDeliveryDate = :scheduledDate, status = 'SCHEDULED' WHERE id = :orderId")
    suspend fun scheduleDelivery(orderId: String, scheduledDate: String)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders SET status = 'DELIVERED', isPaid = 1, commissionStatus = 'PAID', commissionPaymentCode = :paymentCode, commissionAmount = :commissionAmount, commissionPaidAt = :paidAt WHERE id = :orderId")
    suspend fun approveDeliveryAndPaymentWithCommission(orderId: String, paymentCode: String, commissionAmount: Double, paidAt: Long)

    @Query("UPDATE orders SET commissionType = :type, commissionRate = :rate, commissionAmount = :amount WHERE id = :orderId")
    suspend fun updateCommissionCalculation(orderId: String, type: String, rate: Double, amount: Double)

    @Query("UPDATE orders SET isPaid = :isPaid WHERE id = :orderId")
    suspend fun updatePaymentStatus(orderId: String, isPaid: Boolean)

    @Transaction
    @Query("SELECT * FROM orders WHERE accountHolderName = :repName OR accountHolderEmail = :repEmail ORDER BY createdAt DESC")
    fun getOrdersByRep(repName: String, repEmail: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE customerId = :customerId OR customerMobile = :mobile OR customerName = :customerName ORDER BY createdAt DESC")
    fun getOrdersByCustomer(customerId: String, mobile: String = "", customerName: String = ""): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE customerId = :identifier OR customerMobile = :identifier OR customerName = :identifier OR accountHolderEmail = :identifier ORDER BY createdAt DESC")
    fun getOrdersByCustomerIdentifier(identifier: String): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE status = 'REQUESTED' OR status = 'BACKORDER' ORDER BY createdAt DESC")
    fun getBackorderRequests(): Flow<List<OrderWithItems>>

    @Query("UPDATE orders SET isSynced = 1 WHERE id = :orderId")
    suspend fun markAsSynced(orderId: String)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("SELECT COUNT(*) FROM orders WHERE isSynced = 0")
    fun getUnsyncedOrderCountFlow(): Flow<Int>
}
