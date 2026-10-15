package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "build_history")
data class BuildRecord(
    @PrimaryKey val id: String, // commitSha or UUID
    val projectName: String,
    val repoName: String,
    val status: String, // "SUCCESS", "FAILURE", "IN_PROGRESS"
    val elapsedSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val logsText: String,
    val artifactId: Long? = null,
    val artifactDownloadUrl: String? = null,
    val artifactSize: Long? = null,
    val apkFileName: String? = null
)
