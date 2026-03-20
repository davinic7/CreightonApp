package com.devdavinic.creightonapp.data

import androidx.room.*
import com.devdavinic.creightonapp.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getProfile(uid: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    fun observeProfile(uid: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE partnerLinkCode = :code LIMIT 1")
    suspend fun getProfileByLinkCode(code: String): UserProfile?

    @Query("UPDATE user_profiles SET partnerUid = :partnerUid, updatedAt = :now WHERE uid = :uid")
    suspend fun updatePartnerLink(uid: String, partnerUid: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profiles SET city = :city, country = :country, updatedAt = :now WHERE uid = :uid")
    suspend fun updateLocation(uid: String, city: String, country: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profiles SET pinHash = :pinHash, updatedAt = :now WHERE uid = :uid")
    suspend fun updatePin(uid: String, pinHash: String?, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProfile(profile: UserProfile)
}