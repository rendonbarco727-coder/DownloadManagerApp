package com.bmo.downloadmanager.data.mapper

import com.bmo.downloadmanager.core.database.entity.SettingsEntity
import com.bmo.downloadmanager.core.database.entity.SettingValueType as EntitySettingValueType
import com.bmo.downloadmanager.domain.enums.SettingValueType as DomainSettingValueType
import com.bmo.downloadmanager.domain.model.AppSettings

fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    key = key,
    value = value,
    type = DomainSettingValueType.valueOf(valueType.name),
    updatedAt = updatedAt
)

fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
    key = key,
    value = value,
    valueType = EntitySettingValueType.valueOf(type.name),
    updatedAt = updatedAt
)
