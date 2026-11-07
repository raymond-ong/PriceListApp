package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_price_hist")
data class SupplierPriceHistEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    @ColumnInfo(name = "user_id")
    val userId: String,
    val date: Long,
    @ColumnInfo(name = "variant_id")
    val variantId: String
)
