package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.DownloadStatus

data class Download(
    val id: Long = 0,
    val sourceUrl: String,
    val fileName: String,
    val destinationUri: String,
    val mimeType: String? = null,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val totalSegments: Int = 1,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val refererHeader: String? = null,
    val userAgentHeader: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)
