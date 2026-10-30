package com.example.pricelistapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY lastLoginDate DESC")
    fun getAllActiveUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE LOWER(userName) = LOWER(:userName) AND isDeleted = 0 LIMIT 1")
    suspend fun findUserByName(userName: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}
