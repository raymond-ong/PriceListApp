package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    val name: String,
    val description: String? = null,
    @ColumnInfo(name = "cat_id")
    val catId: String,
    @ColumnInfo(name = "supplier_id")
    val supplierId: String
)
