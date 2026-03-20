package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.MainViewModel

// =============================================================================
// MODULE 7 - SPECIAL CASES SCREEN
// Based on Appendix A of the Creighton manual
// =============================================================================

@Composable
fun SpecialCasesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<SpecialCategory?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(Emerald200, Purple100, Pink200)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            SpecialCasesHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info banner
                InfoBanner()

                // Yellow stamps card
                YellowStampsCard()

                // Category list
                Text(
                    "Selecciona tu situacion reproductiva",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                SpecialCategory.entries
                    .filter { it != SpecialCategory.NORMAL }
                    .forEach { category ->
                        CategoryCard(
                            category  = category,
                            onClick   = { selectedCategory = category }
                        )
                    }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Detail dialog
        selectedCategory?.let { category ->
            CategoryDetailDialog(
                category  = category,
                onDismiss = { selectedCategory = null }
            )
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun SpecialCasesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhiteAlpha40)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, "Atras", tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Instrucciones Especiales",
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.primary
            )
            Text(
                "Apendice A del manual Creighton",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

// =============================================================================
// INFO BANNER
// =============================================================================

@Composable
private fun InfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEFF6FF))
            .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Outlined.Info, null,
            tint     = Color(0xFF2563EB),
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                "Las instrucciones especiales se aplican cuando hay circunstancias reproductivas particulares.",
                fontSize   = 13.sp,
                color      = Color(0xFF1E40AF),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Estas instrucciones son una referencia educativa. Siempre consulta con tu Profesional de FertilityCare para una conduccion a profundidad.",
                fontSize   = 11.sp,
                color      = Color(0xFF2563EB).copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
        }
    }
}

// =============================================================================
// YELLOW STAMPS CARD
// =============================================================================

@Composable
private fun YellowStampsCard() {
    var expanded by remember { mutableStateOf(false) }
    val info = SpecialCasesContent.yellowStampInfo

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFEFCE8))
            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFBBF24)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
                Column {
                    Text(
                        info.title,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = Color(0xFF92400E)
                    )
                    Text(
                        "Toca para ver las instrucciones",
                        fontSize = 11.sp,
                        color    = Color(0xFFD97706)
                    )
                }
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                null,
                tint     = Color(0xFFD97706),
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = Color(0xFFFBBF24).copy(alpha = 0.4f))

                Text(
                    info.description,
                    fontSize   = 13.sp,
                    color      = Color(0xFF92400E),
                    lineHeight = 19.sp
                )

                Text(
                    "Cuando se usan:",
                    fontWeight = FontWeight.Medium,
                    fontSize   = 12.sp,
                    color      = Color(0xFF92400E)
                )
                info.phases.forEachIndexed { i, phase ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i+1}", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = Color.White)
                        }
                        Text(phase, fontSize = 12.sp, color = Color(0xFF92400E),
                            lineHeight = 17.sp, modifier = Modifier.weight(1f))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Warning, null,
                        tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    Text(
                        info.discontinueRule,
                        fontSize   = 11.sp,
                        color      = Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }

                Text(
                    "Las estampas amarillas solo se usan bajo indicacion especifica del Profesional de FertilityCare.",
                    fontSize   = 11.sp,
                    color      = Color(0xFFD97706),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// =============================================================================
// CATEGORY CARD
// =============================================================================

@Composable
private fun CategoryCard(
    category: SpecialCategory,
    onClick: () -> Unit
) {
    val (bg, border, accent) = categoryColors(category)
    val hasInstructions = SpecialCasesContent.getInstructions(category).isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIcon(category), null,
                tint     = accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.label,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (category.usesYellowStamps) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFBBF24).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Estampa A", fontSize = 9.sp,
                            color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text(
                SpecialCasesContent.getCategoryNote(category),
                fontSize   = 11.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                maxLines   = 2
            )
        }

        Icon(
            Icons.Outlined.ChevronRight, null,
            tint     = accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

// =============================================================================
// CATEGORY DETAIL DIALOG
// =============================================================================

@Composable
private fun CategoryDetailDialog(
    category: SpecialCategory,
    onDismiss: () -> Unit
) {
    val instructions = SpecialCasesContent.getInstructions(category)
    val (bg, border, accent) = categoryColors(category)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Dialog header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.08f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon(category), null,
                            tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(category.label, fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (category.usesYellowStamps) {
                            Text("Puede requerir estampas amarillas",
                                fontSize = 10.sp, color = Color(0xFFD97706))
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.06f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, null,
                        tint = accent, modifier = Modifier.size(16.dp))
                    Text(
                        SpecialCasesContent.getCategoryNote(category),
                        fontSize   = 13.sp,
                        color      = accent.copy(alpha = 0.9f),
                        lineHeight = 18.sp
                    )
                }

                if (instructions.isEmpty()) {
                    Text(
                        "Para esta categoria se aplican las instrucciones basicas del metodo.",
                        fontSize   = 13.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                } else {
                    Text(
                        "Instrucciones especiales",
                        fontWeight = FontWeight.Medium,
                        fontSize   = 13.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )

                    instructions.forEachIndexed { index, instruction ->
                        InstructionCard(
                            number      = index + 1,
                            instruction = instruction,
                            accent      = accent
                        )
                    }
                }

                // Professional referral note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Warning, null,
                        tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    Text(
                        "Esta informacion es educativa. Consulta con tu Profesional de FertilityCare para una conduccion personalizada de estas instrucciones especiales.",
                        fontSize   = 11.sp,
                        color      = Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// =============================================================================
// INSTRUCTION CARD
// =============================================================================

@Composable
private fun InstructionCard(
    number: Int,
    instruction: SpecialInstruction,
    accent: Color
) {
    val cardBg     = if (instruction.isWarning) Color(0xFFFEE2E2) else WhiteAlpha60
    val cardBorder = if (instruction.isWarning) Color(0xFFEF4444).copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val titleColor = if (instruction.isWarning) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (instruction.isWarning) Color(0xFFEF4444) else accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$number",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
            Text(
                instruction.title,
                fontWeight = FontWeight.Medium,
                fontSize   = 13.sp,
                color      = titleColor
            )
            if (instruction.isWarning) {
                Icon(Icons.Outlined.Warning, null,
                    tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
            }
        }

        Text(
            instruction.body,
            fontSize   = 13.sp,
            color      = MaterialTheme.colorScheme.onSurface,
            lineHeight = 19.sp
        )

        instruction.actionRequired?.let { action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.ArrowForward, null,
                    tint = accent, modifier = Modifier.size(14.dp))
                Text(action, fontSize = 11.sp, color = accent, lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Composable
private fun categoryColors(category: SpecialCategory): Triple<Color, Color, Color> {
    return when (category) {
        SpecialCategory.NORMAL ->
            Triple(WhiteAlpha60, WhiteAlpha40, Emerald600)
        SpecialCategory.BREASTFEEDING_TOTAL,
        SpecialCategory.BREASTFEEDING_PARTIAL ->
            Triple(Color(0xFFF0FDF4), Color(0xFF86EFAC), Emerald600)
        SpecialCategory.POSTPARTUM_NO_BREASTFEEDING ->
            Triple(Color(0xFFEFF6FF), Color(0xFF93C5FD), Color(0xFF2563EB))
        SpecialCategory.POST_PILL ->
            Triple(Color(0xFFFAF5FF), Color(0xFFD8B4FE), Color(0xFF7C3AED))
        SpecialCategory.PREMENOPAUSE ->
            Triple(Color(0xFFFFF7ED), Color(0xFFFDBA74), Color(0xFFD97706))
        SpecialCategory.ANOVULATION ->
            Triple(Color(0xFFFFF7ED), Color(0xFFFDBA74), Color(0xFFD97706))
        SpecialCategory.POSTPARTUM_ABORTION ->
            Triple(Color(0xFFFFF1F2), Color(0xFFFDA4AF), Color(0xFFE11D48))
        SpecialCategory.INFERTILITY ->
            Triple(Color(0xFFF0F9FF), Color(0xFF7DD3FC), Color(0xFF0284C7))
        SpecialCategory.UNUSUAL_BLEEDING ->
            Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFDC2626))
    }
}

@Composable
private fun categoryIcon(category: SpecialCategory): ImageVector = when (category) {
    SpecialCategory.NORMAL                    -> Icons.Outlined.Loop
    SpecialCategory.BREASTFEEDING_TOTAL       -> Icons.Outlined.ChildFriendly
    SpecialCategory.BREASTFEEDING_PARTIAL     -> Icons.Outlined.ChildFriendly
    SpecialCategory.POSTPARTUM_NO_BREASTFEEDING -> Icons.Outlined.PregnantWoman
    SpecialCategory.POST_PILL                 -> Icons.Outlined.Medication
    SpecialCategory.PREMENOPAUSE              -> Icons.Outlined.Elderly
    SpecialCategory.ANOVULATION               -> Icons.Outlined.Timeline
    SpecialCategory.POSTPARTUM_ABORTION       -> Icons.Outlined.Healing
    SpecialCategory.INFERTILITY               -> Icons.Outlined.Favorite
    SpecialCategory.UNUSUAL_BLEEDING          -> Icons.Outlined.Warning
}