package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.enities.*

@Dao
interface SGContentDAO : BaseDao<SGContentEntity> {

    @Query("SELECT * FROM sg_content")
    fun getAll(): List<SGContentEntity>

    @Query("SELECT * FROM sg_content")
    fun getAllLD(): LiveData<List<SGContentEntity>>
}