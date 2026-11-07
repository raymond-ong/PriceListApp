package com.example.pricelistapp.data

import com.example.pricelistapp.data.local.DeviceCache
import com.example.pricelistapp.data.local.DeviceCacheDao
import com.example.pricelistapp.data.local.User
import com.example.pricelistapp.data.local.UserDao
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val deviceCacheDao: DeviceCacheDao
) {
    companion object {
        const val KEY_LAST_USERNAME_ROLE = "last_username_role"
    }

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
        userDao.insertUser(newUser)
        val createdUser = newUser
        saveLastLoggedInUser(createdUser.userName)
        return Result.success(createdUser)
    }

    suspend fun loginUser(user: User): User {
        val updatedUser = user.copy(lastLoginDate = System.currentTimeMillis())
        userDao.updateUser(updatedUser)
        saveLastLoggedInUser(updatedUser.userName)
        return updatedUser
    }

    suspend fun getLastUsernameRoleCache(): DeviceCache? {
        return deviceCacheDao.getCacheByKey(KEY_LAST_USERNAME_ROLE)
    }

    fun getLastUsernameRoleFlow(): Flow<DeviceCache?> {
        return deviceCacheDao.getCacheFlowByKey(KEY_LAST_USERNAME_ROLE)
    }

    private suspend fun saveLastLoggedInUser(userName: String) {
        val currentCache = deviceCacheDao.getCacheByKey(KEY_LAST_USERNAME_ROLE)
        val subvalueToKeep = if (currentCache?.value.equals(userName, ignoreCase = true)) {
            currentCache?.subvalue
        } else {
            null // erase subvalue if logging in as a different user
        }
        deviceCacheDao.insertOrUpdate(
            DeviceCache(
                key = KEY_LAST_USERNAME_ROLE,
                value = userName,
                subvalue = subvalueToKeep
            )
        )
    }

    suspend fun saveLastLoggedInRole(role: String) {
        val currentCache = deviceCacheDao.getCacheByKey(KEY_LAST_USERNAME_ROLE)
        val currentUser = currentCache?.value
        deviceCacheDao.insertOrUpdate(
            DeviceCache(
                key = KEY_LAST_USERNAME_ROLE,
                value = currentUser,
                subvalue = role
            )
        )
    }
}
