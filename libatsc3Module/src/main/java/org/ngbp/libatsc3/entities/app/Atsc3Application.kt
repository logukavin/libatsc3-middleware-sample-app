package org.ngbp.libatsc3.entities.app

data class Atsc3Application(
        val appContextIdList: List<String>,
        val cachePath: String,
        val files: Map<String, Atsc3ApplicationFile>
) {
    fun updateFiles(list: List<Atsc3ApplicationFile>): Atsc3Application {
        val newFiles = HashMap<String, Atsc3ApplicationFile>(this.files)
        list.forEach { file ->
            newFiles[file.contentLocation] = file
        }
        return Atsc3Application(this.appContextIdList, this.cachePath, newFiles)
    }
}