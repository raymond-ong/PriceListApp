package com.example.pricelistapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE is_deleted = 0 ORDER BY last_login_date DESC")
    fun getAllActiveUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE LOWER(user_name) = LOWER(:userName) AND is_deleted = 0 LIMIT 1")
    suspend fun findUserByName(userName: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}
