package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdavinic.creightonapp.ui.theme.AppSkin

// =============================================================================
// SKIN ONBOARDING SCREEN
// Shown on first launch — lets user pick their visual theme
// =============================================================================

@Composable
fun SkinOnboardingScreen(onSkinSelected: (AppSkin) -> Unit) {
    var selected by remember { mutableStateOf(AppSkin.EMERALD) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(selected.gradient()))) {

        Column(modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)) {

            Spacer(Modifier.height(16.dp))

            // Logo / Icon
            Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(Color.White.copy(0.6f)),
                contentAlignment = Alignment.Center) {
                Text("🌿", fontSize = 40.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bienvenida a CreightonApp",
                    fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = selected.accentDark, textAlign = TextAlign.Center)
                Text("Elige el estilo visual que mas te guste.\nPodes cambiarlo en cualquier momento desde tu perfil.",
                    fontSize = 14.sp, color = selected.accentDark.copy(0.7f),
                    textAlign = TextAlign.Center, lineHeight = 20.sp)
            }

            // Skin cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()) {
                AppSkin.entries.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { skin ->
                            SkinCard(
                                skin       = skin,
                                isSelected = selected == skin,
                                modifier   = Modifier.weight(1f)
                            ) { selected = skin }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Preview strip
            AnimatedContent(targetState = selected, transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(200))
            }, label = "preview") { skin ->
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.7f)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Vista previa — ${skin.displayName}",
                        fontSize = 13.sp, color = skin.accent, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Fake stamps
                        listOf(
                            Color(0xFFEF4444) to "H",
                            Color.White       to "👶",
                            skin.accent       to "P",
                            skin.accentLight  to "3"
                        ).forEach { (bg, label) ->
                            Box(modifier = Modifier.size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(2.dp, skin.accent.copy(0.4f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (bg == Color.White || bg == skin.accentLight)
                                        skin.accentDark else Color.White)
                            }
                        }
                    }
                    // Fake button
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(skin.accent).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Registrar observacion", color = Color.White,
                            fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }

            // Confirm button
            Button(
                onClick = { onSkinSelected(selected) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = selected.accent)
            ) {
                Text("Continuar con ${selected.displayName}",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SkinCard(
    skin: AppSkin, isSelected: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1.04f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "skinScale"
    )

    Column(modifier = modifier.scale(scale)
        .clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(skin.gradient()))
        .border(if (isSelected) 3.dp else 1.dp,
            if (isSelected) skin.accent else Color.White.copy(0.5f),
            RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(skin.emoji, fontSize = 24.sp)
            if (isSelected) {
                Box(modifier = Modifier.size(22.dp).clip(CircleShape)
                    .background(skin.accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        Text(skin.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = skin.accentDark)
        Text(skin.description, fontSize = 10.sp, color = skin.accentDark.copy(0.7f),
            lineHeight = 13.sp, textAlign = TextAlign.Center)

        // Color dots
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(skin.gradientStart, skin.accent, skin.accentDark).forEach { c ->
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(c))
            }
        }
    }
}