package com.devdavinic.creightonapp.data

import com.devdavinic.creightonapp.model.DailyRecord
import kotlinx.coroutines.flow.Flow

class RecordRepository(private val recordDao: RecordDao) {

    fun allRecords(userId: String): Flow<List<DailyRecord>> =
        recordDao.getAllRecords(userId)

    fun allRecordsAscending(userId: String): Flow<List<DailyRecord>> =
        recordDao.getAllRecordsAscending(userId)

    suspend fun insert(record: DailyRecord) =
        recordDao.insertRecord(record)

    suspend fun delete(record: DailyRecord) =
        recordDao.deleteRecord(record)

    suspend fun deleteAllForUser(userId: String) =
        recordDao.deleteAllForUser(userId)

    /** Returns today's partial record if one exists */
    suspend fun getTodayPartial(userId: String): DailyRecord? {
        val dayKey = System.currentTimeMillis() / 86_400_000L
        return recordDao.getTodayPartial(userId, dayKey)
    }

    /** Returns true if a final (non-test) record already exists for today */
    suspend fun hasFinalRecordForToday(userId: String): Boolean {
        val dayKey = System.currentTimeMillis() / 86_400_000L
        return recordDao.countFinalRecordsForDay(userId, dayKey) > 0
    }

    /** Returns yesterday's final record — used for Peak Day detection */
    suspend fun getYesterdayRecord(userId: String): DailyRecord? {
        val yesterdayKey = System.currentTimeMillis() / 86_400_000L - 1
        return recordDao.getYesterdayRecord(userId, yesterdayKey)
    }

    /** Delete today's partials before saving final record */
    suspend fun deleteTodayPartials(userId: String) {
        val dayKey = System.currentTimeMillis() / 86_400_000L
        recordDao.deleteTodayPartials(userId, dayKey)
    }

    /** Mark a record as Peak Day and update its official code */
    suspend fun updatePeakDay(id: Int, isPeak: Boolean, code: String) =
        recordDao.updatePeakDay(id, isPeak, code)

    /** Set the system Peak Day suggestion flag on yesterday's record */
    suspend fun updatePeakSuggestion(id: Int, suggests: Boolean) =
        recordDao.updatePeakSuggestion(id, suggests)
}