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
    @Query("SELECT * FROM product WHERE is_deleted = 0")
    suspend fun getAllProductsWithVariants(): List<ProductWithVariants>

    @Transaction
    @Query("SELECT * FROM product WHERE cat_id = :catId AND is_deleted = 0")
    suspend fun getProductsByCategoryId(catId: String): List<ProductWithVariants>

    @Transaction
    @Query("SELECT * FROM product WHERE supplier_id = :supplierId AND is_deleted = 0")
    suspend fun getProductsBySupplierId(supplierId: String): List<ProductWithVariants>

    @Transaction
    @Query("SELECT DISTINCT p.* FROM product p INNER JOIN product_variant v ON p.id = v.product_id INNER JOIN customer_price_hist c ON v.id = c.variant_id WHERE c.customer_id = :customerId AND c.is_deleted = 0 AND p.is_deleted = 0")
    suspend fun getProductsByCustomerId(customerId: String): List<ProductWithVariants>

    @Query("DELETE FROM product_variant WHERE product_id = :productId")
    suspend fun deleteVariantsForProduct(productId: String)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditTrail(audit: ProductAuditTrailEntity)

    @Query("SELECT * FROM product_audit_trail WHERE product_id = :productId ORDER BY date DESC")
    suspend fun getAuditTrailForProduct(productId: String): List<ProductAuditTrailEntity>
}
