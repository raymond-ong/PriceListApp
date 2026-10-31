package com.example.pricelistapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_cache")
data class DeviceCache(
    @PrimaryKey
    val key: String,
    val value: String? = null,
    val subvalue: String? = null
)
