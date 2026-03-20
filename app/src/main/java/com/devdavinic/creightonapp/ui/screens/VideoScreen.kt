package com.devdavinic.creightonapp.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.devdavinic.creightonapp.model.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.devdavinic.creightonapp.ui.theme.*

// =============================================================================
// VIDEO SCREEN
// Catalog of educational videos streamed from Firebase Storage
// Requires ExoPlayer (Media3) dependency in build.gradle:
//   implementation "androidx.media3:media3-exoplayer:1.3.1"
//   implementation "androidx.media3:media3-ui:1.3.1"
// =============================================================================

@Composable
fun VideoScreen(onBack: () -> Unit) {
    var selectedCategory by remember { mutableStateOf<VideoCategory?>(null) }
    var playingVideo     by remember { mutableStateOf<VideoLesson?>(null) }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.linearGradient(listOf(Emerald200, Purple100, Pink200)))) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(modifier = Modifier.fillMaxWidth().background(Color(0x66FFFFFF))
                .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (playingVideo != null) playingVideo = null
                    else if (selectedCategory != null) selectedCategory = null
                    else onBack()
                }) {
                    Icon(Icons.Outlined.ArrowBack, "Atras",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when {
                            playingVideo     != null -> playingVideo!!.title
                            selectedCategory != null -> selectedCategory!!.label
                            else -> "Videos educativos"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${VideoCatalog.all.size} videos disponibles",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(48.dp))
            }

            AnimatedContent(
                targetState = Triple(playingVideo, selectedCategory, Unit),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "videoNav"
            ) { (playing, category, _) ->
                when {
                    playing != null ->
                        VideoPlayerView(video = playing, onBack = { playingVideo = null })
                    category != null ->
                        VideoListView(
                            category   = category,
                            onPlay     = { playingVideo = it }
                        )
                    else ->
                        VideoCategoryGrid(onCategorySelect = { selectedCategory = it })
                }
            }
        }
    }
}

// =============================================================================
// CATEGORY GRID
// =============================================================================

@Composable
private fun VideoCategoryGrid(onCategorySelect: (VideoCategory) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text("Selecciona un tema", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))

        VideoCategory.entries.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cat ->
                    val count = VideoCatalog.byCategory(cat).size
                    CategoryCard(cat, count, Modifier.weight(1f)) { onCategorySelect(cat) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryCard(
    category: VideoCategory, count: Int,
    modifier: Modifier, onClick: () -> Unit
) {
    val colors = listOf(
        Color(0xFFECFDF5) to Emerald600,
        Color(0xFFEFF6FF) to Color(0xFF2563EB),
        Color(0xFFFEF3C7) to Color(0xFFD97706),
        Color(0xFFFAF5FF) to Color(0xFF7C3AED),
        Color(0xFFFEF2F2) to Color(0xFFDC2626),
        Color(0xFFF0FDF4) to Color(0xFF15803D),
        Color(0xFFFDF4FF) to Color(0xFFDB2777),
        Color(0xFFF0F9FF) to Color(0xFF0EA5E9)
    )
    val (bg, accent) = colors[category.ordinal % colors.size]

    Column(modifier = modifier.clip(RoundedCornerShape(16.dp))
        .background(bg).border(1.dp, accent.copy(0.2f), RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(category.icon, fontSize = 28.sp)
        Text(category.label, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = accent)
        Text(category.description, fontSize = 10.sp, color = accent.copy(0.7f), lineHeight = 13.sp)
        Text("$count videos", fontSize = 10.sp, color = accent,
            fontWeight = FontWeight.Medium)
    }
}

// =============================================================================
// VIDEO LIST
// =============================================================================

@Composable
private fun VideoListView(category: VideoCategory, onPlay: (VideoLesson) -> Unit) {
    val videos = VideoCatalog.byCategory(category)
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.6f)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(category.icon, fontSize = 24.sp)
            Column {
                Text(category.label, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(category.description, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        videos.forEach { video ->
            VideoCard(video = video, onPlay = { onPlay(video) })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun VideoCard(video: VideoLesson, onPlay: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(0.75f)).clickable(onClick = onPlay).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {

        // Thumbnail / Play icon
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PlayCircleOutline, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(video.title, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Text(video.description, fontSize = 11.sp, maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccessTime, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp))
                Text("${video.durationMinutes} min", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (video.isForPartner) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Emerald600.copy(0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("Para la pareja", fontSize = 9.sp, color = Emerald600)
                    }
                }
            }
        }

        Icon(Icons.Outlined.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

// =============================================================================
// VIDEO PLAYER
// =============================================================================

@Composable
private fun VideoPlayerView(video: VideoLesson, onBack: () -> Unit) {
    val context = LocalContext.current
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    // Load video URL from Firebase Storage SDK
    // storagePath can be:
    //   - A full https:// URL (plays directly)
    //   - A Storage path like "videos/intro_01.mp4" (SDK fetches download URL)
    LaunchedEffect(video.id) {
        isLoading = true
        errorMsg  = null
        try {
            val url = if (video.storagePath.startsWith("https://")) {
                video.storagePath
            } else {
                // Use Firebase Storage SDK to get download URL programmatically
                FirebaseStorage.getInstance()
                    .reference
                    .child(video.storagePath)
                    .downloadUrl
                    .await()
                    .toString()
            }
            videoUrl  = url
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            errorMsg  = when {
                e.message?.contains("Object does not exist") == true ->
                    "Video no encontrado en Storage. Verifica que el archivo exista en: ${video.storagePath}"
                e.message?.contains("permission") == true ->
                    "Sin permiso para acceder al video. Verifica las reglas de Firebase Storage."
                else -> "Error al cargar el video: ${e.message}"
            }
        }
    }

    val exoPlayer = remember(videoUrl) {
        if (videoUrl == null) null
        else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Player
        Box(modifier = Modifier.fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)) {
            when {
                isLoading ->
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = Color.White)
                errorMsg != null ->
                    Column(modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.ErrorOutline, null,
                            tint = Color.White, modifier = Modifier.size(40.dp))
                        Text(errorMsg ?: "", color = Color.White, fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                exoPlayer != null ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory  = {
                            PlayerView(it).apply {
                                player       = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        }
                    )
            }
        }

        // Video info
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Text(video.title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(Emerald600.copy(0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(video.category.label, fontSize = 11.sp, color = Emerald600)
                }
                Text("${video.durationMinutes} minutos", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(video.description, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)

            // Storage path note (for admin)
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CloudUpload, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp))
                Text("Archivo: ${video.storagePath}", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}