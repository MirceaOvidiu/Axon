package com.axon.data.impl

import com.axon.data.local.dao.SessionDao
import com.axon.data.mapper.toDomain
import com.axon.data.mapper.toEntity
import com.axon.domain.model.SensorData
import com.axon.domain.model.Session
import com.axon.domain.model.SessionStats
import com.axon.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImplementation
    @Inject
    constructor(
        private val sessionDao: SessionDao,
    ) : SessionRepository {
        override fun getAllSessions(): Flow<List<Session>> =
            sessionDao.getAllSessionsFlow().map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getSession(sessionId: Long): Session? = sessionDao.getSession(sessionId)?.toDomain()

        override suspend fun insertSession(session: Session): Long = sessionDao.insertSession(session.toEntity())

        override suspend fun deleteSession(sessionId: Long) {
            sessionDao.deleteSensorDataBySession(sessionId)
            sessionDao.deleteSession(sessionId)
        }

        override suspend fun getSensorDataBySession(sessionId: Long): List<SensorData> =
            sessionDao.getSensorDataBySession(sessionId).map {
                it.toDomain()
            }

        override suspend fun getSessionStats(sessionId: Long): SessionStats? {
            val session = sessionDao.getSession(sessionId) ?: return null
            val avgHr = sessionDao.getAverageHeartRate(sessionId)
            val maxHr = sessionDao.getMaxHeartRate(sessionId)
            val minHr = sessionDao.getMinHeartRate(sessionId)
            val dataPointCount = sessionDao.getSensorDataCount(sessionId)
            val duration = session.endTime - session.startTime

            return SessionStats(
                averageHeartRate = avgHr,
                maxHeartRate = maxHr,
                minHeartRate = minHr,
                duration = duration,
                dataPointCount = dataPointCount,
            )
        }
    }
