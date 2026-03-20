package com.devdavinic.creightonapp.ui.screens

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.devdavinic.creightonapp.export.PdfExporter
import com.devdavinic.creightonapp.ui.theme.*
import com.devdavinic.creightonapp.viewmodel.AuthViewModel
import com.devdavinic.creightonapp.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExportScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val allRecords  by viewModel.allRecords.collectAsState()
    val profile     by authViewModel.currentProfile.collectAsState()

    val allCycles   = remember(allRecords) { PdfExporter.groupIntoCycles(allRecords) }
    val totalCycles = allCycles.size

    var cyclesToExport   by remember { mutableIntStateOf(minOf(4, totalCycles)) }
    var isGenerating     by remember { mutableStateOf(false) }
    var exportedFilePath by remember { mutableStateOf<String?>(null) }
    var errorMessage     by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(Emerald200, Purple100, Pink200)))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Atras",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exportar Planilla", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text("PDF NaProTRACKING — 4 ciclos por hoja",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(48.dp))
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Status
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChipExport("Ciclos disponibles", "$totalCycles", Emerald600, Modifier.weight(1f))
                    StatChipExport("Registros", "${allRecords.size}", Color(0xFF2563EB), Modifier.weight(1f))
                }

                if (totalCycles == 0) {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp)).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp))
                        Text("No hay ciclos registrados para exportar.",
                            fontSize = 13.sp, color = Color(0xFF92400E))
                    }
                } else {
                    // Cycle count selector
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.75f)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        Text("Cuantos ciclos incluir", fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Cada hoja A4 muestra 4 ciclos con hasta 35 dias cada uno.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Option buttons
                        val options = buildList {
                            if (totalCycles >= 1)  add(1  to "1 ciclo")
                            if (totalCycles >= 2)  add(2  to "2 ciclos")
                            if (totalCycles >= 4)  add(4  to "4 ciclos")
                            if (totalCycles >= 6)  add(6  to "6 ciclos")
                            if (totalCycles >= 8)  add(8  to "8 ciclos")
                            if (totalCycles >= 12) add(12 to "12 ciclos")
                            if (totalCycles > 1)   add(totalCycles to "Todos ($totalCycles)")
                        }.distinctBy { it.first }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.chunked(3).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { (n, label) ->
                                        val isSelected = cyclesToExport == n
                                        Box(modifier = Modifier.weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Emerald600 else Color(0xFFF1F5F9))
                                            .border(1.dp,
                                                if (isSelected) Emerald600
                                                else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(10.dp))
                                            .clickable { cyclesToExport = n }
                                            .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(label, fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold
                                                    else FontWeight.Normal,
                                                    color = if (isSelected) Color.White
                                                    else MaterialTheme.colorScheme.onSurface)
                                                val pages = ((n + 3) / 4)
                                                Text("$pages hoja${if (pages > 1) "s" else ""}",
                                                    fontSize = 9.sp,
                                                    color = if (isSelected) Color.White.copy(0.8f)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    // Fill remaining
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }

                        // Preview line
                        val selected = allCycles.takeLast(cyclesToExport)
                        val days     = selected.sumOf { it.size }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(Emerald200.copy(0.4f)).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PictureAsPdf, null,
                                tint = Emerald600, modifier = Modifier.size(16.dp))
                            Text("$cyclesToExport ciclo(s) · $days dias · ${(cyclesToExport + 3) / 4} hoja(s) A4",
                                fontSize = 12.sp, color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium)
                        }
                    }

                    // What's included
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.65f)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("El PDF incluye", fontWeight = FontWeight.Medium,
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        listOf(
                            "Estampas coloreadas por dia (rojo/verde/blanco/verde claro)",
                            "Codigos oficiales NaProTRACKING bajo cada estampa",
                            "Fecha de cada registro",
                            "Dia Pico marcado con P (borde violeta), cuenta post-Pico 1/2/3",
                            "Punto azul = Intercurso (I), punto rosa = Autoexamen (AM)"
                        ).forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 12.sp, color = Emerald600)
                                Text(item, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp)
                            }
                        }
                    }

                    // Success result
                    exportedFilePath?.let { path ->
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Emerald600, RoundedCornerShape(14.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CheckCircle, null,
                                    tint = Emerald600, modifier = Modifier.size(20.dp))
                                Text("PDF generado", fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp, color = Color(0xFF065F46))
                            }
                            Text(path.substringAfterLast("/"), fontSize = 10.sp,
                                color = Color(0xFF065F46))
                            Button(
                                onClick = {
                                    try {
                                        val file = java.io.File(path)
                                        val uri  = FileProvider.getUriForFile(
                                            context, "${context.packageName}.provider", file)
                                        context.startActivity(Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }, "Compartir planilla"))
                                    } catch (e: Exception) {
                                        errorMessage = "Error al compartir: ${e.message}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Emerald600)
                            ) {
                                Icon(Icons.Outlined.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Compartir / Guardar PDF")
                            }
                        }
                    }

                    // Error
                    errorMessage?.let { err ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp))
                            Text(err, fontSize = 12.sp, color = Color(0xFFDC2626))
                        }
                    }

                    // Generate button
                    Button(
                        onClick = {
                            scope.launch {
                                isGenerating = true; errorMessage = null; exportedFilePath = null
                                try {
                                    val selected = allCycles.takeLast(cyclesToExport)
                                    val file = withContext(Dispatchers.IO) {
                                        PdfExporter.generate(
                                            cycles   = selected,
                                            userName = profile?.displayName ?: "Usuaria",
                                            context  = context
                                        )
                                    }
                                    exportedFilePath = file.absolutePath
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        enabled  = !isGenerating,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Generando PDF...")
                        } else {
                            Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generar PDF", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StatChipExport(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(12.dp))
        .background(Color.White.copy(alpha = 0.7f))
        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
    }
}