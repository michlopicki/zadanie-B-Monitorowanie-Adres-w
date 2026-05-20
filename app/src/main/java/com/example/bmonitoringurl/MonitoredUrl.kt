package com.example.bmonitoringurl

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monitored_urls",
    indices = [Index(value = ["url"], unique = true)]
)
data class MonitoredUrl(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    var lastContentHash: String? = null
)