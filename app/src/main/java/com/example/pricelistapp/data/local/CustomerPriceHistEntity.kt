package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_price_hist")
data class CustomerPriceHistEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    @ColumnInfo(name = "user_id")
    val userId: String,
    val date: Long,
    @ColumnInfo(name = "variant_id")
    val variantId: String,
    @ColumnInfo(name = "customer_id")
    val customerId: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Int = 0,
    @ColumnInfo(name = "is_default_price")
    val isDefaultPrice: Int = 0
)
