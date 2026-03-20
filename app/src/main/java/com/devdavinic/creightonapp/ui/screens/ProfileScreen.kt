package com.devdavinic.creightonapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.devdavinic.creightonapp.model.AvatarColors
import com.devdavinic.creightonapp.model.ProfileType
import com.devdavinic.creightonapp.model.UserProfile
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.AuthViewModel
import com.devdavinic.creightonapp.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// =============================================================================
// PROFILE SCREEN v2
// Photo avatar, GPS city, date of birth, skin selector
//
// Requires in build.gradle.kts:
//   implementation("io.coil-kt:coil-compose:2.6.0")          // image loading
//   implementation("com.google.android.gms:play-services-location:21.2.0") // GPS
// =============================================================================

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
    currentSkin: AppSkin = AppSkin.EMERALD,
    onSkinChange: (AppSkin) -> Unit = {},
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val profile by authViewModel.currentProfile.collectAsState()

    var showLinkDialog     by remember { mutableStateOf(false) }
    var showPinDialog      by remember { mutableStateOf(false) }
    var showClearDialog    by remember { mutableStateOf(false) }
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var showDobDialog      by remember { mutableStateOf(false) }
    var showPhotoDialog    by remember { mutableStateOf(false) }
    var editName           by remember { mutableStateOf(false) }
    var nameValue          by remember { mutableStateOf(profile?.displayName ?: "") }
    var selectedColor      by remember { mutableStateOf(profile?.avatarColor ?: AvatarColors.options.first()) }
    var gpsLoading         by remember { mutableStateOf(false) }
    var gpsError           by remember { mutableStateOf<String?>(null) }

    // Camera URI for photo capture
    val photoFile = remember { File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg") }
    val photoUri  = remember(photoFile) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && profile != null) {
            authViewModel.updateProfile(profile!!.copy(avatarPhotoPath = photoFile.absolutePath))
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Copy to local cache for persistence
            val destFile = File(context.filesDir, "avatar_${profile?.uid}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (profile != null) {
                authViewModel.updateProfile(profile!!.copy(avatarPhotoPath = destFile.absolutePath))
            }
        }
    }

    // Camera permission
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(photoUri)
    }

    // Location permission
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                gpsLoading = true
                gpsError   = null
                try {
                    val loc = LocationServices.getFusedLocationProviderClient(context)
                        .lastLocation.await()
                    if (loc != null) {
                        val geo = Geocoder(context, Locale("es"))
                        @Suppress("DEPRECATION")
                        val addresses = geo.getFromLocation(loc.latitude, loc.longitude, 1)
                        val city    = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea
                        val country = addresses?.firstOrNull()?.countryName
                        if (city != null && profile != null) {
                            authViewModel.updateLocation(city, country ?: "")
                        } else {
                            gpsError = "No se pudo determinar la ciudad. Intenta manualmente."
                        }
                    } else {
                        gpsError = "Ubicacion no disponible. Activa el GPS y vuelve a intentar."
                    }
                } catch (e: Exception) {
                    gpsError = "Error al obtener ubicacion: ${e.message}"
                } finally {
                    gpsLoading = false
                }
            }
        } else {
            gpsError = "Permiso de ubicacion denegado."
        }
    }

    fun requestGps() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            scope.launch {
                gpsLoading = true; gpsError = null
                try {
                    val loc = LocationServices.getFusedLocationProviderClient(context)
                        .lastLocation.await()
                    if (loc != null) {
                        val geo = Geocoder(context, Locale("es"))
                        @Suppress("DEPRECATION")
                        val addresses = geo.getFromLocation(loc.latitude, loc.longitude, 1)
                        val city    = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea
                        val country = addresses?.firstOrNull()?.countryName
                        if (city != null && profile != null) {
                            authViewModel.updateLocation(city, country ?: "")
                        } else gpsError = "No se pudo determinar la ciudad."
                    } else gpsError = "GPS no disponible."
                } catch (e: Exception) { gpsError = "Error: ${e.message}" }
                finally { gpsLoading = false }
            }
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(currentSkin.gradient()))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Atras",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("Mi Perfil", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                profile?.let { p ->

                    // ── Avatar card ───────────────────────────────────────────
                    AvatarCard(
                        profile       = p,
                        editName      = editName,
                        nameValue     = nameValue,
                        selectedColor = selectedColor,
                        onPhotoClick  = { showPhotoDialog = true },
                        onEditName    = { editName = true },
                        onNameChange  = { nameValue = it },
                        onSaveName    = {
                            authViewModel.updateProfile(p.copy(displayName = nameValue))
                            editName = false
                        },
                        onColorChange = { hex ->
                            selectedColor = hex
                            authViewModel.updateProfile(p.copy(avatarColor = hex))
                        }
                    )

                    // ── Personal data ─────────────────────────────────────────
                    SectionCard("Datos personales") {
                        ProfileRow(Icons.Outlined.Email, "Email", p.email)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                        // Date of birth
                        ProfileActionRow(
                            icon  = Icons.Outlined.Cake,
                            label = "Fecha de nacimiento",
                            value = p.dateOfBirth?.let {
                                SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es"))
                                    .format(Date(it))
                            } ?: "No configurada",
                            note  = p.age?.let { "Edad: $it anos" }
                        ) { showDobDialog = true }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                        // City with GPS
                        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.LocationOn, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ciudad", fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (p.city != null) "${p.city}, ${p.country ?: ""}"
                                        else "No configurada",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (gpsLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = { requestGps() },
                                        modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Outlined.MyLocation, "Usar GPS",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            gpsError?.let { err ->
                                Text(err, fontSize = 10.sp, color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(start = 30.dp, top = 2.dp))
                            }
                        }
                    }

                    // ── Security ──────────────────────────────────────────────
                    SectionCard("Cuenta") {
                        ProfileActionRow(Icons.Outlined.Pin, "PIN de seguridad",
                            if (p.pinHash != null) "Configurado" else "Sin PIN") {
                            showPinDialog = true
                        }
                    }

                    // ── Partner linking ───────────────────────────────────────
                    SectionCard("Vinculacion con la pareja") {
                        if (p.partnerUid != null) {
                            ProfileRow(Icons.Outlined.Link, "Pareja vinculada",
                                "Vinculada correctamente", valueColor = Emerald600)
                        } else {
                            ProfileRow(Icons.Outlined.QrCode, "Tu codigo de vinculacion",
                                p.partnerLinkCode ?: "---",
                                valueColor = MaterialTheme.colorScheme.primary,
                                note = "Compartilo con tu pareja para vincular las cuentas")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                            ProfileActionRow(Icons.Outlined.LinkOff, "Ingresar codigo de pareja",
                                "Vincular con tu pareja") { showLinkDialog = true }
                        }
                    }

                    // ── Test mode ─────────────────────────────────────────────
                    if (p.isTestMode || p.profileType == ProfileType.TEST.name) {
                        SectionCard("Modo Testeo") {
                            Row(modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.BugReport, null,
                                    tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Modo testeo activo", fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp, color = Color(0xFF92400E))
                                    Text("Sin restriccion de un registro por dia.",
                                        fontSize = 11.sp, color = Color(0xFFD97706))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFFBBF24).copy(alpha = 0.4f))
                            ProfileActionRow(Icons.Outlined.DeleteSweep, "Limpiar registros de prueba",
                                "Eliminar todos los registros de este perfil",
                                isDestructive = true) { showClearDialog = true }
                        }
                    }

                    // ── Skin selector ─────────────────────────────────────────
                    SectionCard("Tema visual") {
                        Text("Tema actual: ${currentSkin.emoji} ${currentSkin.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppSkin.entries.forEach { skin ->
                                val isSel = skin == currentSkin
                                Column(modifier = Modifier.width(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(skin.gradient()))
                                    .border(if (isSel) 2.5.dp else 1.dp,
                                        if (isSel) skin.accent else Color.White.copy(0.5f),
                                        RoundedCornerShape(12.dp))
                                    .clickable { onSkinChange(skin) }
                                    .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(skin.emoji, fontSize = 20.sp)
                                    Text(skin.displayName, fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = skin.accentDark,
                                        textAlign = TextAlign.Center)
                                    if (isSel) Icon(Icons.Outlined.CheckCircle, null,
                                        tint = skin.accent,
                                        modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // ── Logout ────────────────────────────────────────────────
                    SectionCard("Sesion") {
                        ProfileActionRow(Icons.Outlined.Logout, "Cerrar sesion",
                            p.email, isDestructive = true) { showLogoutDialog = true }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────

        if (showPhotoDialog) {
            PhotoSourceDialog(
                onCamera  = {
                    showPhotoDialog = false
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) cameraLauncher.launch(photoUri)
                    else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                },
                onGallery  = { showPhotoDialog = false; galleryLauncher.launch("image/*") },
                onRemove   = {
                    showPhotoDialog = false
                    profile?.let { authViewModel.updateProfile(it.copy(avatarPhotoPath = null)) }
                },
                onDismiss  = { showPhotoDialog = false }
            )
        }

        if (showDobDialog) {
            DateOfBirthDialog(
                currentDob = profile?.dateOfBirth,
                onConfirm  = { epoch ->
                    profile?.let { authViewModel.updateProfile(it.copy(dateOfBirth = epoch)) }
                    showDobDialog = false
                },
                onDismiss  = { showDobDialog = false }
            )
        }

        if (showLinkDialog) {
            LinkPartnerDialog(
                onConfirm = { code ->
                    authViewModel.linkWithPartner(code) { _, _ -> }
                    showLinkDialog = false
                },
                onDismiss = { showLinkDialog = false }
            )
        }

        if (showPinDialog) {
            PinDialog(
                hasPin    = profile?.pinHash != null,
                onConfirm = { pin -> authViewModel.updatePin(pin); showPinDialog = false },
                onRemove  = { authViewModel.updatePin(null); showPinDialog = false },
                onDismiss = { showPinDialog = false }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title   = { Text("Limpiar registros") },
                text    = { Text("Elimina todos los registros de este perfil. No se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        mainViewModel.clearAllRecords()
                        showClearDialog = false
                    }) { Text("Limpiar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title   = { Text("Cerrar sesion") },
                text    = { Text("Tu cuenta y datos locales se mantienen guardados.") },
                confirmButton = {
                    TextButton(onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                        onLogout()
                    }) { Text("Cerrar sesion", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

// =============================================================================
// AVATAR CARD
// =============================================================================

@Composable
private fun AvatarCard(
    profile: UserProfile, editName: Boolean, nameValue: String, selectedColor: String,
    onPhotoClick: () -> Unit, onEditName: () -> Unit,
    onNameChange: (String) -> Unit, onSaveName: () -> Unit,
    onColorChange: (String) -> Unit
) {
    val avatarColor = try { Color(android.graphics.Color.parseColor(profile.avatarColor)) }
    catch (e: Exception) { Emerald600 }

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(0.7f)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // Avatar circle — tap to change photo
        Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.BottomEnd) {
            Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                .background(avatarColor).clickable(onClick = onPhotoClick),
                contentAlignment = Alignment.Center) {
                if (profile.avatarPhotoPath != null) {
                    // AsyncImage requires coil-compose dependency
                    // implementation("io.coil-kt:coil-compose:2.6.0")
                    // Once added, replace this block with:
                    // AsyncImage(model = File(profile.avatarPhotoPath), ...)
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape)
                        .background(avatarColor),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp))
                    }
                } else {
                    Text(profile.displayName.take(1).uppercase(),
                        fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            // Camera badge
            Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, Color.White, CircleShape)
                .clickable(onClick = onPhotoClick),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CameraAlt, null,
                    tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // Name
        if (editName) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = nameValue,
                    onValueChange = onNameChange,
                    label         = { Text("Nombre") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = onSaveName) {
                    Icon(Icons.Outlined.Check, null,
                        tint = Emerald600, modifier = Modifier.size(22.dp))
                }
            }
        } else {
            Row(modifier = Modifier.clickable(onClick = onEditName),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(profile.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Outlined.Edit, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp))
            }
        }

        // Color picker (if no photo)
        if (profile.avatarPhotoPath == null) {
            Text("Color del avatar:", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarColors.options.forEach { hex ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) }
                    catch (e: Exception) { Emerald600 }
                    val isSelected = hex == selectedColor
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
                        .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onColorChange(hex) })
                }
            }
        }
    }
}

// =============================================================================
// PHOTO SOURCE DIALOG
// =============================================================================

@Composable
private fun PhotoSourceDialog(
    onCamera: () -> Unit, onGallery: () -> Unit,
    onRemove: () -> Unit, onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cambiar foto de perfil", style = MaterialTheme.typography.titleMedium)
            listOf(
                Icons.Outlined.CameraAlt    to "Tomar foto con la camara" to onCamera,
                Icons.Outlined.PhotoLibrary to "Elegir de la galeria"     to onGallery,
                Icons.Outlined.Delete       to "Eliminar foto actual"     to onRemove
            ).forEach { (pair, action) ->
                val (icon, label) = pair
                Row(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                    .clickable(onClick = action).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                    Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cancelar")
            }
        }
    }
}

// =============================================================================
// DATE OF BIRTH DIALOG
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthDialog(currentDob: Long?, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val cal = remember {
        Calendar.getInstance().apply {
            if (currentDob != null) timeInMillis = currentDob
            else add(Calendar.YEAR, -25) // default to 25 years ago
        }
    }
    var day   by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var year  by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }

    val months = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")

    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Text("Fecha de nacimiento", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Day
                OutlinedTextField(
                    value         = day.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> if (v in 1..31) day = v } },
                    label         = { Text("Dia") },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                // Month dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1.5f)) {
                    OutlinedTextField(
                        value         = months[month - 1],
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Mes") },
                        shape         = RoundedCornerShape(12.dp),
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier      = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        months.forEachIndexed { idx, name ->
                            DropdownMenuItem(text = { Text(name) },
                                onClick = { month = idx + 1; expanded = false })
                        }
                    }
                }
                // Year
                OutlinedTextField(
                    value         = year.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> if (v in 1900..2030) year = v } },
                    label         = { Text("Ano") },
                    modifier      = Modifier.weight(1.4f),
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Age preview
            val age = Calendar.getInstance().get(Calendar.YEAR) - year
            if (age in 1..120) {
                Text("Edad: $age anos", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        val c = Calendar.getInstance()
                        c.set(year, month - 1, day, 0, 0, 0)
                        onConfirm(c.timeInMillis)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) { Text("Guardar") }
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color.White.copy(0.65f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    note: String? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor)
            note?.let { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String,
    note: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface)
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            note?.let { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.Outlined.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileTypeBadge(profileType: String, isTestMode: Boolean) {
    val (bg, color, label) = when {
        isTestMode || profileType == ProfileType.TEST.name ->
            Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Perfil de Testeo")
        profileType == ProfileType.MASCULINE.name ->
            Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "Perfil Masculino")
        else ->
            Triple(Color(0xFFECFDF5), Emerald600, "Perfil Femenino")
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun LinkPartnerDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Vincular con tu pareja", style = MaterialTheme.typography.titleMedium)
            Text("Ingresa el codigo de 6 letras de tu pareja.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value         = code.uppercase(),
                onValueChange = { if (it.length <= 6) code = it },
                label         = { Text("Codigo de la pareja") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = { onConfirm(code) }, enabled = code.length == 6,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)) {
                    Text("Vincular")
                }
            }
        }
    }
}

@Composable
private fun PinDialog(
    hasPin: Boolean, onConfirm: (String) -> Unit,
    onRemove: () -> Unit, onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (hasPin) "Cambiar PIN" else "Configurar PIN",
                style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value           = pin,
                onValueChange   = { if (it.length <= 6) pin = it },
                label           = { Text("PIN (4-6 digitos)") },
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                singleLine      = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasPin) TextButton(onClick = onRemove) {
                    Text("Quitar PIN", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(onClick = { onConfirm(pin) }, enabled = pin.length >= 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)) {
                    Text("Guardar")
                }
            }
        }
    }
}