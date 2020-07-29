package org.ngbp.libatsc3.entities.app

data class Atsc3ApplicationFile(
        val contentLocation: String,
        val contentType: String?,
        val version: Int
)