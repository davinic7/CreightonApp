package com.devdavinic.creightonapp.data

import androidx.room.*
import com.devdavinic.creightonapp.model.DailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecord)

    // All records newest first
    @Query("SELECT * FROM daily_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllRecords(userId: String): Flow<List<DailyRecord>>

    // Returns all final records ascending - ViewModel calculates current cycle from this
    @Query("""
        SELECT * FROM daily_records
        WHERE userId = :userId AND isPartial = 0
        ORDER BY date ASC
    """)
    fun getCurrentCycleRecords(userId: String): Flow<List<DailyRecord>>

    // All final (non-partial) records ascending
    @Query("SELECT * FROM daily_records WHERE userId = :userId AND isPartial = 0 ORDER BY date ASC")
    fun getAllRecordsAscending(userId: String): Flow<List<DailyRecord>>

    // Today's partial record if exists
    @Query("""
        SELECT * FROM daily_records
        WHERE userId = :userId AND isPartial = 1
        AND date / 86400000 = :dayKey
        ORDER BY date DESC LIMIT 1
    """)
    suspend fun getTodayPartial(userId: String, dayKey: Long): DailyRecord?

    // Count of final records for today (for one-per-day enforcement)
    @Query("""
        SELECT COUNT(*) FROM daily_records
        WHERE userId = :userId AND isPartial = 0 AND isTestRecord = 0
        AND date / 86400000 = :dayKey
    """)
    suspend fun countFinalRecordsForDay(userId: String, dayKey: Long): Int

    // Yesterday's final record (for Peak Day detection)
    @Query("""
        SELECT * FROM daily_records
        WHERE userId = :userId AND isPartial = 0
        AND date / 86400000 = :yesterdayKey
        ORDER BY date DESC LIMIT 1
    """)
    suspend fun getYesterdayRecord(userId: String, yesterdayKey: Long): DailyRecord?

    // Update Peak Day flag on a specific record
    @Query("UPDATE daily_records SET isPeakDay = :isPeak, officialCode = :code WHERE id = :id")
    suspend fun updatePeakDay(id: Int, isPeak: Boolean, code: String)

    // Update systemSuggestsPeakYesterday flag
    @Query("UPDATE daily_records SET systemSuggestsPeakYesterday = :suggests WHERE id = :id")
    suspend fun updatePeakSuggestion(id: Int, suggests: Boolean)

    // Delete partial records for today (replaced by final record)
    @Query("DELETE FROM daily_records WHERE userId = :userId AND isPartial = 1 AND date / 86400000 = :dayKey")
    suspend fun deleteTodayPartials(userId: String, dayKey: Long)

    @Delete
    suspend fun deleteRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}