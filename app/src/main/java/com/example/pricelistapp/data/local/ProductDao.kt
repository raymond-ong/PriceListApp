package com.example.pricelistapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierPriceHist(hist: SupplierPriceHistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerPriceHist(hist: CustomerPriceHistEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Update
    suspend fun updateVariant(variant: ProductVariantEntity)

    @Transaction
    @Query("SELECT * FROM product")
    suspend fun getAllProductsWithVariants(): List<ProductWithVariants>

    @Query("SELECT * FROM supplier_price_hist WHERE variant_id = :variantId ORDER BY date DESC")
    suspend fun getSupplierPriceHistoryForVariant(variantId: String): List<SupplierPriceHistEntity>

    @Query("SELECT * FROM customer_price_hist WHERE variant_id = :variantId ORDER BY date DESC")
    suspend fun getCustomerPriceHistoryForVariant(variantId: String): List<CustomerPriceHistEntity>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId")
    suspend fun getSupplierById(supplierId: String): SupplierEntity?

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?
}
