package com.devdavinic.creightonapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// =============================================================================
// USER PROFILE MODEL
// Stored locally in Room, synced key fields to Firestore
// =============================================================================

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val uid: String,                        // Firebase Auth UID

    val email: String,
    val displayName: String,

    // Profile customization
    val avatarColor: String = "#059669",    // hex color for avatar
    val photoUri: String? = null,           // local file URI for profile photo

    // Profile type
    val profileType: String = ProfileType.FEMININE.name,

    // Test mode - no restrictions on daily records
    val isTestMode: Boolean = false,

    // Partner linking
    val partnerUid: String? = null,         // linked partner's Firebase UID
    val partnerLinkCode: String? = null,    // 6-char code to link with partner

    // Location (city only, for future specialist suggestions)
    val city: String? = null,
    val country: String? = null,

    // PIN protection (stored as hash)
    val pinHash: String? = null,

    // Profile photo — local file path saved after camera/gallery pick
    val avatarPhotoPath: String? = null,

    // Personal data
    val dateOfBirth: Long? = null,          // epoch ms
    val biologicalSex: String? = null,      // for medical context

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Computed age from dateOfBirth
    val age: Int? get() {
        val dob = dateOfBirth ?: return null
        val now = java.util.Calendar.getInstance()
        val birth = java.util.Calendar.getInstance().apply { timeInMillis = dob }
        var age = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
        if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) age--
        return age
    }
}

enum class ProfileType(val label: String, val description: String) {
    FEMININE("Perfil femenino",    "Registro de ciclo y todos los modulos"),
    MASCULINE("Perfil masculino",  "Vista de la pareja vinculada"),
    TEST("Perfil de testeo",       "Sin restricciones de registro, acceso a ambas vistas")
}

// Avatar color options
object AvatarColors {
    val options = listOf(
        "#059669",  // emerald
        "#2563EB",  // blue
        "#7C3AED",  // purple
        "#DB2777",  // pink
        "#D97706",  // amber
        "#DC2626",  // red
        "#0891B2",  // cyan
        "#65A30D"   // lime
    )
}