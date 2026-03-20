package com.devdavinic.creightonapp.ui.screens

import androidx.compose.animation.*
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
import com.devdavinic.creightonapp.model.*
import com.devdavinic.creightonapp.ui.theme.*

// =============================================================================
// TRIVIA SCREEN
// Interactive quiz on the Creighton method
// =============================================================================

@Composable
fun TriviaScreen(onBack: () -> Unit) {
    var session     by remember { mutableStateOf<TriviaSession?>(null) }
    var showAnswer  by remember { mutableStateOf(false) }
    var selectedAns by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(
            Color(0xFFEDE9FE), Color(0xFFD1FAE5), Color(0xFFDBEAFE))))) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (session != null) { session = null; showAnswer = false; selectedAns = null }
                    else onBack()
                }) {
                    Icon(Icons.Outlined.ArrowBack, "Atras",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trivia Creighton", style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF7C3AED))
                    session?.let {
                        Text("${it.currentIndex + 1} / ${it.questions.size}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(48.dp))
            }

            if (session == null) {
                TriviaMenu { count, category ->
                    val questions = if (category == null) TriviaBank.randomSet(count)
                    else (TriviaBank.byCategory(category).shuffled().take(count))
                    session    = TriviaSession(questions = questions)
                    showAnswer = false; selectedAns = null
                }
            } else {
                val s = session!!
                if (s.isComplete) {
                    TriviaResults(session = s, onRestart = {
                        session = null; showAnswer = false; selectedAns = null
                    })
                } else {
                    TriviaQuestionView(
                        session     = s,
                        showAnswer  = showAnswer,
                        selectedAns = selectedAns,
                        onSelect    = { idx ->
                            if (!showAnswer) {
                                selectedAns = idx
                                showAnswer  = true
                            }
                        },
                        onNext      = {
                            val newAnswers = s.answers + (s.currentQuestion!!.id to selectedAns!!)
                            val nextIndex  = s.currentIndex + 1
                            session = s.copy(
                                answers      = newAnswers,
                                currentIndex = nextIndex,
                                isComplete   = nextIndex >= s.questions.size
                            )
                            showAnswer  = false
                            selectedAns = null
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// MENU
// =============================================================================

@Composable
private fun TriviaMenu(onStart: (Int, TriviaCategory?) -> Unit) {
    var selectedCount    by remember { mutableIntStateOf(10) }
    var selectedCategory by remember { mutableStateOf<TriviaCategory?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Total questions badge
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF7C3AED).copy(0.08f))
            .border(1.dp, Color(0xFF7C3AED).copy(0.3f), RoundedCornerShape(14.dp))
            .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(Color(0xFF7C3AED).copy(0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Quiz, null, tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(26.dp))
            }
            Column {
                Text("Pon a prueba tu conocimiento",
                    fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    color = Color(0xFF7C3AED))
                Text("${TriviaBank.all.size} preguntas sobre el Modelo Creighton",
                    fontSize = 12.sp, color = Color(0xFF7C3AED).copy(0.75f))
            }
        }

        // Number of questions
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.7f)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cuantas preguntas?", fontWeight = FontWeight.Medium, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20).forEach { n ->
                    val sel = selectedCount == n
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (sel) Color(0xFF7C3AED) else Color(0xFFF1F5F9))
                        .clickable { selectedCount = n }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Category filter
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.7f)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tema (opcional)", fontWeight = FontWeight.Medium, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)

            // All topics option
            val isAllSel = selectedCategory == null
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (isAllSel) Color(0xFF7C3AED).copy(0.1f) else Color.Transparent)
                .border(if (isAllSel) 1.5.dp else 1.dp,
                    if (isAllSel) Color(0xFF7C3AED) else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp))
                .clickable { selectedCategory = null }.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("🎲", fontSize = 16.sp)
                Text("Todos los temas (mezcla aleatoria)", fontSize = 13.sp,
                    color = if (isAllSel) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface)
                if (isAllSel) Spacer(Modifier.weight(1f)).also {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(16.dp))
                }
            }

            TriviaCategory.entries.forEach { cat ->
                val isSel = selectedCategory == cat
                val count = TriviaBank.byCategory(cat).size
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) Color(0xFF7C3AED).copy(0.1f) else Color.Transparent)
                    .border(if (isSel) 1.5.dp else 1.dp,
                        if (isSel) Color(0xFF7C3AED) else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp))
                    .clickable { selectedCategory = cat }.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.label, fontSize = 13.sp,
                            color = if (isSel) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface)
                        Text("$count preguntas disponibles", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isSel) Icon(Icons.Outlined.CheckCircle, null,
                        tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                }
            }
        }

        Button(
            onClick  = { onStart(selectedCount, selectedCategory) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) {
            Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Comenzar trivia", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// =============================================================================
// QUESTION VIEW
// =============================================================================

@Composable
private fun TriviaQuestionView(
    session: TriviaSession,
    showAnswer: Boolean,
    selectedAns: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    val question  = session.currentQuestion ?: return
    val isCorrect = selectedAns == question.correctIndex

    Column(modifier = Modifier.fillMaxSize()) {

        // Progress bar
        LinearProgressIndicator(
            progress   = { (session.currentIndex + 1f) / session.questions.size },
            modifier   = Modifier.fillMaxWidth(),
            color      = Color(0xFF7C3AED),
            trackColor = Color(0xFF7C3AED).copy(0.15f)
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Category + difficulty badge
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizBadge(question.category.label, Color(0xFF7C3AED))
                QuizBadge(when (question.difficulty) {
                    TriviaDifficulty.EASY   -> "Facil"
                    TriviaDifficulty.MEDIUM -> "Medio"
                    TriviaDifficulty.HARD   -> "Dificil"
                }, when (question.difficulty) {
                    TriviaDifficulty.EASY   -> Emerald600
                    TriviaDifficulty.MEDIUM -> Color(0xFFD97706)
                    TriviaDifficulty.HARD   -> Color(0xFFDC2626)
                })
                QuizBadge(when (question.type) {
                    TriviaType.MULTIPLE_CHOICE -> "Opcion multiple"
                    TriviaType.TRUE_FALSE       -> "V / F"
                    TriviaType.STAMP_GUESS      -> "Adivina la estampa"
                }, Color(0xFF2563EB))
            }

            // Stamp context
            question.stampContext?.let { ctx ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Emerald200.copy(0.4f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Layers, null, tint = Emerald600,
                        modifier = Modifier.size(16.dp))
                    Text("Observacion del dia: $ctx", fontSize = 13.sp,
                        fontWeight = FontWeight.Medium, color = Color(0xFF065F46))
                }
            }

            // Question text
            Text(question.question, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 24.sp)

            // Options
            question.options.forEachIndexed { idx, option ->
                val isSelected = selectedAns == idx
                val isRight    = showAnswer && idx == question.correctIndex
                val isWrong    = showAnswer && isSelected && !isRight

                val scale by animateFloatAsState(
                    if (isSelected) 1.02f else 1f,
                    spring(Spring.DampingRatioMediumBouncy), label = "optScale"
                )
                val bg = when {
                    isRight    -> Color(0xFFECFDF5)
                    isWrong    -> Color(0xFFFEF2F2)
                    isSelected -> Color(0xFF7C3AED).copy(0.08f)
                    else       -> Color.White.copy(0.7f)
                }
                val border = when {
                    isRight    -> Emerald600
                    isWrong    -> Color(0xFFDC2626)
                    isSelected -> Color(0xFF7C3AED)
                    else       -> MaterialTheme.colorScheme.outlineVariant
                }

                Row(modifier = Modifier.fillMaxWidth().scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(if (isSelected || isRight) 2.dp else 1.dp, border, RoundedCornerShape(12.dp))
                    .clickable(enabled = !showAnswer) { onSelect(idx) }
                    .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {

                    // Letter badge
                    Box(modifier = Modifier.size(30.dp).clip(CircleShape)
                        .background(if (isRight) Emerald600 else if (isWrong) Color(0xFFDC2626)
                        else if (isSelected) Color(0xFF7C3AED)
                        else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Text(listOf("A","B","C","D")[idx], fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRight || isWrong || isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(option, fontSize = 14.sp, modifier = Modifier.weight(1f),
                        color = when {
                            isRight -> Color(0xFF065F46)
                            isWrong -> Color(0xFFDC2626)
                            else    -> MaterialTheme.colorScheme.onSurface
                        })

                    if (isRight) Icon(Icons.Outlined.CheckCircle, null,
                        tint = Emerald600, modifier = Modifier.size(20.dp))
                    if (isWrong) Icon(Icons.Outlined.Cancel, null,
                        tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                }
            }

            // Explanation
            AnimatedVisibility(visible = showAnswer) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF3C7))
                    .border(1.dp,
                        if (isCorrect) Emerald600.copy(0.4f) else Color(0xFFFBBF24),
                        RoundedCornerShape(14.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isCorrect) Icons.Outlined.EmojiEvents else Icons.Outlined.Info,
                            null,
                            tint = if (isCorrect) Emerald600 else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp))
                        Text(if (isCorrect) "Correcto!" else "Incorrecto",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            color = if (isCorrect) Emerald600 else Color(0xFFD97706))
                    }
                    Text(question.explanation, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Next button
        AnimatedVisibility(visible = showAnswer) {
            val isLast = session.currentIndex == session.questions.size - 1
            Button(
                onClick  = onNext,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)
                    .navigationBarsPadding().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(if (isLast) "Ver resultados" else "Siguiente pregunta",
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(if (isLast) Icons.Outlined.EmojiEvents else Icons.Outlined.ArrowForward,
                    null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// =============================================================================
// RESULTS
// =============================================================================

@Composable
private fun TriviaResults(session: TriviaSession, onRestart: () -> Unit) {
    val pct   = session.percentage
    val emoji = when { pct >= 80 -> "🏆"; pct >= 60 -> "⭐"; pct >= 40 -> "📚"; else -> "💪" }
    val msg   = when { pct >= 80 -> "Excelente!"; pct >= 60 -> "Muy bien!"; pct >= 40 -> "Buen intento!"; else -> "A seguir aprendiendo!" }
    val color = when { pct >= 80 -> Emerald600; pct >= 60 -> Color(0xFF2563EB); else -> Color(0xFFD97706) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 32.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {

        Text(emoji, fontSize = 64.sp)

        Text(msg, style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)

        // Score circle
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(color.copy(0.1f))
            .border(4.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${session.score}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
                Text("de ${session.questions.size}", fontSize = 13.sp, color = color.copy(0.8f))
            }
        }

        Text("$pct% de respuestas correctas", fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)

        // Per-question summary
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.7f)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resumen", fontWeight = FontWeight.Medium, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface)
            session.questions.forEachIndexed { i, q ->
                val ans = session.answers[q.id]
                val correct = ans == q.correctIndex
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top) {
                    Icon(if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel, null,
                        tint = if (correct) Emerald600 else Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${i + 1}. ${q.question}", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface, lineHeight = 14.sp,
                            maxLines = 2)
                        if (!correct) Text("Correcto: ${q.options[q.correctIndex]}",
                            fontSize = 10.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }

        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Jugar de nuevo", fontWeight = FontWeight.SemiBold)
        }
    }
}

// =============================================================================
// HELPERS
// =============================================================================

@Composable
private fun QuizBadge(label: String, color: Color) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(color.copy(0.1f))
        .padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
    }
}