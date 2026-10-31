package com.example.pricelistapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceCacheDao {
    @Query("SELECT * FROM device_cache WHERE `key` = :key LIMIT 1")
    suspend fun getCacheByKey(key: String): DeviceCache?

    @Query("SELECT * FROM device_cache WHERE `key` = :key LIMIT 1")
    fun getCacheFlowByKey(key: String): Flow<DeviceCache?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: DeviceCache)
}
