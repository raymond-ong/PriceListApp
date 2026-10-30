package com.example.pricelistapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["userName"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userName: String,
    val lastLoginDate: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
