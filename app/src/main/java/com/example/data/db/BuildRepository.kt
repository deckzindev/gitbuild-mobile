package com.example.data.db

import kotlinx.coroutines.flow.Flow

class BuildRepository(private val dao: BuildRecordDao) {

    val allBuilds: Flow<List<BuildRecord>> = dao.getAllBuilds()

    suspend fun saveBuild(record: BuildRecord) {
        dao.insertOrUpdate(record)
    }

    suspend fun getBuild(id: String): BuildRecord? {
        return dao.getBuildById(id)
    }

    suspend fun deleteBuild(id: String) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
