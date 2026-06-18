package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.SegmentStatus

data class DownloadSegment(
    val id: Long = 0,
    val downloadId: Long,
    val segmentIndex: Int,
    val rangeStart: Long,
    val rangeEnd: Long,
    val bytesDownloaded: Long = 0L,
    val segmentUrl: String,
    val status: SegmentStatus = SegmentStatus.PENDING,
    val tempFilePath: String? = null
)
