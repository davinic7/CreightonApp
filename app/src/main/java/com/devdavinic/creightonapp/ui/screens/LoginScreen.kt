package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdavinic.creightonapp.model.AvatarColors
import com.devdavinic.creightonapp.model.ProfileType
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.AuthViewModel

// =============================================================================
// LOGIN SCREEN v2 — skin-aware, animated tabs Login / Registro
// =============================================================================

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    val skin = currentSkin
    var showLogin by remember { mutableStateOf(true) }
    val authState by authViewModel.authState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(skin.gradient()))) {

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)) {

            // Logo
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(skin.accent), contentAlignment = Alignment.Center) {
                    Text("🌿", fontSize = 38.sp)
                }
                Text("CreightonApp", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = skin.accentDark)
                Text("NaProTRACKING", fontSize = 13.sp, color = skin.accentDark.copy(0.6f),
                    letterSpacing = 3.sp)
            }

            // Tab switcher
            Row(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(0.4f)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(true to "Iniciar sesion", false to "Registrarse").forEach { (isLogin, label) ->
                    val isActive = showLogin == isLogin
                    Box(modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) skin.accent else Color.Transparent)
                        .clickable { showLogin = isLogin }
                        .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = if (isActive) Color.White else skin.accentDark.copy(0.7f))
                    }
                }
            }

            // Error from auth state
            val error = (authState as? com.devdavinic.creightonapp.viewmodel.AuthState.Error)?.message
            AnimatedVisibility(visible = error != null) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF2F2))
                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp))
                    Text(error ?: "", fontSize = 13.sp, color = Color(0xFFDC2626))
                }
            }

            // Form
            AnimatedContent(targetState = showLogin, transitionSpec = {
                if (targetState) slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                else slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            }, label = "loginTab") { isLogin ->
                if (isLogin) LoginForm(authViewModel, skin)
                else RegisterForm(authViewModel, skin)
            }
        }
    }
}

// =============================================================================
// LOGIN FORM
// =============================================================================

@Composable
private fun LoginForm(authViewModel: AuthViewModel, skin: AppSkin) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val isLoading = authViewModel.authState.collectAsState().value is
            com.devdavinic.creightonapp.viewmodel.AuthState.Loading

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AuthTextField(email, { email = it }, "Email",
            Icons.Outlined.Email, KeyboardType.Email, skin = skin)
        AuthTextField(password, { password = it }, "Contrasena",
            Icons.Outlined.Lock, KeyboardType.Password, skin = skin,
            isPassword = true, showPass = showPass,
            onTogglePass = { showPass = !showPass })

        Button(
            onClick  = { authViewModel.login(email, password) },
            enabled  = email.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = skin.accent)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Iniciar sesion", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// =============================================================================
// REGISTER FORM
// =============================================================================

@Composable
private fun RegisterForm(authViewModel: AuthViewModel, skin: AppSkin) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var confirmPass  by remember { mutableStateOf("") }
    var displayName  by remember { mutableStateOf("") }
    var showPass     by remember { mutableStateOf(false) }
    var profileType  by remember { mutableStateOf(ProfileType.FEMININE) }
    var avatarColor  by remember { mutableStateOf(AvatarColors.options.first()) }
    val isLoading    = authViewModel.authState.collectAsState().value is
            com.devdavinic.creightonapp.viewmodel.AuthState.Loading

    val passMatch = password == confirmPass || confirmPass.isEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        AuthTextField(displayName, { displayName = it }, "Nombre",
            Icons.Outlined.Person, KeyboardType.Text, skin = skin)
        AuthTextField(email, { email = it }, "Email",
            Icons.Outlined.Email, KeyboardType.Email, skin = skin)
        AuthTextField(password, { password = it }, "Contrasena",
            Icons.Outlined.Lock, KeyboardType.Password, skin = skin,
            isPassword = true, showPass = showPass,
            onTogglePass = { showPass = !showPass })
        AuthTextField(confirmPass, { confirmPass = it }, "Confirmar contrasena",
            Icons.Outlined.Lock, KeyboardType.Password, skin = skin,
            isPassword = true, showPass = showPass,
            isError = !passMatch,
            errorText = if (!passMatch) "Las contrasenas no coinciden" else null)

        // Profile type
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tipo de perfil:", fontSize = 12.sp,
                color = skin.accentDark.copy(0.7f))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ProfileType.FEMININE, ProfileType.MASCULINE, ProfileType.TEST)
                    .forEach { type ->
                        val isSel = profileType == type
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) skin.accent else Color.White.copy(0.5f))
                            .border(1.dp,
                                if (isSel) skin.accent else skin.accent.copy(0.3f),
                                RoundedCornerShape(10.dp))
                            .clickable { profileType = type }
                            .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(when (type) {
                                    ProfileType.FEMININE  -> "👩"
                                    ProfileType.MASCULINE -> "👨"
                                    ProfileType.TEST      -> "🧪"
                                }, fontSize = 18.sp)
                                Text(when (type) {
                                    ProfileType.FEMININE  -> "Femenino"
                                    ProfileType.MASCULINE -> "Masculino"
                                    ProfileType.TEST      -> "Testeo"
                                }, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                    color = if (isSel) Color.White else skin.accentDark)
                            }
                        }
                    }
            }
        }

        // Avatar color
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Color del avatar:", fontSize = 12.sp, color = skin.accentDark.copy(0.7f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarColors.options.forEach { hex ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) }
                    catch (e: Exception) { skin.accent }
                    val isSel = hex == avatarColor
                    Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(color)
                        .border(if (isSel) 3.dp else 0.dp, Color.White, CircleShape)
                        .clickable { avatarColor = hex })
                }
            }
        }

        val canRegister = email.isNotBlank() && password.length >= 6 &&
                passMatch && displayName.isNotBlank() && !isLoading

        Button(
            onClick  = {
                authViewModel.register(email, password, displayName, profileType, avatarColor)
            },
            enabled  = canRegister,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = skin.accent)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Crear cuenta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// =============================================================================
// REUSABLE TEXT FIELD
// =============================================================================

@Composable
private fun AuthTextField(
    value: String, onValueChange: (String) -> Unit,
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType, skin: AppSkin,
    isPassword: Boolean = false, showPass: Boolean = false,
    onTogglePass: (() -> Unit)? = null,
    isError: Boolean = false, errorText: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            label         = { Text(label) },
            leadingIcon   = { Icon(icon, null, tint = skin.accent) },
            trailingIcon  = if (isPassword) {{
                IconButton(onClick = { onTogglePass?.invoke() }) {
                    Icon(if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        null, tint = skin.accent)
                }
            }} else null,
            visualTransformation = if (isPassword && !showPass)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine    = true,
            isError       = isError,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(14.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = skin.accent,
                unfocusedBorderColor = skin.accent.copy(0.3f),
                focusedLabelColor    = skin.accent,
                cursorColor          = skin.accent,
                focusedLeadingIconColor = skin.accent
            )
        )
        if (isError && errorText != null) {
            Text(errorText, fontSize = 10.sp, color = Color(0xFFDC2626),
                modifier = Modifier.padding(start = 8.dp))
        }
    }
}