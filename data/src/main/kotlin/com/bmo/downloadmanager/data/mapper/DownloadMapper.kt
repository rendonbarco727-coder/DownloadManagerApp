package com.bmo.downloadmanager.data.mapper

import com.bmo.downloadmanager.core.database.entity.DownloadEntity
import com.bmo.downloadmanager.core.database.entity.DownloadSegmentEntity
import com.bmo.downloadmanager.core.database.entity.DownloadStatus as EntityDownloadStatus
import com.bmo.downloadmanager.core.database.entity.SegmentStatus as EntitySegmentStatus
import com.bmo.downloadmanager.domain.enums.DownloadStatus as DomainDownloadStatus
import com.bmo.downloadmanager.domain.enums.SegmentStatus as DomainSegmentStatus
import com.bmo.downloadmanager.domain.model.Download
import com.bmo.downloadmanager.domain.model.DownloadSegment

fun DownloadEntity.toDomain(): Download = Download(
    id = id,
    sourceUrl = sourceUrl,
    fileName = fileName,
    destinationUri = destinationUri,
    mimeType = mimeType,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    totalSegments = totalSegments,
    status = DomainDownloadStatus.valueOf(status.name),
    refererHeader = refererHeader,
    userAgentHeader = userAgentHeader,
    createdAt = createdAt,
    completedAt = completedAt,
    errorMessage = errorMessage
)

fun Download.toEntity(): DownloadEntity = DownloadEntity(
    id = id,
    sourceUrl = sourceUrl,
    fileName = fileName,
    destinationUri = destinationUri,
    mimeType = mimeType,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    totalSegments = totalSegments,
    status = EntityDownloadStatus.valueOf(status.name),
    refererHeader = refererHeader,
    userAgentHeader = userAgentHeader,
    createdAt = createdAt,
    completedAt = completedAt,
    errorMessage = errorMessage
)

fun DownloadSegmentEntity.toDomain(): DownloadSegment = DownloadSegment(
    id = id,
    downloadId = downloadId,
    segmentIndex = segmentIndex,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    bytesDownloaded = bytesDownloaded,
    segmentUrl = segmentUrl,
    status = DomainSegmentStatus.valueOf(status.name),
    tempFilePath = tempFilePath
)

fun DownloadSegment.toEntity(): DownloadSegmentEntity = DownloadSegmentEntity(
    id = id,
    downloadId = downloadId,
    segmentIndex = segmentIndex,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    bytesDownloaded = bytesDownloaded,
    segmentUrl = segmentUrl,
    status = EntitySegmentStatus.valueOf(status.name),
    tempFilePath = tempFilePath
)
