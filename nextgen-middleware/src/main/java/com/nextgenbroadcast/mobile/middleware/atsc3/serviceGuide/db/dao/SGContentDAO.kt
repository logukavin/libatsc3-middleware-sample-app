package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.enities.*

@Dao
interface SGContentDAO : BaseDao<SGContentEntity> {

    @Query("SELECT * FROM sg_content")
    fun getAll(): List<SGContentEntity>

    @Query("SELECT name FROM content_name WHERE contentId = :id AND language = :language")
    fun getAllName(id: String, language: String): List<String>

    //TODO: move to separate DAO
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg content: ContentNameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg content: ContentDescriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg content: ContentServiceIdEntity)

    @Query("SELECT * FROM sg_content")
    fun getAllLD(): LiveData<List<SGContentEntity>>
}