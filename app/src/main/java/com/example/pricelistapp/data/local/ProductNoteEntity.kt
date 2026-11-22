package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_notes")
data class ProductNoteEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    @ColumnInfo(name = "product_id")
    val productId: String,
    val notes: String,
    @ColumnInfo(name = "last_modified_date")
    val lastModifiedDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_modified_user")
    val lastModifiedUser: String
)
