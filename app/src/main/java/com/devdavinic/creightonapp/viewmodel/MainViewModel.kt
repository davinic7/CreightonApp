package com.devdavinic.creightonapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devdavinic.creightonapp.data.RecordRepository
import com.devdavinic.creightonapp.data.SymptomRepository
import com.devdavinic.creightonapp.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: RecordRepository,
    private val symptomRepository: SymptomRepository? = null,
    private val userId: String,
    val isTestMode: Boolean = false,
    private val appContext: android.content.Context? = null
) : ViewModel() {

    // =========================================================================
    // DATA STREAMS
    // =========================================================================

    // Symptom streams
    val allSymptoms: StateFlow<List<DailySymptom>> = symptomRepository?.allSymptoms(userId)
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        ?: MutableStateFlow(emptyList())

    val allRecords: StateFlow<List<DailyRecord>> = repository.allRecords(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allRecordsAscending: StateFlow<List<DailyRecord>> =
        repository.allRecordsAscending(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // currentCycleRecords derived in Kotlin — last cycle from groupAllCycles
    // This correctly handles consecutive H/M days as the same cycle
    val currentCycleRecords: StateFlow<List<DailyRecord>> = allRecordsAscending
        .map { records -> groupAllCycles(records).lastOrNull() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // =========================================================================
    // CYCLE ANALYSIS
    // =========================================================================

    val cycleAnalysis: StateFlow<CycleAnalysis?> = allRecordsAscending
        .map { allRecs ->
            if (allRecs.isEmpty()) return@map null
            val allCycles    = groupAllCycles(allRecs)
            val currentCycle = allCycles.lastOrNull() ?: emptyList()
            CycleAnalysisEngine.analyze(currentCycle = currentCycle, allCycles = allCycles)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // =========================================================================
    // UI STATE
    // =========================================================================

    private val _todayAlreadyRegistered = MutableStateFlow(false)
    val todayAlreadyRegistered: StateFlow<Boolean> = _todayAlreadyRegistered

    // System suggestion: "yesterday might have been Peak Day"
    private val _peakDaySuggestion = MutableStateFlow<DailyRecord?>(null)
    val peakDaySuggestion: StateFlow<DailyRecord?> = _peakDaySuggestion

    // Today's partial record (if any)
    private val _todayPartial = MutableStateFlow<DailyRecord?>(null)
    val todayPartial: StateFlow<DailyRecord?> = _todayPartial

    init {
        viewModelScope.launch {
            _todayPartial.value = repository.getTodayPartial(userId)
        }
    }

    // =========================================================================
    // SAVE PARTIAL OBSERVATION (during the day)
    // Increments observationCount if a partial already exists for today.
    // Uses Mayor Signo: only updates if new sign is MORE fertile than existing.
    // =========================================================================

    fun savePartialObservation(
        bleeding: BleedingLevel?,
        sensation: Sensation?,
        consistency: MucusConsistency?,
        color: MucusColor?,
        lubrication: LubricationSensation?,
        hasIntercourse: Boolean,
        breastSelfExam: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.getTodayPartial(userId)
            val newCount = (existing?.observationCount ?: 0) + 1

            // Mayor Signo: keep the most fertile sign seen today
            val bestBleeding     = chooseMostFertileBleeding(existing?.bleedingLevel, bleeding?.code)
            val bestLubrication  = existing?.lubricationSensation ?: lubrication?.code
            val bestConsistency  = chooseMostFertileConsistency(existing?.mucusConsistency, consistency?.code)
            val bestColor        = existing?.mucusColor ?: color?.code
            val bestSensation    = if (bestLubrication != null || bestConsistency != null) null
            else chooseMostFertileSensation(existing?.sensation, sensation?.code)

            val cycleDay    = currentCycleRecords.value.size + 1
            val officialCode = buildPartialCode(bestBleeding, bestSensation, bestConsistency,
                bestColor, bestLubrication, newCount)
            val stampType    = CreightonLogic.computeStamp(
                bestBleeding?.let { c -> BleedingLevel.entries.find { it.code == c } },
                bestLubrication?.let { c -> LubricationSensation.entries.find { it.code == c } },
                bestConsistency?.let { c -> MucusConsistency.entries.find { it.code == c } },
                bestColor?.let { c -> MucusColor.entries.find { it.code == c } },
                bestSensation?.let { c -> Sensation.entries.find { it.code == c } },
                0
            )

            val partial = DailyRecord(
                id                   = existing?.id ?: 0,
                userId               = userId,
                isTestRecord         = isTestMode,
                date                 = System.currentTimeMillis(),
                cycleDay             = cycleDay,
                isPartial            = true,
                observationCount     = newCount,
                bleedingLevel        = bestBleeding,
                mucusConsistency     = bestConsistency,
                mucusColor           = bestColor,
                lubricationSensation = bestLubrication,
                sensation            = bestSensation,
                hasIntercourse       = hasIntercourse || (existing?.hasIntercourse == true),
                breastSelfExam       = breastSelfExam || (existing?.breastSelfExam == true),
                officialCode         = officialCode,
                stampType            = stampType.name
            )
            repository.insert(partial)
            _todayPartial.value = partial
        }
    }

    // =========================================================================
    // SAVE FINAL RECORD (end of day)
    // Deletes today's partials, saves final record, checks for Peak Day.
    // =========================================================================

    fun saveFinalRecord(
        bleeding: BleedingLevel?,
        sensation: Sensation?,
        consistency: MucusConsistency?,
        color: MucusColor?,
        lubrication: LubricationSensation?,
        isPeakDay: Boolean,
        hasIntercourse: Boolean,
        breastSelfExam: Boolean,
        observationCount: Int = 1
    ) {
        viewModelScope.launch {
            // Normal mode: one final record per day
            if (!isTestMode && repository.hasFinalRecordForToday(userId)) {
                _todayAlreadyRegistered.value = true
                return@launch
            }
            _todayAlreadyRegistered.value = false

            // Delete any partials for today
            repository.deleteTodayPartials(userId)
            _todayPartial.value = null

            val cycleDay      = currentCycleRecords.value.size + 1
            val postPeakCount = computePostPeakCount(currentCycleRecords.value)
            val officialCode  = CreightonLogic.buildOfficialCode(
                bleeding, sensation, consistency, color, lubrication,
                frequencyFromCount(observationCount), isPeakDay
            )
            val stampType = CreightonLogic.computeStamp(
                bleeding, lubrication, consistency, color, sensation, postPeakCount
            )

            val record = DailyRecord(
                userId               = userId,
                isTestRecord         = isTestMode,
                date                 = System.currentTimeMillis(),
                cycleDay             = cycleDay,
                isPartial            = false,
                observationCount     = observationCount,
                bleedingLevel        = bleeding?.code,
                mucusConsistency     = consistency?.code,
                mucusColor           = color?.code,
                lubricationSensation = lubrication?.code,
                sensation            = sensation?.code,
                isPeakDay            = isPeakDay,
                postPeakCount        = postPeakCount,
                hasIntercourse       = hasIntercourse,
                breastSelfExam       = breastSelfExam,
                officialCode         = officialCode,
                stampType            = stampType.name
            )
            repository.insert(record)

            // Trigger automatic notifications
            appContext?.let { ctx ->
                val prefs = ctx.getSharedPreferences("notif_settings", android.content.Context.MODE_PRIVATE)

                // Day 7: breast exam reminder
                if (prefs.getBoolean("breast_enabled", true) && cycleDay == 7) {
                    com.devdavinic.creightonapp.notifications.NotificationScheduler
                        .scheduleBreastExamToday(ctx)
                }

                // Fertile phase start: first mucus sign of cycle
                val wasNotFertile = currentCycleRecords.value.dropLast(1)
                    .none { it.stampType == StampType.WHITE_BABY.name }
                if (prefs.getBoolean("fertile_enabled", true) &&
                    stampType == StampType.WHITE_BABY && wasNotFertile) {
                    com.devdavinic.creightonapp.notifications.NotificationScheduler
                        .notifyFertilePhaseStart(ctx)
                    // Also notify partner
                    if (prefs.getBoolean("partner_enabled", true)) {
                        com.devdavinic.creightonapp.notifications.NotificationScheduler
                            .notifyPartner(ctx)
                    }
                }

                // P+3: double peak question
                if (prefs.getBoolean("double_peak_enabled", true) && postPeakCount == 3) {
                    com.devdavinic.creightonapp.notifications.NotificationScheduler
                        .scheduleDoublePeakQuestion(ctx)
                }
            }

            // Auto Peak Day detection
            checkAndSuggestPeakDay(record)
        }
    }

    // =========================================================================
    // PEAK DAY CONFIRMATION
    // Called when user confirms "Yes, yesterday was Peak Day"
    // =========================================================================

    fun confirmPeakDayYesterday() {
        viewModelScope.launch {
            val yesterday = repository.getYesterdayRecord(userId) ?: return@launch
            val updatedCode = CreightonLogic.buildOfficialCode(
                yesterday.bleedingLevel?.let { c -> BleedingLevel.entries.find { it.code == c } },
                yesterday.sensation?.let { c -> Sensation.entries.find { it.code == c } },
                yesterday.mucusConsistency?.let { c -> MucusConsistency.entries.find { it.code == c } },
                yesterday.mucusColor?.let { c -> MucusColor.entries.find { it.code == c } },
                yesterday.lubricationSensation?.let { c -> LubricationSensation.entries.find { it.code == c } },
                frequencyFromCount(yesterday.observationCount),
                isPeakDay = true
            )
            repository.updatePeakDay(yesterday.id, isPeak = true, code = updatedCode)
            repository.updatePeakSuggestion(yesterday.id, suggests = false)
            _peakDaySuggestion.value = null
        }
    }

    fun dismissPeakDaySuggestion() {
        viewModelScope.launch {
            _peakDaySuggestion.value?.let {
                repository.updatePeakSuggestion(it.id, suggests = false)
            }
            _peakDaySuggestion.value = null
        }
    }

    // =========================================================================
    // DELETE / CLEAR
    // =========================================================================

    // =========================================================================
    // SYMPTOMS
    // =========================================================================

    fun saveSymptoms(
        symptoms: Map<SymptomType, SymptomIntensity>,
        notes: String,
        dateEpoch: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val cycleDay = currentCycleRecords.value.size
            val hasUnusual = symptoms.keys.any {
                it.category == SymptomCategory.UNUSUAL_BLEEDING
            }
            val encoded = DailySymptom.encodeSymptoms(symptoms)
            val dayKey  = dateEpoch / 86_400_000L
            val existing = symptomRepository?.getSymptomForDay(userId, dayKey)
            symptomRepository?.insert(DailySymptom(
                id               = existing?.id ?: 0,
                userId           = userId,
                date             = dateEpoch,
                cycleDay         = cycleDay,
                symptomsEncoded  = encoded,
                notes            = notes,
                isUnusualBleeding = hasUnusual
            ))
        }
    }

    suspend fun getTodaySymptoms(): DailySymptom? {
        val dayKey = System.currentTimeMillis() / 86_400_000L
        return symptomRepository?.getSymptomForDay(userId, dayKey)
    }

    suspend fun getBiomarkerAlerts(): List<BiomarkerAlert> {
        val recent = symptomRepository?.getRecentSymptoms(userId, 14) ?: emptyList()
        return BiomarkerAnalyzer.analyze(recent, cycleAnalysis.value)
    }

    fun deleteRecord(record: DailyRecord) {
        viewModelScope.launch { repository.delete(record) }
    }

    /** Update an existing record — used for editing from Planilla */
    fun updateRecord(record: DailyRecord) {
        viewModelScope.launch { repository.insert(record) }
    }

    fun clearAllRecords() {
        viewModelScope.launch { repository.deleteAllForUser(userId) }
    }

    // =========================================================================
    // TEST MODE: SAVE RECORD ON SPECIFIC DATE
    // Only available in test mode — allows simulating past days
    // =========================================================================

    fun saveRecordOnDate(
        dateEpoch: Long,
        bleeding: BleedingLevel?,
        sensation: Sensation?,
        consistency: MucusConsistency?,
        color: MucusColor?,
        lubrication: LubricationSensation?,
        isPeakDay: Boolean,
        hasIntercourse: Boolean,
        breastSelfExam: Boolean,
        observationCount: Int = 1
    ) {
        if (!isTestMode) return
        viewModelScope.launch {
            // Compute cycleDay from existing records up to this date
            val recordsBefore = allRecordsAscending.value
                .filter { it.date < dateEpoch && !it.isPartial }
            val cycleDay = recordsBefore.size + 1
            val postPeakCount = computePostPeakCount(recordsBefore)
            val officialCode = CreightonLogic.buildOfficialCode(
                bleeding, sensation, consistency, color, lubrication,
                frequencyFromCount(observationCount), isPeakDay
            )
            val stampType = CreightonLogic.computeStamp(
                bleeding, lubrication, consistency, color, sensation, postPeakCount
            )
            repository.insert(DailyRecord(
                userId               = userId,
                isTestRecord         = true,
                date                 = dateEpoch,
                cycleDay             = cycleDay,
                isPartial            = false,
                observationCount     = observationCount,
                bleedingLevel        = bleeding?.code,
                mucusConsistency     = consistency?.code,
                mucusColor           = color?.code,
                lubricationSensation = lubrication?.code,
                sensation            = sensation?.code,
                isPeakDay            = isPeakDay,
                postPeakCount        = postPeakCount,
                hasIntercourse       = hasIntercourse,
                breastSelfExam       = breastSelfExam,
                officialCode         = officialCode,
                stampType            = stampType.name
            ))
        }
    }

    // =========================================================================
    // TEST MODE: GENERATE REALISTIC CYCLE DATA
    // Generates N complete cycles ending today with realistic Creighton patterns
    // =========================================================================

    fun generateTestCycles(numberOfCycles: Int = 3) {
        if (!isTestMode) return
        viewModelScope.launch {
            repository.deleteAllForUser(userId)

            val today      = System.currentTimeMillis()
            val dayMs      = 86_400_000L

            // Realistic pattern: 28-day cycle, Peak around day 15, post-Peak 13 days
            val cycleLength   = 28
            val peakDay       = 15
            val menstrualDays = 5

            // Generate cycles going backwards from today
            var currentDate = today

            repeat(numberOfCycles) { cycleNum ->
                val cycleStartDate = currentDate - (cycleLength * dayMs)
                // Normalize to midnight to avoid DST drift
                val startMidnight = java.util.Calendar.getInstance().apply {
                    timeInMillis = cycleStartDate
                    set(java.util.Calendar.HOUR_OF_DAY, 8)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis

                for (dayInCycle in 1..cycleLength) {
                    val recordDate = startMidnight + (dayInCycle - 1) * dayMs
                    if (recordDate > today) continue

                    val bleeding: BleedingLevel?
                    val sensation: Sensation?
                    val consistency: MucusConsistency?
                    val color: MucusColor?
                    val lubrication: LubricationSensation?
                    val isPeak: Boolean

                    when {
                        // Menstrual: days 1-5
                        dayInCycle <= menstrualDays -> {
                            bleeding    = when (dayInCycle) { 1 -> BleedingLevel.H; 2 -> BleedingLevel.H; 3 -> BleedingLevel.M; 4 -> BleedingLevel.L; else -> BleedingLevel.VL }
                            sensation   = null; consistency = null; color = null; lubrication = null; isPeak = false
                        }
                        // Dry days: 6-11
                        dayInCycle in 6..11 -> {
                            bleeding    = null; sensation = Sensation.DRY
                            consistency = null; color = null; lubrication = null; isPeak = false
                        }
                        // Building mucus: 12-13
                        dayInCycle in 12..13 -> {
                            bleeding    = null; sensation = Sensation.WET
                            consistency = MucusConsistency.STICKY; color = MucusColor.CLOUDY
                            lubrication = null; isPeak = false
                        }
                        // Pre-Peak mucus: 14
                        dayInCycle == 14 -> {
                            bleeding    = null; sensation = null
                            consistency = MucusConsistency.TACKY; color = MucusColor.CLOUDY_CLEAR
                            lubrication = null; isPeak = false
                        }
                        // Peak Day: day 15
                        dayInCycle == peakDay -> {
                            bleeding    = null; sensation = null
                            consistency = MucusConsistency.STRETCHY; color = MucusColor.CLEAR
                            lubrication = null; isPeak = true
                        }
                        // Post-Peak 1: still has some mucus
                        dayInCycle == peakDay + 1 -> {
                            bleeding    = null; sensation = Sensation.WET
                            consistency = MucusConsistency.STICKY; color = MucusColor.CLOUDY
                            lubrication = null; isPeak = false
                        }
                        // Post-Peak 2-3: drying
                        dayInCycle in (peakDay + 2)..(peakDay + 3) -> {
                            bleeding    = null; sensation = Sensation.DRY
                            consistency = null; color = null; lubrication = null; isPeak = false
                        }
                        // Post-Peak dry phase: days 18-28
                        else -> {
                            bleeding    = null; sensation = Sensation.DRY
                            consistency = null; color = null; lubrication = null; isPeak = false
                        }
                    }

                    val postPeakCount = if (dayInCycle in (peakDay + 1)..(peakDay + 3))
                        dayInCycle - peakDay else 0
                    val freq = ObservationFrequency.X1
                    val code = CreightonLogic.buildOfficialCode(
                        bleeding, sensation, consistency, color, lubrication, freq, isPeak)
                    val stamp = CreightonLogic.computeStamp(
                        bleeding, lubrication, consistency, color, sensation, postPeakCount)

                    repository.insert(DailyRecord(
                        userId               = userId,
                        isTestRecord         = true,
                        date                 = recordDate,
                        cycleDay             = dayInCycle,
                        isPartial            = false,
                        observationCount     = 1,
                        bleedingLevel        = bleeding?.code,
                        sensation            = sensation?.code,
                        mucusConsistency     = consistency?.code,
                        mucusColor           = color?.code,
                        lubricationSensation = lubrication?.code,
                        isPeakDay            = isPeak,
                        postPeakCount        = postPeakCount,
                        hasIntercourse       = false,
                        breastSelfExam       = false,
                        officialCode         = code,
                        stampType            = stamp.name
                    ))
                }
                currentDate = cycleStartDate
            }
        }
    }

    fun resetTodayAlreadyRegistered() {
        _todayAlreadyRegistered.value = false
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private suspend fun checkAndSuggestPeakDay(todayRecord: DailyRecord) {
        val yesterday = repository.getYesterdayRecord(userId) ?: return
        if (yesterday.isPeakDay) return  // already marked
        if (!yesterday.hasPeakTypeSign) return  // yesterday wasn't Peak-type

        // Today dropped below Peak-type → yesterday was likely Peak Day
        if (!todayRecord.hasPeakTypeSign) {
            repository.updatePeakSuggestion(yesterday.id, suggests = true)
            _peakDaySuggestion.value = yesterday
            // Fire notification
            appContext?.let { ctx ->
                val prefs = ctx.getSharedPreferences("notif_settings", android.content.Context.MODE_PRIVATE)
                if (prefs.getBoolean("peak_enabled", true)) {
                    com.devdavinic.creightonapp.notifications.NotificationScheduler
                        .notifyPeakDaySuggestion(ctx)
                }
            }
        }
    }

    private fun computePostPeakCount(cycleRecords: List<DailyRecord>): Int {
        if (cycleRecords.isEmpty()) return 0
        val lastPeakIndex = cycleRecords.indexOfLast { it.isPeakDay }
        if (lastPeakIndex == -1) return 0
        val daysSincePeak = cycleRecords.size - lastPeakIndex
        return if (daysSincePeak in 1..3) daysSincePeak else 0
    }

    private fun groupAllCycles(records: List<DailyRecord>): List<List<DailyRecord>> {
        if (records.isEmpty()) return emptyList()
        val sorted  = records.filter { !it.isPartial }.sortedBy { it.date }
        val cycles  = mutableListOf<MutableList<DailyRecord>>()
        var current = mutableListOf<DailyRecord>()

        for (i in sorted.indices) {
            val record      = sorted[i]
            val isMenstrual = record.bleedingLevel == "H" || record.bleedingLevel == "M"

            if (isMenstrual && current.isNotEmpty()) {
                val prev      = sorted[i - 1]
                val dayGap    = (record.date - prev.date) / 86_400_000L

                // Count consecutive menstrual days at the END of current cycle
                var consecMenst = 0
                for (j in current.indices.reversed()) {
                    val r = current[j]
                    if (r.bleedingLevel == "H" || r.bleedingLevel == "M") consecMenst++
                    else break
                }

                // Start new cycle ONLY when:
                // 1. There is NO consecutive menstrual run at end of current cycle
                //    (meaning we had non-menstrual days since last H/M)
                //    AND the current cycle is at least 15 days long
                // 2. OR there is a date gap > 5 days (missing data)
                val isContinuousMenstrual = consecMenst > 0 && dayGap <= 2
                val isNewCycle = !isContinuousMenstrual && current.size >= 15
                val hasBigGap  = dayGap > 5

                if (isNewCycle || hasBigGap) {
                    cycles.add(current)
                    current = mutableListOf()
                }
            }
            current.add(record)
        }
        if (current.isNotEmpty()) cycles.add(current)
        return cycles
    }

    // Mayor Signo helpers — return the most fertile of two codes
    private fun chooseMostFertileBleeding(existing: String?, new: String?): String? {
        val order = listOf("H", "M", "L", "VL", "B")
        val existingIdx = existing?.let { order.indexOf(it) } ?: Int.MAX_VALUE
        val newIdx      = new?.let { order.indexOf(it) } ?: Int.MAX_VALUE
        return if (existingIdx <= newIdx) existing else new
    }

    private fun chooseMostFertileConsistency(existing: String?, new: String?): String? {
        val order = listOf("10", "8", "6")
        val existingIdx = existing?.let { order.indexOf(it) } ?: Int.MAX_VALUE
        val newIdx      = new?.let { order.indexOf(it) } ?: Int.MAX_VALUE
        return if (existingIdx <= newIdx) existing else new
    }

    private fun chooseMostFertileSensation(existing: String?, new: String?): String? {
        val order = listOf("4", "2W", "2", "0")
        val existingIdx = existing?.let { order.indexOf(it) } ?: Int.MAX_VALUE
        val newIdx      = new?.let { order.indexOf(it) } ?: Int.MAX_VALUE
        return if (existingIdx <= newIdx) existing else new
    }

    private fun buildPartialCode(
        bleeding: String?, sensation: String?, consistency: String?,
        color: String?, lubrication: String?, count: Int
    ): String {
        val freq = frequencyFromCount(count)
        val b = bleeding?.let { BleedingLevel.entries.find { e -> e.code == it } }
        val s = sensation?.let { Sensation.entries.find { e -> e.code == it } }
        val c = consistency?.let { MucusConsistency.entries.find { e -> e.code == it } }
        val mc = color?.let { MucusColor.entries.find { e -> e.code == it } }
        val l = lubrication?.let { LubricationSensation.entries.find { e -> e.code == it } }
        return CreightonLogic.buildOfficialCode(b, s, c, mc, l, freq, false)
    }

    private fun frequencyFromCount(count: Int): ObservationFrequency = when {
        count >= 4 -> ObservationFrequency.AD
        count == 3 -> ObservationFrequency.X3
        count == 2 -> ObservationFrequency.X2
        else       -> ObservationFrequency.X1
    }
}

// =============================================================================
// FACTORY
// =============================================================================

class MainViewModelFactory(
    private val repository: RecordRepository,
    private val symptomRepository: SymptomRepository? = null,
    private val userId: String,
    private val isTestMode: Boolean = false,
    private val appContext: android.content.Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, symptomRepository, userId, isTestMode, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}