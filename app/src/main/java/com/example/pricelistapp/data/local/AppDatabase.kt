package com.example.pricelistapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        DeviceCache::class,
        CategoryEntity::class,
        SupplierEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        SupplierPriceHistEntity::class,
        CustomerPriceHistEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceCacheDao(): DeviceCacheDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pricelist_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

                if (SampleDataConfig.ENABLE_SAMPLE_DATA) {
                    kotlinx.coroutines.runBlocking {
                        SampleDataInitializer.initializeIfNeeded(context.applicationContext, instance)
                    }
                }

                INSTANCE = instance
                instance
            }
        }
    }
}
