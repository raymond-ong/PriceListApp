package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["name"], unique = true)]
)
data class SupplierEntity(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    val name: String,
    val address: String? = null,
    @ColumnInfo(name = "contact_num")
    val contactNum: String? = null,
    val notes: String? = null
)
