package com.devdavinic.creightonapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devdavinic.creightonapp.model.DailyRecord
import com.devdavinic.creightonapp.model.DailySymptom
import com.devdavinic.creightonapp.model.UserProfile

@Database(
    entities  = [DailyRecord::class, UserProfile::class, DailySymptom::class],
    version   = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun symptomDao(): SymptomDao

    companion object {

        // v1 -> v2: recreate table with Creighton columns
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL, cycleDay INTEGER NOT NULL,
                        bleedingLevel TEXT, sensation TEXT, mucusConsistency TEXT,
                        mucusColor TEXT, lubricationSensation TEXT, observationFrequency TEXT,
                        isPeakDay INTEGER NOT NULL DEFAULT 0, postPeakCount INTEGER NOT NULL DEFAULT 0,
                        hasIntercourse INTEGER NOT NULL DEFAULT 0, breastSelfExam INTEGER NOT NULL DEFAULT 0,
                        officialCode TEXT NOT NULL DEFAULT '', stampType TEXT NOT NULL DEFAULT 'GREEN_SOLID'
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT OR IGNORE INTO daily_records_new
                        (id, date, cycleDay, isPeakDay, hasIntercourse, officialCode, stampType)
                    SELECT id, date, COALESCE(cycleDay,1), isPeakDay, hasIntercourse, officialCode, 'GREEN_SOLID'
                    FROM daily_records
                """.trimIndent())
                database.execSQL("DROP TABLE daily_records")
                database.execSQL("ALTER TABLE daily_records_new RENAME TO daily_records")
            }
        }

        // v2 -> v3: add userId, isTestRecord, user_profiles table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_records ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE daily_records ADD COLUMN isTestRecord INTEGER NOT NULL DEFAULT 0")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profiles (
                        uid TEXT PRIMARY KEY NOT NULL, email TEXT NOT NULL, displayName TEXT NOT NULL,
                        avatarColor TEXT NOT NULL DEFAULT '#059669', photoUri TEXT,
                        profileType TEXT NOT NULL DEFAULT 'FEMININE', isTestMode INTEGER NOT NULL DEFAULT 0,
                        partnerUid TEXT, partnerLinkCode TEXT, city TEXT, country TEXT, pinHash TEXT,
                        createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // v3 -> v4: add partial observation support to daily_records
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add partial support columns
                database.execSQL("ALTER TABLE daily_records ADD COLUMN isPartial INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE daily_records ADD COLUMN observationCount INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE daily_records ADD COLUMN systemSuggestsPeakYesterday INTEGER NOT NULL DEFAULT 0")

                // observationFrequency is now derived from observationCount
                // Migrate existing frequency values to observationCount
                database.execSQL("""
                    UPDATE daily_records SET observationCount = CASE
                        WHEN observationFrequency = 'AD' THEN 4
                        WHEN observationFrequency = 'X3' THEN 3
                        WHEN observationFrequency = 'X2' THEN 2
                        ELSE 1
                    END
                """.trimIndent())

                // Drop old observationFrequency column by recreating table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_records_v4 (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        isTestRecord INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL,
                        cycleDay INTEGER NOT NULL,
                        isPartial INTEGER NOT NULL DEFAULT 0,
                        observationCount INTEGER NOT NULL DEFAULT 1,
                        bleedingLevel TEXT,
                        mucusConsistency TEXT,
                        mucusColor TEXT,
                        lubricationSensation TEXT,
                        sensation TEXT,
                        isPeakDay INTEGER NOT NULL DEFAULT 0,
                        systemSuggestsPeakYesterday INTEGER NOT NULL DEFAULT 0,
                        postPeakCount INTEGER NOT NULL DEFAULT 0,
                        hasIntercourse INTEGER NOT NULL DEFAULT 0,
                        breastSelfExam INTEGER NOT NULL DEFAULT 0,
                        officialCode TEXT NOT NULL DEFAULT '',
                        stampType TEXT NOT NULL DEFAULT 'GREEN_SOLID'
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO daily_records_v4
                        (id, userId, isTestRecord, date, cycleDay, isPartial, observationCount,
                         bleedingLevel, mucusConsistency, mucusColor, lubricationSensation,
                         sensation, isPeakDay, systemSuggestsPeakYesterday, postPeakCount,
                         hasIntercourse, breastSelfExam, officialCode, stampType)
                    SELECT id, userId, isTestRecord, date, cycleDay, isPartial, observationCount,
                           bleedingLevel, mucusConsistency, mucusColor, lubricationSensation,
                           sensation, isPeakDay, systemSuggestsPeakYesterday, postPeakCount,
                           hasIntercourse, breastSelfExam, officialCode, stampType
                    FROM daily_records
                """.trimIndent())
                database.execSQL("DROP TABLE daily_records")
                database.execSQL("ALTER TABLE daily_records_v4 RENAME TO daily_records")
            }
        }

        // v5 -> v6: add avatarPhotoPath, dateOfBirth, biologicalSex to user_profiles
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN avatarPhotoPath TEXT")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN dateOfBirth INTEGER")
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN biologicalSex TEXT")
            }
        }

        // v4 -> v5: add daily_symptoms table
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_symptoms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL DEFAULT '',
                        date INTEGER NOT NULL,
                        cycleDay INTEGER NOT NULL,
                        symptomsEncoded TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        isUnusualBleeding INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_daily_symptoms_userId_date ON daily_symptoms(userId, date)"
                )
            }
        }
    }
}