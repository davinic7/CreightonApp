package com.devdavinic.creightonapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devdavinic.creightonapp.data.RecordRepository
import com.devdavinic.creightonapp.data.UserProfileDao
import com.devdavinic.creightonapp.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

// =============================================================================
// AUTH VIEWMODEL
// =============================================================================

sealed class AuthState {
    object Loading   : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val user: FirebaseUser, val profile: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val profileDao: UserProfileDao,
    private val recordRepository: RecordRepository? = null
) : ViewModel() {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState      = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    // Partner profile — loaded when masculine profile is logged in
    private val _partnerProfile = MutableStateFlow<UserProfile?>(null)
    val partnerProfile: StateFlow<UserProfile?> = _partnerProfile.asStateFlow()

    // Partner's cycle records (read-only, loaded from Firestore)
    private val _partnerRecords = MutableStateFlow<List<DailyRecord>>(emptyList())
    val partnerRecords: StateFlow<List<DailyRecord>> = _partnerRecords.asStateFlow()

    // Link status message
    private val _linkMessage = MutableStateFlow<String?>(null)
    val linkMessage: StateFlow<String?> = _linkMessage.asStateFlow()

    init { checkCurrentUser() }

    // =========================================================================
    // AUTH
    // =========================================================================

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _authState.value = AuthState.LoggedOut
            } else {
                val profile = loadOrCreateProfile(user)
                _currentProfile.value = profile
                _authState.value = AuthState.LoggedIn(user, profile)
                loadPartnerDataIfNeeded(profile)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result  = auth.signInWithEmailAndPassword(email, password).await()
                val user    = result.user ?: throw Exception("Usuario no encontrado")
                val profile = loadOrCreateProfile(user)
                _currentProfile.value = profile
                _authState.value = AuthState.LoggedIn(user, profile)
                loadPartnerDataIfNeeded(profile)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(parseAuthError(e.message))
            }
        }
    }

    fun register(
        email: String, password: String, displayName: String,
        profileType: ProfileType, avatarColor: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result   = auth.createUserWithEmailAndPassword(email, password).await()
                val user     = result.user ?: throw Exception("Error al crear usuario")
                val linkCode = generateLinkCode()
                val profile  = UserProfile(
                    uid             = user.uid,
                    email           = email,
                    displayName     = displayName,
                    profileType     = profileType.name,
                    avatarColor     = avatarColor,
                    isTestMode      = profileType == ProfileType.TEST,
                    partnerLinkCode = linkCode
                )
                profileDao.upsertProfile(profile)
                syncProfileToFirestore(profile)
                _currentProfile.value = profile
                _authState.value = AuthState.LoggedIn(user, profile)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(parseAuthError(e.message))
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentProfile.value  = null
        _partnerProfile.value  = null
        _partnerRecords.value  = emptyList()
        _authState.value       = AuthState.LoggedOut
    }

    // =========================================================================
    // PROFILE UPDATES
    // =========================================================================

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            val updated = profile.copy(updatedAt = System.currentTimeMillis())
            profileDao.upsertProfile(updated)
            syncProfileToFirestore(updated)
            _currentProfile.value = updated
            val user = auth.currentUser
            if (user != null) _authState.value = AuthState.LoggedIn(user, updated)
        }
    }

    fun updateLocation(city: String, country: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            profileDao.updateLocation(uid, city, country)
            _currentProfile.value = _currentProfile.value?.copy(city = city, country = country)
        }
    }

    fun updatePin(pin: String?) {
        viewModelScope.launch {
            val uid  = auth.currentUser?.uid ?: return@launch
            val hash = pin?.let { hashPin(it) }
            profileDao.updatePin(uid, hash)
            _currentProfile.value = _currentProfile.value?.copy(pinHash = hash)
        }
    }

    fun verifyPin(pin: String): Boolean {
        val stored = _currentProfile.value?.pinHash ?: return true
        return hashPin(pin) == stored
    }

    // =========================================================================
    // PARTNER LINKING — fixed to try local DB first, then Firestore
    // =========================================================================

    fun linkWithPartner(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val myUid = auth.currentUser?.uid ?: return@launch
            val clean = code.uppercase().trim()

            // 1. Try local DB first (works offline)
            val localPartner = profileDao.getProfileByLinkCode(clean)
            if (localPartner != null && localPartner.uid != myUid) {
                performLink(myUid, localPartner.uid, onResult)
                return@launch
            }

            // 2. Try Firestore
            try {
                val snap = firestore.collection("profiles")
                    .whereEqualTo("partnerLinkCode", clean)
                    .limit(1)
                    .get()
                    .await()

                if (snap.isEmpty) {
                    onResult(false, "Codigo no encontrado. Asegurate de que tu pareja haya iniciado sesion al menos una vez.")
                    return@launch
                }

                val partnerUid = snap.documents.first().id
                if (partnerUid == myUid) {
                    onResult(false, "No puedes vincularte contigo mismo.")
                    return@launch
                }

                performLink(myUid, partnerUid, onResult)

            } catch (e: Exception) {
                onResult(false, "Sin conexion. Asegurate de que tu pareja este en la misma red o haya sincronizado antes.")
            }
        }
    }

    private suspend fun performLink(
        myUid: String, partnerUid: String,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            // Update my profile
            profileDao.updatePartnerLink(myUid, partnerUid)
            _currentProfile.value = _currentProfile.value?.copy(partnerUid = partnerUid)

            // Update Firestore
            firestore.collection("profiles").document(myUid)
                .update("partnerUid", partnerUid).await()

            // Also update partner's Firestore to point back to me
            firestore.collection("profiles").document(partnerUid)
                .update("partnerUid", myUid).await()

            // Load partner profile
            val partnerProfile = loadPartnerProfile(partnerUid)
            _partnerProfile.value = partnerProfile

            onResult(true, "Vinculacion exitosa con ${partnerProfile?.displayName ?: "tu pareja"}.")
            _linkMessage.value = null
        } catch (e: Exception) {
            onResult(false, "Error al vincular: ${e.message}")
        }
    }

    fun unlinkPartner(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val myUid = auth.currentUser?.uid ?: return@launch
            try {
                val partnerUid = _currentProfile.value?.partnerUid

                // Remove link from my profile
                profileDao.updatePartnerLink(myUid, "")
                _currentProfile.value = _currentProfile.value?.copy(partnerUid = null)

                // Remove from Firestore
                firestore.collection("profiles").document(myUid)
                    .update("partnerUid", null).await()

                // Remove back-link from partner if possible
                partnerUid?.let { pUid ->
                    try {
                        firestore.collection("profiles").document(pUid)
                            .update("partnerUid", null).await()
                    } catch (_: Exception) {}
                }

                _partnerProfile.value = null
                _partnerRecords.value = emptyList()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    // =========================================================================
    // PARTNER DATA ACCESS
    // =========================================================================

    private suspend fun loadPartnerDataIfNeeded(profile: UserProfile) {
        val partnerUid = profile.partnerUid ?: return
        val partnerProfile = loadPartnerProfile(partnerUid)
        _partnerProfile.value = partnerProfile
    }

    private suspend fun loadPartnerProfile(partnerUid: String): UserProfile? {
        // Try local
        val local = profileDao.getProfile(partnerUid)
        if (local != null) return local

        // Try Firestore
        return try {
            val doc = firestore.collection("profiles").document(partnerUid).get().await()
            if (doc.exists()) {
                val profile = UserProfile(
                    uid             = partnerUid,
                    email           = doc.getString("email") ?: "",
                    displayName     = doc.getString("displayName") ?: "Pareja",
                    avatarColor     = doc.getString("avatarColor") ?: "#059669",
                    profileType     = doc.getString("profileType") ?: "FEMININE",
                    isTestMode      = doc.getBoolean("isTestMode") ?: false,
                    partnerUid      = doc.getString("partnerUid"),
                    partnerLinkCode = doc.getString("partnerLinkCode"),
                    city            = doc.getString("city"),
                    country         = doc.getString("country"),
                    createdAt       = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt       = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                profileDao.upsertProfile(profile)
                profile
            } else null
        } catch (e: Exception) { null }
    }

    /** Register intercourse from partner's profile — saves to partner's record */
    fun registerIntercourseForPartner(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val partnerUid = _currentProfile.value?.partnerUid
            if (partnerUid == null) {
                onResult(false)
                return@launch
            }
            // Push intercourse event to Firestore for partner's app to pick up
            try {
                val dayKey = System.currentTimeMillis() / 86_400_000L
                firestore.collection("partner_events")
                    .document("${partnerUid}_${dayKey}")
                    .set(mapOf(
                        "type"       to "INTERCOURSE",
                        "fromUid"    to (auth.currentUser?.uid ?: ""),
                        "toUid"      to partnerUid,
                        "date"       to System.currentTimeMillis(),
                        "processed"  to false
                    )).await()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /** Check and process pending partner events (intercourse I marker) */
    fun processPendingPartnerEvents(repository: RecordRepository) {
        viewModelScope.launch {
            val myUid = auth.currentUser?.uid ?: return@launch
            try {
                val dayKey = System.currentTimeMillis() / 86_400_000L
                val events = firestore.collection("partner_events")
                    .whereEqualTo("toUid", myUid)
                    .whereEqualTo("processed", false)
                    .whereEqualTo("type", "INTERCOURSE")
                    .get().await()

                events.documents.forEach { doc ->
                    // Mark as processed — actual I marking handled by MainViewModel on next load
                    firestore.collection("partner_events")
                        .document(doc.id).update("processed", true).await()
                }
            } catch (_: Exception) {}
        }
    }

    fun clearLinkMessage() { _linkMessage.value = null }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private suspend fun loadOrCreateProfile(user: FirebaseUser): UserProfile {
        val local = profileDao.getProfile(user.uid)
        if (local != null) return local

        return try {
            val doc = firestore.collection("profiles").document(user.uid).get().await()
            if (doc.exists()) {
                val profile = UserProfile(
                    uid             = user.uid,
                    email           = doc.getString("email") ?: user.email ?: "",
                    displayName     = doc.getString("displayName") ?: user.displayName ?: "Usuario",
                    avatarColor     = doc.getString("avatarColor") ?: "#059669",
                    profileType     = doc.getString("profileType") ?: "FEMININE",
                    isTestMode      = doc.getBoolean("isTestMode") ?: false,
                    partnerUid      = doc.getString("partnerUid"),
                    partnerLinkCode = doc.getString("partnerLinkCode"),
                    city            = doc.getString("city"),
                    country         = doc.getString("country"),
                    createdAt       = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt       = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                profileDao.upsertProfile(profile)
                profile
            } else {
                val linkCode = generateLinkCode()
                val profile  = UserProfile(
                    uid             = user.uid,
                    email           = user.email ?: "",
                    displayName     = user.displayName ?: "Usuario",
                    partnerLinkCode = linkCode
                )
                profileDao.upsertProfile(profile)
                syncProfileToFirestore(profile)
                profile
            }
        } catch (e: Exception) {
            val profile = UserProfile(
                uid         = user.uid,
                email       = user.email ?: "",
                displayName = user.displayName ?: "Usuario"
            )
            profileDao.upsertProfile(profile)
            profile
        }
    }

    private fun syncProfileToFirestore(profile: UserProfile) {
        val data = hashMapOf(
            "email"           to profile.email,
            "displayName"     to profile.displayName,
            "avatarColor"     to profile.avatarColor,
            "profileType"     to profile.profileType,
            "isTestMode"      to profile.isTestMode,
            "partnerUid"      to profile.partnerUid,
            "partnerLinkCode" to profile.partnerLinkCode,
            "city"            to profile.city,
            "country"         to profile.country,
            "createdAt"       to profile.createdAt,
            "updatedAt"       to profile.updatedAt
        )
        firestore.collection("profiles").document(profile.uid).set(data)
    }

    private fun generateLinkCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun hashPin(pin: String): String {
        var hash = 0
        pin.forEach { hash = hash * 31 + it.code }
        return hash.toString()
    }

    private fun parseAuthError(message: String?): String = when {
        message == null                           -> "Error desconocido"
        "password" in message                     -> "Contrasena incorrecta"
        "email" in message && "format" in message -> "Email invalido"
        "no user" in message.lowercase()          -> "No existe una cuenta con ese email"
        "already in use" in message               -> "Ese email ya esta registrado"
        "weak password" in message                -> "La contrasena debe tener al menos 6 caracteres"
        "network" in message.lowercase()          -> "Sin conexion. Verifica tu internet"
        else                                      -> message
    }
}

// =============================================================================
// FACTORY
// =============================================================================

class AuthViewModelFactory(
    private val profileDao: UserProfileDao,
    private val recordRepository: RecordRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(profileDao, recordRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}