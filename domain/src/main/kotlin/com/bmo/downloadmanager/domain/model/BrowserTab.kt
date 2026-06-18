package com.bmo.downloadmanager.domain.model

data class BrowserTab(
    val id: Long = 0,
    val currentUrl: String,
    val title: String? = null,
    val favicon: ByteArray? = null,
    val tabOrder: Int,
    val isActive: Boolean = false,
    val webViewState: ByteArray? = null,
    val isSessionRestored: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrowserTab) return false
        return id == other.id &&
            currentUrl == other.currentUrl &&
            title == other.title &&
            (favicon?.contentEquals(other.favicon ?: byteArrayOf()) ?: (other.favicon == null)) &&
            tabOrder == other.tabOrder &&
            isActive == other.isActive &&
            (webViewState?.contentEquals(other.webViewState ?: byteArrayOf()) ?: (other.webViewState == null)) &&
            isSessionRestored == other.isSessionRestored &&
            createdAt == other.createdAt &&
            lastAccessedAt == other.lastAccessedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + currentUrl.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + tabOrder
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (webViewState?.contentHashCode() ?: 0)
        result = 31 * result + isSessionRestored.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastAccessedAt.hashCode()
        return result
    }
}
