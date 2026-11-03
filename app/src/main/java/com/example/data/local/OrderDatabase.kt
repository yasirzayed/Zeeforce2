package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.Supplier
import com.example.data.model.SupplierProduct
import com.example.data.model.SupplierOrder
import com.example.data.model.SupplierOrderItem
import com.example.data.model.SyncQueueItem
import com.example.data.model.StaffActivityLog
import com.example.data.model.StaffMember
import com.example.data.model.CommissionRecord
import com.example.data.model.CustomerOrder
import com.example.data.local.SupplierDao

import com.example.data.model.AdminAuditLog

@Database(
    entities = [
        Customer::class,
        Product::class,
        OrderEntity::class,
        OrderItemEntity::class,
        CustomerOrder::class,
        SyncQueueItem::class,
        StaffMember::class,
        CommissionRecord::class,
        StaffActivityLog::class,
        Supplier::class,
        SupplierProduct::class,
        SupplierOrder::class,
        SupplierOrderItem::class,
        AdminAuditLog::class
    ],
    version = 12,
    exportSchema = false
)
abstract class OrderDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun customerOrderDao(): CustomerOrderDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun staffDao(): StaffDao
    abstract fun commissionDao(): CommissionDao
    abstract fun staffActivityDao(): StaffActivityDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierOrderDao(): SupplierOrderDao
    abstract fun adminAuditLogDao(): AdminAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: OrderDatabase? = null

        fun getDatabase(context: Context): OrderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrderDatabase::class.java,
                    "zayed_salesforce_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
