package com.example.pricelistapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["user_name"], unique = true)]
)
data class User(
    @PrimaryKey
    val id: String = UlidGenerator.generate(),
    @ColumnInfo(name = "user_name")
    val userName: String,
    @ColumnInfo(name = "last_login_date")
    val lastLoginDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
