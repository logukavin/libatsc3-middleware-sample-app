package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.enities

import androidx.room.*

@Entity(tableName = "sg_schedule",
        indices = [Index(value = ["service_id"])]
)
data class SGScheduleEntity(
        @PrimaryKey
        val id: String,
        @ColumnInfo(name = "service_id")
        val serviceId: Int,
        val version: Long
)