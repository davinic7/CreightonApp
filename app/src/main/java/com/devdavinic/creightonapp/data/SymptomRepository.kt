package com.devdavinic.creightonapp.data

import com.devdavinic.creightonapp.model.DailySymptom
import kotlinx.coroutines.flow.Flow

class SymptomRepository(private val symptomDao: SymptomDao) {

    fun allSymptoms(userId: String): Flow<List<DailySymptom>> =
        symptomDao.getAllSymptoms(userId)

    suspend fun insert(symptom: DailySymptom) =
        symptomDao.insertSymptom(symptom)

    suspend fun getSymptomForDay(userId: String, dayKey: Long): DailySymptom? =
        symptomDao.getSymptomForDay(userId, dayKey)

    suspend fun getRecentSymptoms(userId: String, limit: Int = 14): List<DailySymptom> =
        symptomDao.getRecentSymptoms(userId, limit)

    suspend fun delete(symptom: DailySymptom) =
        symptomDao.deleteSymptom(symptom)

    suspend fun deleteAllForUser(userId: String) =
        symptomDao.deleteAllForUser(userId)
}