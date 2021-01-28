package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.enities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "content_description",
        primaryKeys = ["contentId", "language"],
        foreignKeys = [ForeignKey(
                entity = SGContentEntity::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("contentId"),
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
        )]
)
data class ContentDescriptionEntity(
        val contentId: String,
        val language: String,
        val description: String
)