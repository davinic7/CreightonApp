package com.devdavinic.creightonapp.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// =============================================================================
// DAILY RECORD - v2
// Supports partial observations during the day.
// At end of day, the HIGHEST fertility sign is confirmed as the final record.
// Observation count drives automatic X1/X2/X3/AD frequency.
// =============================================================================

@Entity(
    tableName = "daily_records",
    indices   = [Index(value = ["userId", "date"])]
)
data class DailyRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Owner
    val userId: String = "",
    val isTestRecord: Boolean = false,

    // Date (epoch ms of the day)
    val date: Long,
    val cycleDay: Int,

    // ── Partial observation support ───────────────────────────────────────────
    // true  = draft saved during the day, not yet confirmed
    // false = final end-of-day record
    val isPartial: Boolean = false,

    // How many times the BEST sign was observed during the day
    // Drives automatic frequency: 1=X1, 2=X2, 3=X3, 4+=AD
    val observationCount: Int = 1,

    // ── Bleeding ──────────────────────────────────────────────────────────────
    val bleedingLevel: String? = null,

    // ── Mucus observation (exclusive of sensation) ────────────────────────────
    // If muco is present, sensation is NOT recorded separately
    val mucusConsistency: String? = null,
    val mucusColor: String? = null,

    // ── Lubrication (always Peak-type, exclusive of other sensation/mucus) ────
    val lubricationSensation: String? = null,

    // ── Sensation WITHOUT mucus (only when no mucus/lubrication visible) ──────
    val sensation: String? = null,

    // ── Markers ───────────────────────────────────────────────────────────────
    // isPeakDay: set automatically by the system when the NEXT day shows
    // lower-quality mucus, then confirmed by user. Can also be set manually.
    val isPeakDay: Boolean = false,

    // System suggestion: "yesterday may have been Peak Day"
    // Set true when today's sign dropped from Peak-type to lower
    val systemSuggestsPeakYesterday: Boolean = false,

    val postPeakCount: Int = 0,
    val hasIntercourse: Boolean = false,
    val breastSelfExam: Boolean = false,

    // ── Generated fields ──────────────────────────────────────────────────────
    // observationFrequency is now DERIVED from observationCount, not stored
    val officialCode: String = "",
    val stampType: String = "GREEN_SOLID"
) {
    // Derived frequency — computed from observationCount
    val observationFrequency: String get() = when {
        observationCount >= 4 -> "AD"
        observationCount == 3 -> "X3"
        observationCount == 2 -> "X2"
        else                  -> "X1"
    }

    // Whether this record has a Peak-type sign
    val hasPeakTypeSign: Boolean get() =
        lubricationSensation != null ||
                mucusConsistency == MucusConsistency.STRETCHY.code ||
                mucusColor == MucusColor.CLEAR.code ||
                mucusColor == MucusColor.CLOUDY_CLEAR.code
}