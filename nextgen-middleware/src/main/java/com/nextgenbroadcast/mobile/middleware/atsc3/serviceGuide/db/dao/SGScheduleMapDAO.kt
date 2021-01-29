package com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.db.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Query

@Dao
interface SGScheduleMapDAO {

    @Query("SELECT *, :bsid AS bsid, :category AS category FROM sg_service")
    fun getAllService(bsid: Int, category: Int): Cursor

    @Query("SELECT *, :bsid AS bsid, :category AS category FROM sg_service WHERE serviceId = :id")
    fun getServiceById(id: Int, bsid: Int, category: Int): Cursor

    @Query("SELECT sch.*, schc.contentId, schp.startTime, schp.endTime, schp.duration, cnt.icon, cntn.name, cntd.description\n" +
            "FROM sg_schedule sch, sg_schedule_content schc, sg_presentation schp\n" +
            "LEFT JOIN sg_content cnt ON cnt.id = schc.contentId\n" +
            "LEFT JOIN (SELECT * FROM (SELECT * FROM sg_content_name WHERE language = :lang UNION ALL SELECT * FROM sg_content_name WHERE language NOTNULL) GROUP BY contentId) cntn ON cntn.contentId = schc.contentId\n" +
            "LEFT JOIN (SELECT * FROM (SELECT * FROM sg_content_description WHERE language = :lang UNION ALL SELECT * FROM sg_content_description WHERE language NOTNULL) GROUP BY contentId) cntd ON cntd.contentId = schc.contentId\n" +
            "WHERE sch.service_id = :serviceId AND schc.scheduleId = sch.id AND schp.content_Id = schc.contentId AND schp.endTime > :startTime AND schp.startTime < :endTime")
    fun getContentBy(serviceId: Int, startTime: Long, endTime: Long, lang: String): Cursor
}