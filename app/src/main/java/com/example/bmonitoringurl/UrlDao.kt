package com.example.bmonitoringurl

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UrlDao {
    @Query("SELECT * FROM monitored_urls")
    suspend fun getAllUrls(): List<MonitoredUrl>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(url: MonitoredUrl)

    @Update
    suspend fun update(url: MonitoredUrl)

    @Delete
    suspend fun delete(url: MonitoredUrl)
}