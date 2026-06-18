package com.bmo.downloadmanager.domain.model

import com.bmo.downloadmanager.domain.enums.SettingValueType

data class AppSettings(
    val key: String,
    val value: String,
    val type: SettingValueType = SettingValueType.STRING,
    val updatedAt: Long = System.currentTimeMillis()
)
