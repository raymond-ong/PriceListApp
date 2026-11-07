package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_variant")
data class ProductVariantEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    @ColumnInfo(name = "product_id")
    val productId: String,
    val name: String,
    @ColumnInfo(name = "supplier_price")
    val supplierPrice: Double,
    val unit: String,
    @ColumnInfo(name = "selling_price")
    val sellingPrice: Double
)
