package com.bmo.downloadmanager.domain.model

data class BrowserHistory(
    val id: Long = 0,
    val url: String,
    val title: String? = null,
    val favicon: ByteArray? = null,
    val visitCount: Int = 1,
    val lastVisitedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserHistory) return false
        return id == other.id &&
            url == other.url &&
            title == other.title &&
            (favicon?.contentEquals(other.favicon ?: byteArrayOf()) ?: (other.favicon == null)) &&
            visitCount == other.visitCount &&
            lastVisitedAt == other.lastVisitedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + visitCount
        result = 31 * result + lastVisitedAt.hashCode()
        return result
    }
}
