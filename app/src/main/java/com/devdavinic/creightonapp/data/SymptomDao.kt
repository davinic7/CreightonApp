package com.devdavinic.creightonapp.data

import androidx.room.*
import com.devdavinic.creightonapp.model.DailySymptom
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: DailySymptom)

    @Query("SELECT * FROM daily_symptoms WHERE userId = :userId ORDER BY date DESC")
    fun getAllSymptoms(userId: String): Flow<List<DailySymptom>>

    @Query("""
        SELECT * FROM daily_symptoms WHERE userId = :userId
        AND date / 86400000 = :dayKey LIMIT 1
    """)
    suspend fun getSymptomForDay(userId: String, dayKey: Long): DailySymptom?

    @Query("""
        SELECT * FROM daily_symptoms WHERE userId = :userId
        ORDER BY date DESC LIMIT :limit
    """)
    suspend fun getRecentSymptoms(userId: String, limit: Int = 14): List<DailySymptom>

    @Delete
    suspend fun deleteSymptom(symptom: DailySymptom)

    @Query("DELETE FROM daily_symptoms WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}