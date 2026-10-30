package com.example.pricelistapp.data

import com.example.pricelistapp.data.local.User
import com.example.pricelistapp.data.local.UserDao
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    val activeUsers: Flow<List<User>> = userDao.getAllActiveUsers()

    suspend fun createUser(userName: String): Result<User> {
        val trimmed = userName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("User name cannot be empty."))
        }

        val existing = userDao.findUserByName(trimmed)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("User '$trimmed' already exists."))
        }

        val newUser = User(
            userName = trimmed,
            lastLoginDate = System.currentTimeMillis()
        )
        val generatedId = userDao.insertUser(newUser)
        val createdUser = newUser.copy(id = generatedId)
        return Result.success(createdUser)
    }

    suspend fun loginUser(user: User): User {
        val updatedUser = user.copy(lastLoginDate = System.currentTimeMillis())
        userDao.updateUser(updatedUser)
        return updatedUser
    }
}
