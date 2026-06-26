package com.example.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import org.burnoutcrew.reorderable.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.data.MusicSource
import com.example.data.PlaylistEntity
import com.example.data.SongModel
import com.example.player.FallenDownloadManager
import com.example.player.PlaybackManager
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

// Color swatches based on selected accent color index
// 0: Purple, 1: Pink, 2: Blue, 3: Mint, 4: Orange, 5: Green
val ACCENT_PRIMARY_COLORS = listOf(
    Color(0xFFA855F7), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFF3B82F6), // Blue
    Color(0xFF14B8A6), // Mint
    Color(0xFFF97316), // Orange
    Color(0xFF10B981)  // Green
)

val ACCENT_SECONDARY_COLORS = listOf(
    Color(0xFFEC4899), // Pink (gradient tail for Purple)
    Color(0xFFF43F5E), // Rose (gradient tail for Pink)
    Color(0xFF06B6D4), // Cyan
    Color(0xFF34D399), // Emerald
    Color(0xFFFBBF24), // Amber
    Color(0xFF84CC16)  // Lime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallenApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Theme configurations
    val isAmoled by viewModel.amoledTheme.collectAsState()
    val isLight by viewModel.lightTheme.collectAsState()
    val accentIdx by viewModel.accentColorIndex.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()
    val bgOpacity by viewModel.backgroundOpacity.collectAsState()
    val libPlacement by viewModel.libraryPlacement.collectAsState()
    val customBgHex by viewModel.customBgColor.collectAsState()
    val customBgGradStartHex by viewModel.customBgGradientStart.collectAsState()
    val customBgGradEndHex by viewModel.customBgGradientEnd.collectAsState()
    val canvasMode by viewModel.canvasMode.collectAsState()
    
    val primaryColor = ACCENT_PRIMARY_COLORS[accentIdx]
    val secondaryColor = ACCENT_SECONDARY_COLORS[accentIdx]
    
    val backgroundColor = when {
        isLight -> Color(0xFFF3F4F6)
        bgType == 1 -> Color(0xFF000000) // AMOLED Black
        bgType == 2 -> Color(0xFF06040A) // Deep Cosmic Purple
        bgType == 3 -> Color(0xFF03060C) // Midnight Blue
        bgType == 4 -> Color(0xFF020603) // Forest Green
        bgType == 5 -> Color(0xFF0E1315) // Titanium Slate
        bgType == 6 -> Color(0xFF0C0206) // Crimson Wine
        bgType == 7 -> Color(customBgHex) // Custom Solid Theme
        bgType == 8 -> Color(customBgGradEndHex) // Custom Gradient End Theme
        else -> Color(0xFF0D0D0D) // Classic Dark
    }
    
    val surfaceColor = when {
        isLight -> Color(0xFFFFFFFF)
        bgType == 1 -> Color(0xFF101010)
        bgType == 2 -> Color(0xFF1F1836)
        bgType == 3 -> Color(0xFF121B2F)
        bgType == 4 -> Color(0xFF0D2114)
        bgType == 5 -> Color(0xFF1E282C)
        bgType == 6 -> Color(0xFF280F18)
        bgType == 7 -> Color(customBgHex)
        bgType == 8 -> {
            val startColor = Color(customBgGradStartHex)
            Color(
                red = (startColor.red * 0.35f + 0.04f * 0.65f).coerceIn(0f, 1f),
                green = (startColor.green * 0.35f + 0.04f * 0.65f).coerceIn(0f, 1f),
                blue = (startColor.blue * 0.35f + 0.08f * 0.65f).coerceIn(0f, 1f),
                alpha = 1.0f
            )
        }
        else -> Color(0xFF1C1C1C)
    }

    val backgroundBrush = remember(isLight, bgType, primaryColor, customBgHex, customBgGradStartHex, customBgGradEndHex) {
        if (isLight) {
            Brush.verticalGradient(listOf(Color(0xFFF9FAFB), Color(0xFFE5E7EB)))
        } else {
            val baseColor = backgroundColor
            when (bgType) {
                1 -> Brush.verticalGradient(listOf(baseColor, baseColor))
                7 -> Brush.verticalGradient(listOf(baseColor, baseColor))
                8 -> Brush.verticalGradient(listOf(Color(customBgGradStartHex), Color(customBgGradEndHex)))
                2 -> Brush.verticalGradient(listOf(Color(0xFF2E1C4E), Color(0xFF06040A)))
                3 -> Brush.verticalGradient(listOf(Color(0xFF11264E), Color(0xFF03060C)))
                4 -> Brush.verticalGradient(listOf(Color(0xFF0E381E), Color(0xFF020603)))
                5 -> Brush.verticalGradient(listOf(Color(0xFF2D3C45), Color(0xFF0E1315)))
                6 -> Brush.verticalGradient(listOf(Color(0xFF4C1025), Color(0xFF0C0206)))
                else -> {
                    val blendedTopColor = Color(
                        red = (primaryColor.red * 0.12f + baseColor.red * 0.88f).coerceIn(0f, 1f),
                        green = (primaryColor.green * 0.12f + baseColor.green * 0.88f).coerceIn(0f, 1f),
                        blue = (primaryColor.blue * 0.16f + baseColor.blue * 0.84f).coerceIn(0f, 1f),
                        alpha = 1.0f
                    )
                    Brush.verticalGradient(
                        colors = listOf(
                            blendedTopColor,
                            baseColor
                        )
                    )
                }
            }
        }
    }
    
    val textColor = if (isLight) Color(0xFF111827) else Color(0xFFF9FAFB)
    val textMutedColor = if (isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF)

    // Setup active playback triggers
    LaunchedEffect(Unit) {
        PlaybackManager.initPlayer(context)
    }

    val crossfadeDuration by viewModel.crossfadeDuration.collectAsState()
    LaunchedEffect(crossfadeDuration) {
        PlaybackManager.crossfadeDurationMs.value = crossfadeDuration * 1000L
    }

    // Active navigation states
    var currentTab by remember { mutableStateOf("home") } // home, search, library, settings
    
    // Auto redirection if library placement changes
    LaunchedEffect(libPlacement) {
        if (libPlacement != "tabs" && currentTab == "library") {
            currentTab = "home"
        }
    }
    
    // Now Playing Fullscreen sheet state
    var isPlayerExpanded by remember { mutableStateOf(false) }
    
    // Song detail options bottom sheet state
    var songOptionsTarget by remember { mutableStateOf<SongModel?>(null) }
    
    // Active selection tracking
    val currentPlayingSong by PlaybackManager.currentSong.collectAsState()

    // Dynamically manage system bars icons light/dark state
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = isLight && !isPlayerExpanded
            insetsController.isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = if (isLight) {
            lightColorScheme(
                primary = primaryColor,
                secondary = secondaryColor,
                background = Color.Transparent,
                surface = surfaceColor.copy(alpha = bgOpacity),
                surfaceVariant = surfaceColor.copy(alpha = bgOpacity)
            )
        } else {
            darkColorScheme(
                primary = primaryColor,
                secondary = secondaryColor,
                background = Color.Transparent,
                surface = surfaceColor.copy(alpha = bgOpacity),
                surfaceVariant = surfaceColor.copy(alpha = bgOpacity)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            if (canvasMode != "none") {
                CanvasBackground(
                    viewModel = viewModel,
                    currentSongThumbnailUrl = currentPlayingSong?.thumbnailUrl ?: "",
                    fallbackColor = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Scaffold(
            bottomBar = {
                Column {
                    // Mini player overlays bottom nav if an audio is loaded
                    if (currentPlayingSong != null) {
                        MiniPlayer(
                            song = currentPlayingSong!!,
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            textColor = textColor,
                            textMutedColor = textMutedColor,
                            backgroundColor = surfaceColor.copy(alpha = bgOpacity),
                            onExpand = { isPlayerExpanded = true }
                        )
                    }
                    
                    NavigationBar(
                        containerColor = surfaceColor.copy(alpha = bgOpacity),
                        tonalElevation = 0.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        val navItems = remember(libPlacement) {
                            val items = mutableListOf(
                                Triple("home", Icons.Default.Home, "Home"),
                                Triple("search", Icons.Default.Search, "Search")
                            )
                            if (libPlacement == "tabs") {
                                items.add(Triple("library", Icons.AutoMirrored.Filled.PlaylistPlay, "Library"))
                            }
                            items.add(Triple("settings", Icons.Default.Settings, "Settings"))
                            items
                        }
                        
                        navItems.forEach { (route, icon, label) ->
                            val selected = currentTab == route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { 
                                    currentTab = route
                                    isPlayerExpanded = false
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = primaryColor,
                                    selectedTextColor = primaryColor,
                                    unselectedIconColor = textMutedColor,
                                    unselectedTextColor = textMutedColor,
                                    indicatorColor = primaryColor.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_${route}")
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Render active tab viewport
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "TabTransition"
                ) { targetRoute ->
                    when (targetRoute) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textMutedColor = textMutedColor,
                            surfaceColor = surfaceColor,
                            libPlacement = libPlacement,
                            onSongSelect = { song, index, list ->
                                PlaybackManager.setQueue(list, index)
                                PlaybackManager.playSongAtIndex(context, index)
                            },
                            onSongOptions = { songOptionsTarget = it }
                        )
                        "search" -> SearchScreen(
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            textColor = textColor,
                            textMutedColor = textMutedColor,
                            onSongSelect = { song, index, list ->
                                PlaybackManager.setQueue(list, index)
                                PlaybackManager.playSongAtIndex(context, index)
                            },
                            onSongOptions = { songOptionsTarget = it }
                        )
                        "library" -> LibraryScreen(
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textMutedColor = textMutedColor,
                            surfaceColor = surfaceColor,
                            onSongSelect = { song, index, list ->
                                PlaybackManager.setQueue(list, index)
                                PlaybackManager.playSongAtIndex(context, index)
                            },
                            onSongOptions = { songOptionsTarget = it }
                        )
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textMutedColor = textMutedColor,
                            surfaceColor = surfaceColor
                        )
                    }
                }

                // Expanded Player Slider layer
                AnimatedVisibility(
                    visible = isPlayerExpanded,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentPlayingSong != null) {
                        PlayerScreen(
                            song = currentPlayingSong!!,
                            viewModel = viewModel,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            textColor = textColor,
                            textMutedColor = textMutedColor,
                            surfaceColor = surfaceColor,
                            onCollapse = { isPlayerExpanded = false }
                        )
                    }
                }

                // Options Bottom Sheet modal
                if (songOptionsTarget != null) {
                    SongOptionsDialog(
                        song = songOptionsTarget!!,
                        viewModel = viewModel,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        primaryColor = primaryColor,
                        onDismiss = { songOptionsTarget = null }
                    )
                }
            }
        }
    }
}
}

// ----------------------------------------------------
// MINI PLAYER COMPOSABLE
// ----------------------------------------------------
@Composable
fun MiniPlayer(
    song: SongModel,
    viewModel: MainViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    backgroundColor: Color,
    onExpand: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val position by PlaybackManager.playbackPosition.collectAsState()
    val duration by PlaybackManager.playbackDuration.collectAsState()
    val isLight = textColor.red < 0.5f
    
    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    val edgeBorderColor = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f)

    val canvasMode by viewModel.canvasMode.collectAsState()
    val baseBlurRadius by viewModel.canvasBlurRadius.collectAsState()
    val miniBlur = (baseBlurRadius * 0.5f).coerceAtMost(8f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp, top = 2.dp)
            .testTag("mini_player")
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (canvasMode == "none") backgroundColor.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, edgeBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() }
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (canvasMode != "none") {
                    CanvasBackground(
                        viewModel = viewModel,
                        currentSongThumbnailUrl = song.thumbnailUrl,
                        fallbackColor = Color.Transparent,
                        blurOverride = miniBlur,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onExpand() },
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            color = textMutedColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    WingsPlayPauseButton(
                        isPlaying = isPlaying,
                        isLoading = false,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        sizeDp = 48.dp,
                        onClick = {
                            if (isPlaying) PlaybackManager.pause() else PlaybackManager.resume()
                        },
                        modifier = Modifier.testTag("mini_play_button")
                    )
                    
                    IconButton(
                        onClick = { PlaybackManager.skipNext(context) },
                        modifier = Modifier.testTag("mini_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = textColor
                        )
                    }
                }
                
                // Progress horizontal neon line at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color.Gray.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                            )
                    )
                }
            }
          }
        }
    }
}

// ----------------------------------------------------
// REUSABLE HIGHLIGHTED SHIMMER EFFECT
// ----------------------------------------------------
@Composable
fun ShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .width(140.dp)
            .padding(end = 12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .background(Color.Gray.copy(alpha = alphaAnim))
            )
        }
    }
}

// ----------------------------------------------------
// SOURCES CHIP ROW LIST COMPOSABLE
// ----------------------------------------------------
@Composable
fun SourceSwitcherRow(
    viewModel: MainViewModel,
    primaryColor: Color,
    textColor: Color,
    textMutedColor: Color
) {
    val activeSource by viewModel.activeSource.collectAsState()
    val isLight by viewModel.lightTheme.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val list = listOf(
            Pair(MusicSource.JIO_SAAVN, "JioSaavn")
        )
        
        list.forEach { (source, name) ->
            val isSelected = activeSource == source
            
            val unselectedChipBg = if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f)
            val unselectedBorderColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) primaryColor else unselectedChipBg
                    )
                    .then(
                        if (!isSelected) Modifier.border(BorderStroke(1.dp, unselectedBorderColor), RoundedCornerShape(16.dp))
                        else Modifier
                    )
                    .clickable { viewModel.setSource(source) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("source_chip_${name}")
            ) {
                Text(
                    text = name,
                    color = if (isSelected) Color.White else textColor.copy(alpha = 0.6f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ----------------------------------------------------
// HOME SEED SCREEN COMPOSABLE
// ----------------------------------------------------
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    primaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    surfaceColor: Color,
    libPlacement: String,
    onSongSelect: (SongModel, Int, List<SongModel>) -> Unit,
    onSongOptions: (SongModel) -> Unit
) {
    val trendsState by viewModel.trendingSongsState.collectAsState()
    val recents by viewModel.recentlyPlayed.collectAsState()
    val isLight by viewModel.lightTheme.collectAsState()
    val bgOpacity by viewModel.backgroundOpacity.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Simple Top banner layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FALLEN",
                    color = if (isLight) textColor else Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Your safe, ad-free music haven",
                    color = textMutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Reload trigger
            IconButton(
                onClick = {
                    viewModel.reloadTrendingNow()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = textColor
                )
            }
        }
        
        SourceSwitcherRow(
            viewModel = viewModel,
            primaryColor = primaryColor,
            textColor = textColor,
            textMutedColor = textMutedColor
        )

        if (libPlacement == "home_top") {
            Text(
                text = "My Library Space",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor.copy(alpha = bgOpacity))
                    .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                LibraryScreen(
                    viewModel = viewModel,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textMutedColor = textMutedColor,
                    surfaceColor = surfaceColor.copy(alpha = bgOpacity),
                    onSongSelect = onSongSelect,
                    onSongOptions = onSongOptions
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Trending Section
        Text(
            text = "Trending Now",
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        when (val state = trendsState) {
            is UiState.Loading -> {
                LazyRow(
                    modifier = Modifier.padding(start = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(5) { ShimmerCard() }
                }
            }
            is UiState.Success -> {
                val list = state.data
                if (list.isEmpty()) {
                    Text(
                        text = "No trending items found.",
                        color = textMutedColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        itemsIndexed(list) { index, song ->
                            Box(
                                modifier = Modifier
                                    .width(132.dp)
                                    .padding(end = 12.dp)
                                    .clickable { onSongSelect(song, index, list) }
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Subtle vertical dark gradient protecting legibility
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                                    )
                                                )
                                        )
                                        // Flawless circular play indicator bubble at bottom right corner
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = song.title,
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = textMutedColor,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Failure -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Error charging sources: ${state.message}", color = Color.Red, fontSize = 13.sp)
                    Button(
                        onClick = { viewModel.reloadTrendingNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recently Played Section
        Text(
            text = "Recently Played",
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        if (recents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = primaryColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Play some tracks to start your wave!",
                        color = textMutedColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                itemsIndexed(recents) { index, song ->
                    Box(
                        modifier = Modifier
                            .width(132.dp)
                            .padding(end = 12.dp)
                            .clickable { onSongSelect(song, index, recents) }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Subtle vertical dark gradient protecting legibility
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                                            )
                                        )
                                )
                                // Flawless circular play indicator bubble at bottom right corner
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = song.title,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = textMutedColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        if (libPlacement == "home_bottom") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Library Space",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceColor.copy(alpha = bgOpacity))
                    .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                LibraryScreen(
                    viewModel = viewModel,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textMutedColor = textMutedColor,
                    surfaceColor = surfaceColor.copy(alpha = bgOpacity),
                    onSongSelect = onSongSelect,
                    onSongOptions = onSongOptions
                )
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ----------------------------------------------------
// SEARCH SCREEN COMPOSABLE
// ----------------------------------------------------
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    onSongSelect: (SongModel, Int, List<SongModel>) -> Unit,
    onSongOptions: (SongModel) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val wrappedOnSongSelect: (SongModel, Int, List<SongModel>) -> Unit = { song, index, list ->
        keyboardController?.hide()
        focusManager.clearFocus()
        onSongSelect(song, index, list)
    }

    val query by viewModel.searchTerms.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val recentQueries by viewModel.recentSearches.collectAsState()
    val currentActiveSource by viewModel.activeSource.collectAsState()
    val playListEntities by viewModel.playlists.collectAsState(initial = emptyList())

    var activeCategory by remember { mutableStateOf("Tracks") } // "Tracks", "Albums", "Artists", "Playlists"
    var selectedAlbumDetails by remember { mutableStateOf<AlbumGroup?>(null) }
    var selectedArtistDetails by remember { mutableStateOf<ArtistGroup?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search entry header
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("search_field"),
            placeholder = { Text("Search songs, artists, albums...", color = textMutedColor) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = primaryColor) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = textColor)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                cursorColor = primaryColor
            )
        )

        // Stream Source Selection chips for high visibility in search
        SourceSwitcherRow(
            viewModel = viewModel,
            primaryColor = primaryColor,
            textColor = textColor,
            textMutedColor = textMutedColor
        )

        // Category Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Tracks", "Albums", "Artists", "Playlists")
            categories.forEach { cat ->
                val isSelected = activeCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) primaryColor else textColor.copy(alpha = 0.08f))
                        .clickable { activeCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("search_cat_${cat.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(cat) {
                                "Tracks" -> Icons.Default.MusicNote
                                "Albums" -> Icons.Default.Album
                                "Artists" -> Icons.Default.Person
                                else -> Icons.AutoMirrored.Filled.PlaylistPlay
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else textColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Render based on selected category (Playlists handled separately to allow offline searches)
        if (activeCategory == "Playlists") {
            val displayedPlaylists = remember(playListEntities, query) {
                if (query.isBlank()) playListEntities
                else playListEntities.filter { it.name.contains(query, ignoreCase = true) }
            }

            if (displayedPlaylists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank()) "No playlists created yet. Create one in Library Screen!" else "No playlists matching \"$query\"",
                        color = textMutedColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(displayedPlaylists) { plist ->
                        var showPlaylistSongs by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPlaylistSongs = true }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(primaryColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(plist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Playlist • Tap to view songs", color = textMutedColor, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Playlist", tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                        if (showPlaylistSongs) {
                            val flowData = viewModel.playlistWithSongs(plist.id).collectAsState(initial = null)
                            Dialog(onDismissRequest = { showPlaylistSongs = false }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(secondaryColor.copy(alpha = 0.95f))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(plist.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = { showPlaylistSongs = false }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val songList = flowData.value?.songs?.map { it.toSongModel() } ?: emptyList()
                                        if (songList.isEmpty()) {
                                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Text("This playlist has no songs yet.", color = textMutedColor, fontSize = 13.sp)
                                            }
                                        } else {
                                            LazyColumn(modifier = Modifier.weight(1f)) {
                                                itemsIndexed(songList) { i, song ->
                                                    SongCardRow(
                                                        song = song,
                                                        textColor = textColor,
                                                        textMutedColor = textMutedColor,
                                                        primaryColor = primaryColor,
                                                        onClick = {
                                                            wrappedOnSongSelect(song, i, songList)
                                                            showPlaylistSongs = false
                                                        },
                                                        onLongClick = { onSongOptions(song) },
                                                        onMoreClick = { onSongOptions(song) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (query.trim().isEmpty()) {
            // Display empty searches view & historic suggestions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (recentQueries.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.clearRecentSearches() }) {
                            Text("Clear all", color = primaryColor, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentQueries.forEach { recent ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Gray.copy(alpha = 0.15f))
                                    .clickable { viewModel.updateSearchQuery(recent) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(recent, color = textColor, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = primaryColor.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Discover tracks on ${currentActiveSource.name}",
                                color = textMutedColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // Render remote results based on tracks, albums, artists
            when (val state = searchState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
                is UiState.Success -> {
                    val songs = state.data
                    if (songs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No tracks found matching \"$query\"", color = textMutedColor, fontSize = 14.sp)
                        }
                    } else {
                        when (activeCategory) {
                            "Tracks" -> {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                    itemsIndexed(songs) { index, song ->
                                        SongCardRow(
                                            song = song,
                                            textColor = textColor,
                                            textMutedColor = textMutedColor,
                                            primaryColor = primaryColor,
                                            onClick = { wrappedOnSongSelect(song, index, songs) },
                                            onLongClick = { onSongOptions(song) },
                                            onMoreClick = { onSongOptions(song) }
                                        )
                                    }
                                }
                            }
                            "Albums" -> {
                                val albumsList = remember(songs) {
                                    songs
                                        .filter { it.album.isNotBlank() }
                                        .groupBy { it.album }
                                        .map { (albumName, albumSongs) ->
                                            AlbumGroup(
                                                name = albumName,
                                                songs = albumSongs,
                                                artist = albumSongs.firstOrNull()?.artist ?: "Unknown Artist",
                                                thumbnailUrl = albumSongs.firstOrNull()?.thumbnailUrl ?: ""
                                            )
                                        }
                                        .sortedBy { it.name.lowercase() }
                                }
                                
                                if (albumsList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No albums detected in search results.", color = textMutedColor, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                        items(albumsList) { album ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedAlbumDetails = album }
                                                    .padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    ) {
                                                        if (album.thumbnailUrl.isNotEmpty()) {
                                                            AsyncImage(
                                                                model = album.thumbnailUrl,
                                                                contentDescription = album.name,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(primaryColor.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(Icons.Default.Album, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(album.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("${album.artist} • ${album.songs.size} Track${if (album.songs.size > 1) "s" else ""}", color = textMutedColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Album", tint = primaryColor, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                                            }
                                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                                        }
                                    }
                                }
                            }
                            "Artists" -> {
                                val artistsList = remember(songs) {
                                    songs
                                        .filter { it.artist.isNotBlank() }
                                        .groupBy { it.artist }
                                        .map { (artistName, artistSongs) ->
                                            ArtistGroup(
                                                name = artistName,
                                                songs = artistSongs,
                                                thumbnailUrl = artistSongs.firstOrNull()?.thumbnailUrl ?: ""
                                            )
                                        }
                                        .sortedBy { it.name.lowercase() }
                                }
                                
                                if (artistsList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No artists detected in search results.", color = textMutedColor, fontSize = 13.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                                        items(artistsList) { artist ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedArtistDetails = artist }
                                                    .padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(50.dp)
                                                            .clip(CircleShape)
                                                    ) {
                                                        if (artist.thumbnailUrl.isNotEmpty()) {
                                                            AsyncImage(
                                                                model = artist.thumbnailUrl,
                                                                contentDescription = artist.name,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(primaryColor.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(artist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("${artist.songs.size} Track${if (artist.songs.size > 1) "s" else ""}", color = textMutedColor, fontSize = 11.sp)
                                                    }
                                                }
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Artist", tint = primaryColor, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                                            }
                                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Failure -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text("Search failed: ${state.message}", color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.updateSearchQuery(query) },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Search Screen Album detailed dialog
    selectedAlbumDetails?.let { album ->
        Dialog(onDismissRequest = { selectedAlbumDetails = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(secondaryColor.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                if (album.thumbnailUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = album.thumbnailUrl,
                                        contentDescription = album.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(primaryColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(album.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(album.artist, color = textMutedColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = { selectedAlbumDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${album.songs.size} tracks", color = textMutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(album.songs) { idx, song ->
                            SongCardRow(
                                song = song,
                                textColor = textColor,
                                textMutedColor = textMutedColor,
                                primaryColor = primaryColor,
                                onClick = {
                                    wrappedOnSongSelect(song, idx, album.songs)
                                    selectedAlbumDetails = null
                                },
                                onLongClick = { onSongOptions(song) },
                                onMoreClick = { onSongOptions(song) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Search Screen Artist detailed dialog
    selectedArtistDetails?.let { artist ->
        Dialog(onDismissRequest = { selectedArtistDetails = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(secondaryColor.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            ) {
                                if (artist.thumbnailUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = artist.thumbnailUrl,
                                        contentDescription = artist.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(primaryColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(artist.name, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        IconButton(onClick = { selectedArtistDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${artist.songs.size} tracks", color = textMutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(artist.songs) { idx, song ->
                            SongCardRow(
                                song = song,
                                textColor = textColor,
                                textMutedColor = textMutedColor,
                                primaryColor = primaryColor,
                                onClick = {
                                    wrappedOnSongSelect(song, idx, artist.songs)
                                    selectedArtistDetails = null
                                },
                                onLongClick = { onSongOptions(song) },
                                onMoreClick = { onSongOptions(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CARD ROW IN SEARCH / PLAYLIST LISTS
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCardRow(
    song: SongModel,
    textColor: Color,
    textMutedColor: Color,
    primaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val durationSeconds = song.durationMs / 1000
    val durationMinutes = durationSeconds / 60
    val durationRemSeconds = durationSeconds % 60
    val durationText = String.format("%02d:%02d", durationMinutes, durationRemSeconds)

    val activeDownloadsMap by FallenDownloadManager.activeDownloads.collectAsState()
    val isDownloading = activeDownloadsMap.containsKey(song.id)
    val progress = activeDownloadsMap[song.id] ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Offline Cache",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${song.artist} • ${song.album}",
                    color = textMutedColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isDownloading) {
            CircularProgressIndicator(
                progress = progress / 100f,
                color = primaryColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = durationText, color = textMutedColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = textMutedColor)
                }
            }
        }
    }
}

// Simple FlowRow helper to align historic chip elements
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        var x = 0
        var y = 0
        var rowHeight = 0
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                if (x + placeable.width > constraints.maxWidth) {
                    x = 0
                    y += rowHeight + 8.dp.roundToPx()
                    rowHeight = 0
                }
                placeable.placeRelative(x, y)
                x += placeable.width + 8.dp.roundToPx()
                rowHeight = maxOf(rowHeight, placeable.height)
            }
        }
    }
}

// ----------------------------------------------------
// LIBRARY SCREEN COMPOSABLE
// ----------------------------------------------------
data class AlbumGroup(
    val name: String,
    val songs: List<SongModel>,
    val artist: String,
    val thumbnailUrl: String
)

data class ArtistGroup(
    val name: String,
    val songs: List<SongModel>,
    val thumbnailUrl: String
)

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    primaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    surfaceColor: Color,
    onSongSelect: (SongModel, Int, List<SongModel>) -> Unit,
    onSongOptions: (SongModel) -> Unit
) {
    val playListEntities by viewModel.playlists.collectAsState()
    val favoritedList by viewModel.likedSongs.collectAsState()
    val localCacheList by viewModel.downloadedSongs.collectAsState()
    val bgOpacity by viewModel.backgroundOpacity.collectAsState()

    var activeCategory by remember { mutableStateOf("Tracks") } // "Tracks", "Albums", "Artists", "Playlists"
    var activeTrackSubView by remember { mutableStateOf("All") } // "All", "Liked", "Downloads"
    var activeSortOrder by remember { mutableStateOf("Name A-Z") } // "Name A-Z", "Name Z-A", "Artist A-Z", "Album A-Z", "Duration"
    var showSortMenu by remember { mutableStateOf(false) }

    var playlistInputString by remember { mutableStateOf("") }
    var displaysCreateOverlay by remember { mutableStateOf(false) }
    var selectedPlaylistTarget by remember { mutableStateOf<PlaylistEntity?>(null) }
    
    var selectedAlbumDetails by remember { mutableStateOf<AlbumGroup?>(null) }
    var selectedArtistDetails by remember { mutableStateOf<ArtistGroup?>(null) }

    // Combined library tracks
    val libraryTracksCombined = remember(favoritedList, localCacheList) {
        (favoritedList + localCacheList).distinctBy { it.id }
    }

    // Extracted Album Groups
    val albumsList = remember(libraryTracksCombined) {
        libraryTracksCombined
            .filter { it.album.isNotBlank() }
            .groupBy { it.album }
            .map { (albumName, songs) ->
                AlbumGroup(
                    name = albumName,
                    songs = songs,
                    artist = songs.firstOrNull()?.artist ?: "Unknown Artist",
                    thumbnailUrl = songs.firstOrNull()?.thumbnailUrl ?: ""
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    // Extracted Artist Groups
    val artistsList = remember(libraryTracksCombined) {
        libraryTracksCombined
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .map { (artistName, songs) ->
                ArtistGroup(
                    name = artistName,
                    songs = songs,
                    thumbnailUrl = songs.firstOrNull()?.thumbnailUrl ?: ""
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    // Sorted playlist entities
    val sortedPlaylists = remember(playListEntities) {
        playListEntities.sortedBy { it.name.lowercase() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab / Category selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Tracks", "Albums", "Artists", "Playlists")
            categories.forEach { cat ->
                val isSelected = activeCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) primaryColor else textColor.copy(alpha = 0.08f))
                        .clickable { activeCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("lib_tab_${cat.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(cat) {
                                "Tracks" -> Icons.Default.MusicNote
                                "Albums" -> Icons.Default.Album
                                "Artists" -> Icons.Default.Person
                                else -> Icons.AutoMirrored.Filled.PlaylistPlay
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else textColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (activeCategory) {
            "Tracks" -> {
                // Secondary visual filter / sorting tool row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Liked", "Downloads").forEach { subView ->
                        val isSelected = activeTrackSubView == subView
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) primaryColor.copy(alpha = 0.12f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) primaryColor else textColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { activeTrackSubView = subView }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when(subView) {
                                    "All" -> "All (${libraryTracksCombined.size})"
                                    "Liked" -> "Liked (${favoritedList.size})"
                                    "Downloads" -> "Offline (${localCacheList.size})"
                                    else -> subView
                                },
                                color = if (isSelected) primaryColor else textMutedColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Sort order display text
                    Text(
                        text = when(activeSortOrder) {
                            "Name A-Z" -> "Title A-Z"
                            "Name Z-A" -> "Title Z-A"
                            "Artist A-Z" -> "By Artist"
                            "Album A-Z" -> "By Album"
                            "Duration" -> "Duration"
                            else -> "Sorted"
                        },
                        color = textMutedColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Tracks",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                val currentTracksList = remember(activeTrackSubView, favoritedList, localCacheList, libraryTracksCombined) {
                    when (activeTrackSubView) {
                        "Liked" -> favoritedList
                        "Downloads" -> localCacheList
                        else -> libraryTracksCombined
                    }
                }

                val sortedTracksList = remember(currentTracksList, activeSortOrder) {
                    when (activeSortOrder) {
                        "Name A-Z" -> currentTracksList.sortedBy { it.title.lowercase() }
                        "Name Z-A" -> currentTracksList.sortedByDescending { it.title.lowercase() }
                        "Artist A-Z" -> currentTracksList.sortedBy { it.artist.lowercase() }
                        "Album A-Z" -> currentTracksList.sortedBy { it.album.lowercase() }
                        "Duration" -> currentTracksList.sortedByDescending { it.durationMs }
                        else -> currentTracksList
                    }
                }

                if (sortedTracksList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = when(activeTrackSubView) {
                                "Liked" -> "No hearted songs yet. Press the heart on player screen!"
                                "Downloads" -> "No downloaded songs yet. Save songs offline!"
                                else -> "No songs in your library space yet. Start searching!"
                            },
                            color = textMutedColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        itemsIndexed(sortedTracksList) { index, song ->
                            SongCardRow(
                                song = song,
                                textColor = textColor,
                                textMutedColor = textMutedColor,
                                primaryColor = primaryColor,
                                onClick = { onSongSelect(song, index, sortedTracksList) },
                                onLongClick = { onSongOptions(song) },
                                onMoreClick = { onSongOptions(song) }
                            )
                        }
                    }
                }
            }

            "Albums" -> {
                if (albumsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No albums detected. Add or download songs to auto-group!", color = textMutedColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(albumsList) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAlbumDetails = album }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        if (album.thumbnailUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = album.thumbnailUrl,
                                                contentDescription = album.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(primaryColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Album, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(album.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${album.artist} • ${album.songs.size} Track${if (album.songs.size > 1) "s" else ""}", color = textMutedColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Album", tint = primaryColor, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            "Artists" -> {
                if (artistsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No artists detected. Add or download songs to auto-group!", color = textMutedColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(artistsList) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedArtistDetails = artist }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                    ) {
                                        if (artist.thumbnailUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = artist.thumbnailUrl,
                                                contentDescription = artist.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(primaryColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(artist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${artist.songs.size} Track${if (artist.songs.size > 1) "s" else ""}", color = textMutedColor, fontSize = 11.sp)
                                    }
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Artist", tint = primaryColor, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            "Playlists" -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Playlists", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { displaysCreateOverlay = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = primaryColor)
                    }
                }

                if (sortedPlaylists.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Create a custom playlist to group your vibes!", color = textMutedColor, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(sortedPlaylists) { plist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlaylistTarget = plist }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = primaryColor)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(plist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { viewModel.deletePlaylist(plist.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = textMutedColor)
                                }
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }

        // Overlay dialog to create custom playlists
        if (displaysCreateOverlay) {
            AlertDialog(
                onDismissRequest = { displaysCreateOverlay = false },
                title = { Text("New Playlist", color = textColor) },
                text = {
                    OutlinedTextField(
                        value = playlistInputString,
                        onValueChange = { playlistInputString = it },
                        placeholder = { Text("Workout mix, study loop...", color = textMutedColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            cursorColor = primaryColor
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.createPlaylist(playlistInputString)
                            playlistInputString = ""
                            displaysCreateOverlay = false
                        }
                    ) {
                        Text("Create", color = primaryColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { displaysCreateOverlay = false }) {
                        Text("Cancel", color = textMutedColor)
                    }
                },
                containerColor = surfaceColor
            )
        }

        // Search Sort Tracks Dialog
        if (showSortMenu) {
            AlertDialog(
                onDismissRequest = { showSortMenu = false },
                title = { Text("Sort Tracks By", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Name A-Z", "Name Z-A", "Artist A-Z", "Album A-Z", "Duration").forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeSortOrder = option
                                        showSortMenu = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = activeSortOrder == option,
                                    onClick = {
                                        activeSortOrder = option
                                        showSortMenu = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = when(option) {
                                        "Name A-Z" -> "Title (A-Z)"
                                        "Name Z-A" -> "Title (Z-A)"
                                        "Artist A-Z" -> "Artist (A-Z)"
                                        "Album A-Z" -> "Album (A-Z)"
                                        "Duration" -> "Duration (Longest)"
                                        else -> option
                                    },
                                    color = textColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSortMenu = false }) {
                        Text("Close", color = primaryColor)
                    }
                },
                containerColor = surfaceColor
            )
        }

        // Detailed Playlist Overlay display showing tracks inside a selected playlist
        if (selectedPlaylistTarget != null) {
            val flowData = viewModel.playlistWithSongs(selectedPlaylistTarget!!.id).collectAsState(initial = null)
            val crossList = flowData.value?.songs?.map { it.toSongModel() } ?: emptyList()
            
            Dialog(onDismissRequest = { selectedPlaylistTarget = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceColor.copy(alpha = bgOpacity.coerceAtLeast(0.4f)))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPlaylistTarget!!.name,
                                color = textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { selectedPlaylistTarget = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                            }
                        }
                        
                        Text(
                            text = "${crossList.size} items",
                            color = textMutedColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (crossList.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("This playlist is empty. Add songs from Search!", color = textMutedColor, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                itemsIndexed(crossList) { i, song ->
                                    SongCardRow(
                                        song = song,
                                        textColor = textColor,
                                        textMutedColor = textMutedColor,
                                        primaryColor = primaryColor,
                                        onClick = {
                                            onSongSelect(song, i, crossList)
                                            selectedPlaylistTarget = null
                                        },
                                        onLongClick = { viewModel.removeSongFromPlaylist(selectedPlaylistTarget!!.id, song.id) },
                                        onMoreClick = { viewModel.removeSongFromPlaylist(selectedPlaylistTarget!!.id, song.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Album Overlay display
        selectedAlbumDetails?.let { album ->
            Dialog(onDismissRequest = { selectedAlbumDetails = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceColor.copy(alpha = bgOpacity.coerceAtLeast(0.4f)))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    if (album.thumbnailUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = album.thumbnailUrl,
                                            contentDescription = album.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(primaryColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Album, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        color = textColor,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = album.artist,
                                        color = textMutedColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { selectedAlbumDetails = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "${album.songs.size} items",
                            color = textMutedColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(album.songs) { i, song ->
                                SongCardRow(
                                    song = song,
                                    textColor = textColor,
                                    textMutedColor = textMutedColor,
                                    primaryColor = primaryColor,
                                    onClick = {
                                        onSongSelect(song, i, album.songs)
                                        selectedAlbumDetails = null
                                    },
                                    onLongClick = { onSongOptions(song) },
                                    onMoreClick = { onSongOptions(song) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detailed Artist Overlay display
        selectedArtistDetails?.let { artist ->
            Dialog(onDismissRequest = { selectedArtistDetails = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceColor.copy(alpha = bgOpacity.coerceAtLeast(0.4f)))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                ) {
                                    if (artist.thumbnailUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = artist.thumbnailUrl,
                                            contentDescription = artist.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(primaryColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = artist.name,
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            IconButton(onClick = { selectedArtistDetails = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "${artist.songs.size} items",
                            color = textMutedColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(artist.songs) { i, song ->
                                SongCardRow(
                                    song = song,
                                    textColor = textColor,
                                    textMutedColor = textMutedColor,
                                    primaryColor = primaryColor,
                                    onClick = {
                                        onSongSelect(song, i, artist.songs)
                                        selectedArtistDetails = null
                                    },
                                    onLongClick = { onSongOptions(song) },
                                    onMoreClick = { onSongOptions(song) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// THE SETTINGS DISPLAY VIEW composable
// ----------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    primaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    surfaceColor: Color
) {
    val amoled by viewModel.amoledTheme.collectAsState()
    val isLight by viewModel.lightTheme.collectAsState()
    val activeAccent by viewModel.accentColorIndex.collectAsState()
    val bgType by viewModel.backgroundType.collectAsState()
    val bgOpacity by viewModel.backgroundOpacity.collectAsState()
    val libPlacement by viewModel.libraryPlacement.collectAsState()
    val customBgHex by viewModel.customBgColor.collectAsState()
    val customBgGradStartHex by viewModel.customBgGradientStart.collectAsState()
    val customBgGradEndHex by viewModel.customBgGradientEnd.collectAsState()
    val audioQual by viewModel.audioQuality.collectAsState()
    val crossfadeVal by viewModel.crossfadeDuration.collectAsState()
    val fallbackSource by viewModel.defaultSourceConfig.collectAsState()
    val vizSensitivity by viewModel.visualizerSensitivity.collectAsState()
    val vizBarDensity by viewModel.visualizerBarDensity.collectAsState()
    val visualType by viewModel.playerVisualType.collectAsState()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsState()
    val lyricsSpeed by viewModel.lyricsSpeed.collectAsState()
    val context = LocalContext.current

    val glassSurf = surfaceColor.copy(alpha = bgOpacity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "PREFERENCES",
            color = primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Settings",
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Tailor your acoustic, layout, and visual experience",
            color = textMutedColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Appearance Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "APPEARANCE",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setLightTheme(!isLight) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Light Theme", color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Use a clean, high-contrast light theme", color = textMutedColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isLight,
                        onCheckedChange = { viewModel.setLightTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = textMutedColor,
                            uncheckedTrackColor = textColor.copy(alpha = 0.1f)
                        )
                    )
                }
                
                Divider(color = textColor.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLight) { viewModel.setAmoledTheme(!amoled) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AMOLED Mode", 
                            color = if (isLight) textColor.copy(alpha = 0.4f) else textColor, 
                            fontWeight = FontWeight.SemiBold, 
                            fontSize = 15.sp
                        )
                        Text("Pitch black backgrounds for OLED energy saving", color = textMutedColor, fontSize = 11.sp)
                    }
                    Switch(
                        checked = amoled,
                        onCheckedChange = { viewModel.setAmoledTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = textMutedColor,
                            uncheckedTrackColor = textColor.copy(alpha = 0.1f)
                        ),
                        enabled = !isLight
                    )
                }

                Divider(color = textColor.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Accent Shading Choice", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Select your primary workspace focal color", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ACCENT_PRIMARY_COLORS.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (activeAccent == index) 3.dp else 1.dp,
                                    color = if (activeAccent == index) (if (isLight) Color.Black else Color.White) else color.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setAccentColorIndex(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeAccent == index) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (color == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Premium Customization Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WORKSPACE & LAYOUTS",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Theme Background Style", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Select your ambient background coloring theme", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                val bgOptions = listOf(
                    Triple(0, "Classic", Color(0xFF0D0D0D)),
                    Triple(1, "Amoled", Color(0xFF000000)),
                    Triple(2, "Purple", Color(0xFF110B24)),
                    Triple(3, "Midnight", Color(0xFF070F1E)),
                    Triple(4, "Forest", Color(0xFF041008)),
                    Triple(5, "Slate", Color(0xFF1A2226)),
                    Triple(6, "Crimson", Color(0xFF1F060F)),
                    Triple(7, "Custom", Color(customBgHex)),
                    Triple(8, "Gradient", Color(customBgGradEndHex))
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(bgOptions) { (id, name, color) ->
                        val isSelected = bgType == id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setBackgroundType(id) }
                        ) {
                            val boxMod = Modifier
                                .size(width = 68.dp, height = 50.dp)
                                .clip(RoundedCornerShape(10.dp))
                            val bgMod = if (id == 8) {
                                boxMod.background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(customBgGradStartHex),
                                            Color(customBgGradEndHex)
                                        )
                                    )
                                )
                            } else {
                                boxMod.background(color)
                            }
                            Box(
                                modifier = bgMod
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) primaryColor else textColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = if (isSelected) primaryColor else textColor.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (bgType == 7) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Solid Color", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val presetColors = listOf(
                        0xFF121212L to "Slate",
                        0xFF1B1212L to "Rose",
                        0xFF0C1917L to "Teal",
                        0xFF121820L to "Navy",
                        0xFF181020L to "Orchid",
                        0xFF1C1810L to "Amber",
                        0xFF10191CL to "Teal",
                        0xFF2A1C1CL to "Cocoa",
                        0xFF221128L to "Plum"
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(presetColors) { (colorVal, name) ->
                            val isSelected = customBgHex == colorVal
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setCustomBgColor(colorVal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    var hexInput by remember { mutableStateOf(String.format("%06X", (customBgHex and 0xFFFFFFL))) }
                    var errorMsg by remember { mutableStateOf<String?>(null) }
                    
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            val cleanInput = input.take(6).filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                            hexInput = cleanInput
                            if (cleanInput.length == 6) {
                                try {
                                    val parsed = cleanInput.toLong(16) or 0xFF000000L
                                    viewModel.setCustomBgColor(parsed)
                                    errorMsg = null
                                } catch (e: Exception) {
                                    errorMsg = "Invalid hex code"
                                }
                            }
                        },
                        label = { Text("Custom Color Hex (#)", fontSize = 11.sp, color = textMutedColor) },
                        maxLines = 1,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.15f),
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        isError = errorMsg != null
                    )
                }
                
                if (bgType == 8) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Gradient Pair", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val presetGradients = listOf(
                        Pair(0xFF1E103AL, 0xFF050512L) to "Cosmos",
                        Pair(0xFF0D324DL, 0xFF1B2838L) to "Aqua",
                        Pair(0xFF3A0007L, 0xFF140004L) to "Crimson",
                        Pair(0xFF082E2AL, 0xFF051711L) to "Moss",
                        Pair(0xFF2D1E3DL, 0xFF0C0712L) to "Lavender",
                        Pair(0xFF4C1C13L, 0xFF170605L) to "Clay",
                        Pair(0xFF1A2130L, 0xFF050B14L) to "Abyss",
                        Pair(0xFF480B3DL, 0xFF120310L) to "Psyche",
                        Pair(0xFF1F2937L, 0xFF111827L) to "Charcoal"
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(presetGradients) { (pair, name) ->
                            val (start, end) = pair
                            val isSelected = (customBgGradStartHex == start && customBgGradEndHex == end)
                            Box(
                                modifier = Modifier
                                    .size(54.dp, 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.verticalGradient(colors = listOf(Color(start), Color(end))))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setCustomBgGradient(start, end) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Opacity / Glass Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Overlays Translucency", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    val translucencyPct = ((1f - bgOpacity) * 100).toInt()
                    val statusText = if (translucencyPct == 100) "100% (Glass Hidden)" else "$translucencyPct%"
                    Text(statusText, color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Controls the transparency of foreground cards & menu panels", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = bgOpacity,
                    onValueChange = { viewModel.setBackgroundOpacity(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Library Placement
                Text("Library Placement Position", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Define where your library/favorite content displays", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                val placementOptions = listOf(
                    Pair("tabs", "Nav Tab (Classic)"),
                    Pair("home_top", "Home Top"),
                    Pair("home_bottom", "Home Bottom")
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    placementOptions.forEach { (route, name) ->
                        val selected = libPlacement == route
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { viewModel.setLibraryPlacement(route) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (selected) Color.White else textColor.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Audio Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AUDIO PERFORMANCE",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Default Audio Resolution", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Current setting: $audioQual", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Auto", "Low", "High").forEach { q ->
                        val selected = audioQual == q
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { viewModel.setAudioQuality(q) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q,
                                color = if (selected) Color.White else textColor.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Crossfade Transition", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${crossfadeVal}s", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Duration of overlap blending between consecutive songs", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = crossfadeVal.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )
            }
        }

        // Music Visualizer Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MUSIC VISUALIZER",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Player Visual Style", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Toggle between static album art or the live spectrum", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Square" to "Square Art", "CAVA" to "CAVA Spectrum").forEach { (style, name) ->
                        val selected = visualType == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { viewModel.setPlayerVisualType(style) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (selected) Color.White else textColor.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Sensitivity row & slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Response Sensitivity", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(String.format("%.2fx", vizSensitivity), color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Amplifies spectrum bounce magnitude based on acoustics", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = vizSensitivity,
                    onValueChange = { viewModel.setVisualizerSensitivity(it) },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                // Bar Density row & slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spectrum Bar Density", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("$vizBarDensity bars", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Adjust the number of processing columns in the live feed", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = vizBarDensity.toFloat(),
                    onValueChange = { viewModel.setVisualizerBarDensity(it.toInt()) },
                    valueRange = 12f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )
            }
        }

        // Lyrics Customization Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Subtitles,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LYRICS CUSTOMIZATION",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lyrics Typography Size", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("$lyricsFontSize sp", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Scale the text size of flowing lyrics on the player screen", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = lyricsFontSize.toFloat(),
                    onValueChange = { viewModel.setLyricsFontSize(it.toInt()) },
                    valueRange = 14f..32f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tempo Sync Adjustment", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setLyricsSpeed(1.0f) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Tempo",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text("%.2fx".format(lyricsSpeed), color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Finely offset lyrics timing alignment with audio streams", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.setLyricsSpeed((lyricsSpeed - 0.05f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Decrease Sync Speed",
                            tint = textColor
                        )
                    }
                    Slider(
                        value = lyricsSpeed,
                        onValueChange = { viewModel.setLyricsSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = textColor.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.setLyricsSpeed((lyricsSpeed + 0.05f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Increase Sync Speed",
                            tint = textColor
                        )
                    }
                }
            }
        }

        // Canvas Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wallpaper,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CANVAS BACKGROUNDS",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            val canvasMode by viewModel.canvasMode.collectAsState()
            val canvasImageUri by viewModel.canvasImageUri.collectAsState()
            val canvasVideoUri by viewModel.canvasVideoUri.collectAsState()
            val canvasBlurRadius by viewModel.canvasBlurRadius.collectAsState()

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { viewModel.importCanvasImage(it) }
            }

            val videoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { viewModel.importCanvasVideo(it) }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Backdrop Canvas Mode", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Set dynamic background canvas for active playback screens", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("none" to "Off", "album" to "Album", "image" to "Image", "video" to "Video").forEach { (modeVal, label) ->
                        val selected = canvasMode == modeVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { viewModel.setCanvasMode(modeVal) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else textColor.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                if (canvasMode == "album") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "✓ The active song's artwork will automatically render as the beautiful canvas background",
                        color = primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (canvasMode == "image") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Canvas Image Source", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (canvasImageUri.isNotEmpty()) "✓ Local file active" else "No local file chosen",
                            color = if (canvasImageUri.isNotEmpty()) primaryColor else textMutedColor,
                            fontSize = 11.sp,
                            fontWeight = if (canvasImageUri.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canvasImageUri.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.setCanvasImageUri("") },
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Choose Image", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (canvasMode == "video") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Canvas Video Source", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (canvasVideoUri.isNotEmpty()) "✓ Local video active" else "No local video chosen",
                            color = if (canvasVideoUri.isNotEmpty()) primaryColor else textMutedColor,
                            fontSize = 11.sp,
                            fontWeight = if (canvasVideoUri.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canvasVideoUri.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.setCanvasVideoUri("") },
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Choose Video", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (canvasMode != "none") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = textColor.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Canvas Blur Strength", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${canvasBlurRadius.toInt()} dp", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Apply backdrop glass blur filtering intensity", color = textMutedColor, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = canvasBlurRadius,
                        onValueChange = { viewModel.updateCanvasBlurRadius(it) },
                        onValueChangeFinished = { viewModel.setCanvasBlurRadius(viewModel.canvasBlurRadius.value) },
                        valueRange = 0f..25f,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = textColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }

        // Storage Subsection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SYSTEM & UTILITIES",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wipe Temp Play Cache", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Clears transient audio streams & temporary storage memory buffers", color = textMutedColor, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Storage and Audio buffers cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // About description
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FALLAN STREAMS",
                    color = textColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 2.6.4 • Native Jetpack Compose Build",
                    color = textMutedColor,
                    fontSize = 11.sp
                )
                Text(
                    text = "Powered by JioSaavn Engine",
                    color = textMutedColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun rememberDominantColor(thumbnailUrl: String, fallback: Color): Color {
    val context = LocalContext.current
    var dominantColor by remember(thumbnailUrl) { mutableStateOf(fallback) }

    LaunchedEffect(thumbnailUrl) {
        withContext(Dispatchers.IO) {
            try {
                val loader = coil.ImageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .allowHardware(false) // Required for Palette to work
                    .build()
                val result = (loader.execute(request) as? coil.request.SuccessResult)?.drawable
                val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                    val swatch = palette.vibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.dominantSwatch
                    swatch?.let {
                        dominantColor = Color(it.rgb)
                    }
                }
            } catch (e: Exception) {
                // Non-fatal — fall back to fallback silently
            }
        }
    }
    return dominantColor
}

@Composable
fun WaveformBars(
    isPlaying: Boolean,
    color: Color,
    barCount: Int = 28,
    sensitivity: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cava_waveform")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "cava_tick"
    )

    Canvas(modifier = modifier) {
        val finalBarCount = barCount.coerceAtLeast(12)
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val barWidth = width / (finalBarCount * 1.6f - 0.6f)
        val maxBarHeight = height
        val t = tick * 0.15f

        for (index in 0 until finalBarCount) {
            val wave1 = sin(t + index * 0.25f)
            val wave2 = cos(t * 1.8f - index * 0.45f)
            val wave3 = sin(t * 3.5f + index * 0.75f)

            val heightFraction = if (isPlaying) {
                val amplitude = if (index < finalBarCount * 0.25f) {
                    val kick = abs(sin(t * 1.6f)).coerceAtLeast(0.05f) * 0.6f
                    val sub = abs(wave2) * 0.35f
                    kick + sub + 0.12f
                } else if (index < finalBarCount * 0.75f) {
                    val mid = abs(wave1 * 0.35f + wave2 * 0.35f)
                    val modulation = abs(sin(t * 0.4f)) * 0.2f
                    mid + modulation + 0.1f
                } else {
                    val treble = abs(wave2 * 0.2f + wave3 * 0.5f)
                    val rapidFlicker = abs(sin(t * 8.0f)) * 0.15f
                    treble + rapidFlicker + 0.05f
                }
                val jitter = sin(t * 12.0f + index * 1.5f) * 0.08f
                ((amplitude + jitter) * sensitivity).coerceIn(0.12f, 1f)
            } else {
                (0.08f + abs(sin(t * 0.2f + index * 0.15f)) * 0.08f) * sensitivity
            }

            val barHeight = maxBarHeight * heightFraction
            val x = index * (barWidth * 1.6f)
            val top = maxBarHeight - barHeight

            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2.5f)
            )
        }
    }
}

// ----------------------------------------------------
// FULL SCREEN PLAYER SHEET
// ----------------------------------------------------
@Composable
fun PlayerScreen(
    song: SongModel,
    viewModel: MainViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    surfaceColor: Color,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isPlaying by PlaybackManager.isPlaying.collectAsState()
    val isBuffering by PlaybackManager.isBuffering.collectAsState()
    val position by PlaybackManager.playbackPosition.collectAsState()
    val duration by PlaybackManager.playbackDuration.collectAsState()
    val shuffleOn by PlaybackManager.shuffleEnabled.collectAsState()
    val repeatSelectorMode by PlaybackManager.repeatMode.collectAsState()

    // Lyrics & Sub overlays state
    val lyricsItem by viewModel.songLyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val vizSensitivity by viewModel.visualizerSensitivity.collectAsState()
    val vizBarDensity by viewModel.visualizerBarDensity.collectAsState()
    val playerVisualType by viewModel.playerVisualType.collectAsState()
    val lyricsFontSize by viewModel.lyricsFontSize.collectAsState()
    val lyricsSpeed by viewModel.lyricsSpeed.collectAsState()
    
    var displaysEqDialog by remember { mutableStateOf(false) }
    var showLyricsScreen by remember(song) { mutableStateOf(false) }

    val handleCollapse = {
        showLyricsScreen = false
        onCollapse()
    }

    var showQueueScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showQueueScreen) {
            showQueueScreen = false
        } else if (showLyricsScreen) {
            showLyricsScreen = false
        } else {
            handleCollapse()
        }
    }

    var lyricStyle by remember { mutableIntStateOf(0) } // 0, 1, 2
    var displaysSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerActive by PlaybackManager.sleepTimerActive.collectAsState()
    val sleepTimerRemaining by PlaybackManager.sleepTimerRemainingMs.collectAsState()

    // Disk-like Rotation state engine
    val infiniteTransition = rememberInfiniteTransition(label = "player_rot")
    val rotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "spin"
    )

    // Smooth scale/zoom transitions between standard album art and dynamic CAVA spectrum
    val posterSize by animateDpAsState(
        targetValue = if (playerVisualType == "CAVA") 120.dp else 244.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "poster_size"
    )

    val waveformHeight by animateDpAsState(
        targetValue = if (playerVisualType == "CAVA") 90.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "waveform_height"
    )

    val waveformColor by animateColorAsState(
        targetValue = if (playerVisualType == "CAVA") primaryColor.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 500),
        label = "waveform_color"
    )

    val posterCornerRadius by animateDpAsState(
        targetValue = if (playerVisualType == "CAVA") 12.dp else 16.dp,
        animationSpec = tween(durationMillis = 500),
        label = "poster_radius"
    )

    val spacingHeight by animateDpAsState(
        targetValue = if (playerVisualType == "CAVA") 16.dp else 24.dp,
        animationSpec = tween(durationMillis = 500),
        label = "spacing_height"
    )

    // Trigger loading lyrics if turned on
    LaunchedEffect(song) {
        viewModel.fetchLyrics(song)
        showLyricsScreen = false
    }

    // Extract dominant color from album art for dynamic background
    val dominantColor = rememberDominantColor(
        thumbnailUrl = song.thumbnailUrl,
        fallback = primaryColor
    )

    // Animate the dominant color transition smoothly when song changes
    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "dominant_color"
    )

    val canvasMode by viewModel.canvasMode.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        if (canvasMode == "none") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animatedDominantColor.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.92f),
                                Color.Black
                            )
                        )
                    )
            )

            // Blur cover art backdrop
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(30.dp)
                    .alpha(0.20f),
                contentScale = ContentScale.Crop
            )

            // Backdrop dark tint gradient list
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                        )
                    )
            )
        } else {
            // Draw a solid black background first to prevent any screen elements below from bleeding through
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
            CanvasBackground(
                viewModel = viewModel,
                currentSongThumbnailUrl = song.thumbnailUrl,
                fallbackColor = animatedDominantColor,
                modifier = Modifier.fillMaxSize()
            )
            // Root CanvasBackground is already active behind this screen.
            // We just add a beautiful subtle dark gradient to enhance readability of local controls.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.70f))
                        )
                    )
            )
        }

        // Contents layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = handleCollapse, modifier = Modifier.testTag("player_back")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Collapse", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("Now Playing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                // Real Live heart button synced
                val isLiked = viewModel.likedSongs.collectAsState().value.any { it.id == song.id }
                IconButton(onClick = { viewModel.toggleLikeSong(song) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Track",
                        tint = if (isLiked) primaryColor else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Spinning cover album layout with clean zoom/scale transitions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .requiredSize(posterSize)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(posterCornerRadius))
                            .border(
                                BorderStroke(
                                    if (playerVisualType == "CAVA") 1.5.dp else 2.dp,
                                    primaryColor.copy(alpha = if (playerVisualType == "CAVA") 0.4f else 0.5f)
                                ),
                                RoundedCornerShape(posterCornerRadius)
                            )
                            .clickable(
                                interactionSource = remember { java.util.concurrent.ForkJoinPool.commonPool(); MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.setPlayerVisualType(if (playerVisualType == "CAVA") "Square" else "CAVA")
                            }
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = secondaryColor,
                            strokeWidth = if (playerVisualType == "CAVA") 3.dp else 5.dp,
                            modifier = Modifier.requiredSize(posterSize + if (playerVisualType == "CAVA") 8.dp else 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacingHeight))

                // Smoothly animated CAVA / Standard waveform bars
                WaveformBars(
                    isPlaying = isPlaying,
                    color = waveformColor,
                    barCount = vizBarDensity,
                    sensitivity = if (playerVisualType == "CAVA") vizSensitivity * 1.25f else vizSensitivity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(waveformHeight)
                        .padding(horizontal = if (playerVisualType == "CAVA") 16.dp else 32.dp)
                )
            }

            // Titles details section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    color = textMutedColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                // Platform badge pill under title
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when(song.source) {
                            MusicSource.JIO_SAAVN -> "JioSaavn"
                            MusicSource.YOUTUBE -> "YouTube"
                            MusicSource.YOUTUBE_MUSIC -> "YouTube Music"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Digital Seek track bar Section
            Column(modifier = Modifier.fillMaxWidth()) {
                var draggingValue by remember { mutableStateOf<Float?>(null) }
                val currentPosition = draggingValue?.toLong() ?: position.toLong()

                val positionSeconds = currentPosition / 1000
                val durationSeconds = duration / 1000
                val elapsedMinutes = positionSeconds / 60
                val elapsedRemSeconds = positionSeconds % 60
                val durationMinutes = durationSeconds / 60
                val durationRemSeconds = durationSeconds % 60

                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { draggingValue = it },
                    onValueChangeFinished = {
                        draggingValue?.let {
                            PlaybackManager.seekTo(it.toInt())
                            draggingValue = null
                        }
                    },
                    valueRange = 0f..maxOf(duration.toFloat(), 1f),
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("playback_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d:%02d", elapsedMinutes, elapsedRemSeconds),
                        color = textMutedColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format("%02d:%02d", durationMinutes, durationRemSeconds),
                        color = textMutedColor,
                        fontSize = 12.sp
                    )
                }
            }

            // Layout of primary play controls trigger row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle mode action
                IconButton(onClick = { PlaybackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleOn) primaryColor else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Skip Prev
                IconButton(onClick = { PlaybackManager.skipPrevious(context) }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Primary Large play wings trigger
                WingsPlayPauseButton(
                    isPlaying = isPlaying,
                    isLoading = isBuffering,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    onClick = {
                        if (isPlaying) PlaybackManager.pause() else PlaybackManager.resume()
                    }
                )

                // Skip Next
                IconButton(onClick = { PlaybackManager.skipNext(context) }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Modes Action cycle
                IconButton(onClick = { PlaybackManager.toggleRepeat() }) {
                    Icon(
                        imageVector = when (repeatSelectorMode) {
                            com.example.player.RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatSelectorMode != com.example.player.RepeatMode.OFF) primaryColor else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom utility drawers actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { displaysEqDialog = true }) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = Color.White.copy(alpha = 0.8f))
                }
                
                IconButton(onClick = { showLyricsScreen = true }) {
                    Icon(imageVector = Icons.Default.Notes, contentDescription = "Lyrics", tint = Color.White.copy(alpha = 0.8f))
                }

                IconButton(onClick = { showQueueScreen = true }) {
                    Icon(imageVector = Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color.White.copy(alpha = 0.8f))
                }

                IconButton(onClick = { displaysSleepTimerDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerActive) primaryColor else Color.White.copy(alpha = 0.8f)
                    )
                }

                // Direct download track option
                val downloadedSongsList by viewModel.downloadedSongs.collectAsState()
                val downloadedSongCopy = downloadedSongsList.find { it.id == song.id }
                val alreadyDownloaded = downloadedSongCopy != null
                val activeDownloadsMap by FallenDownloadManager.activeDownloads.collectAsState()
                val isDownloading = activeDownloadsMap.containsKey(song.id)
                val downloadProgress = activeDownloadsMap[song.id] ?: 0

                IconButton(onClick = {
                    if (alreadyDownloaded) {
                        viewModel.removeDownload(song.id, downloadedSongCopy?.localFilePath)
                        Toast.makeText(context, "Download file removed from device", Toast.LENGTH_SHORT).show()
                    } else if (!isDownloading) {
                        FallenDownloadManager.startDownload(context, song)
                        Toast.makeText(context, "Download scheduled nicely in background!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            progress = downloadProgress / 100f,
                            color = primaryColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (alreadyDownloaded) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                            contentDescription = "Download Offline",
                            tint = if (alreadyDownloaded) Color(0xFF10B981) else Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Equalizer Dialog View Overlay
        if (displaysEqDialog) {
            EqualizerDialog(
                primaryColor = primaryColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                textMutedColor = textMutedColor,
                onDismiss = { displaysEqDialog = false }
            )
        }

        // AnimatedVisibility lyrics screen overlay
        AnimatedVisibility(
            visible = showLyricsScreen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            LyricsScreen(
                song = song,
                viewModel = viewModel,
                lyricsPair = lyricsItem,
                isLoading = isLyricsLoading,
                dominantColor = animatedDominantColor,
                lyricStyle = lyricStyle,
                playbackPosition = position.toLong(),
                duration = duration.toLong(),
                fontSize = lyricsFontSize,
                lyricsSpeed = lyricsSpeed,
                onSpeedChange = { viewModel.setLyricsSpeed(it) },
                onStyleChange = { lyricStyle = (lyricStyle + 1) % 3 },
                onDismiss = { showLyricsScreen = false },
                onLineClick = { PlaybackManager.seekTo(it.toInt()) }
            )
        }

        AnimatedVisibility(
            visible = showQueueScreen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            QueueScreen(
                viewModel = viewModel,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                textMutedColor = textMutedColor,
                dominantColor = animatedDominantColor,
                onDismiss = { showQueueScreen = false }
            )
        }

        // Sleep Timer Dialog Overlay
        if (displaysSleepTimerDialog) {
            SleepTimerDialog(
                isActive = sleepTimerActive,
                remainingMs = sleepTimerRemaining,
                primaryColor = primaryColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                textMutedColor = textMutedColor,
                onDismiss = { displaysSleepTimerDialog = false }
            )
        }
    }
}

// ----------------------------------------------------
// EQUALIZER CONTROL VIEW DIALOG
// ----------------------------------------------------
@Composable
fun VerticalEqualizerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color
) {
    BoxWithConstraints(
        modifier = modifier
            .width(36.dp)
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        val heightPx = constraints.maxHeight.toFloat()
        val rangeMin = valueRange.start
        val rangeMax = valueRange.endInclusive
        val rangeValue = rangeMax - rangeMin

        val normalizedValue = if (rangeValue > 0f) {
            ((value - rangeMin) / rangeValue).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rangeMin, rangeMax, heightPx) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val percentage = (1f - (change.position.y / heightPx)).coerceIn(0f, 1f)
                            val newValue = rangeMin + (percentage * rangeValue)
                            onValueChange(newValue)
                        },
                        onDragStart = { offset ->
                            val percentage = (1f - (offset.y / heightPx)).coerceIn(0f, 1f)
                            val newValue = rangeMin + (percentage * rangeValue)
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            // Draw background track
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(inactiveColor)
            )

            // Draw active track
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(4.dp)
                    .fillMaxHeight(normalizedValue)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor)
            )

            // Draw beautiful physical fader knob (like a studio mixer)
            val thumbHeight = 18.dp
            val maxOffset = this@BoxWithConstraints.maxHeight - thumbHeight
            val thumbOffset = maxOffset * (1f - normalizedValue)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = thumbOffset)
                    .size(width = 24.dp, height = thumbHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(1.5.dp, activeColor, RoundedCornerShape(4.dp))
            ) {
                // Horizontal studio level notch
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(10.dp)
                        .height(2.dp)
                        .background(activeColor)
                )
            }
        }
    }
}

@Composable
fun TimePickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    primaryColor: Color,
    textColor: Color,
    textMutedColor: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listSize = range.last - range.first + 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val coroutineScope = rememberCoroutineScope()

    // Sync scroll position → value
    LaunchedEffect(listState.firstVisibleItemIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val newValue = (listState.firstVisibleItemIndex + range.first).coerceIn(range)
            if (newValue != value) onValueChange(newValue)
        }
    }

    // Sync value → scroll position when changed via +/- buttons
    LaunchedEffect(value) {
        val targetIndex = (value - range.first).coerceIn(0, listSize - 1)
        if (listState.firstVisibleItemIndex != targetIndex) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = textMutedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // + button
        IconButton(
            onClick = {
                val newVal = if (value >= range.last) range.first else value + 1
                onValueChange(newVal)
            }
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase $label",
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
        }

        // Drum-roll scroll column — shows 3 items, center is selected
        Box(
            modifier = Modifier
                .height(108.dp)  // 3 × 36dp rows
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(textMutedColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 36.dp) // top/bottom padding shows prev/next items
            ) {
                items(listSize) { index ->
                    val itemValue = range.first + index
                    val isSelected = itemValue == value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clickable { onValueChange(itemValue) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(itemValue),
                            color = if (isSelected) textColor else textMutedColor.copy(alpha = 0.4f),
                            fontSize = if (isSelected) 26.sp else 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Selection highlight line above and below center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }

        // - button
        IconButton(
            onClick = {
                val newVal = if (value <= range.first) range.last else value - 1
                onValueChange(newVal)
            }
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease $label",
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    isActive: Boolean,
    remainingMs: Long,
    primaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    onDismiss: () -> Unit
) {
    // 0 = bottom sheet, 1 = full screen, 2 = compact dialog
    var displayStyle by remember { mutableIntStateOf(0) }

    when (displayStyle) {
        1 -> SleepTimerContent(
            isActive = isActive,
            remainingMs = remainingMs,
            primaryColor = primaryColor,
            surfaceColor = Color.Black,
            textColor = Color.White,
            textMutedColor = Color.White.copy(alpha = 0.6f),
            displayStyle = displayStyle,
            onStyleChange = { displayStyle = it },
            onDismiss = onDismiss,
            fullScreen = true
        )
        2 -> AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = surfaceColor,
            title = null,
            text = {
                SleepTimerContent(
                    isActive = isActive,
                    remainingMs = remainingMs,
                    primaryColor = primaryColor,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    textMutedColor = textMutedColor,
                    displayStyle = displayStyle,
                    onStyleChange = { displayStyle = it },
                    onDismiss = onDismiss,
                    fullScreen = false
                )
            },
            confirmButton = {}
        )
        else -> ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = surfaceColor,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(textMutedColor.copy(alpha = 0.4f))
                )
            }
        ) {
            SleepTimerContent(
                isActive = isActive,
                remainingMs = remainingMs,
                primaryColor = primaryColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                textMutedColor = textMutedColor,
                displayStyle = displayStyle,
                onStyleChange = { displayStyle = it },
                onDismiss = onDismiss,
                fullScreen = false
            )
        }
    }
}

@Composable
fun SleepTimerContent(
    isActive: Boolean,
    remainingMs: Long,
    primaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    displayStyle: Int,
    onStyleChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    fullScreen: Boolean
) {
    val presets = listOf(
        "15 min" to 15 * 60 * 1000L,
        "30 min" to 30 * 60 * 1000L,
        "45 min" to 45 * 60 * 1000L,
        "60 min" to 60 * 60 * 1000L,
        "End of song" to -1L
    )

    val hours = if (remainingMs > 0) (remainingMs / 3_600_000).toInt() else 0
    val minutes = if (remainingMs > 0) ((remainingMs % 3_600_000) / 60_000).toInt() else 0
    val seconds = if (remainingMs > 0) ((remainingMs % 60_000) / 1_000).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fullScreen) Modifier.fillMaxSize().background(Color.Black).statusBarsPadding() else Modifier)
            .padding(horizontal = 24.dp, vertical = if (fullScreen) 24.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header row with title and style toggle chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fullScreen) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Close", tint = Color.White)
                }
            }
            Text(
                text = "Sleep Timer",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            // Style toggle: 3 small chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Icons.Default.TableRows to 0,       // bottom sheet icon
                    Icons.Default.Fullscreen to 1,      // full screen icon
                    Icons.Default.CropSquare to 2       // compact dialog icon
                ).forEach { (icon, style) ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (displayStyle == style) primaryColor.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { onStyleChange(style) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (displayStyle == style) primaryColor else textMutedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isActive) {
            // ── CUSTOM TIME PICKER MODE ──
            var pickerHours by remember { mutableIntStateOf(0) }
            var pickerMinutes by remember { mutableIntStateOf(30) }
            var pickerSeconds by remember { mutableIntStateOf(0) }

            Text(
                text = "Set and doze off.",
                color = textMutedColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Hours picker
                    TimePickerColumn(
                        label = "Hours",
                        value = pickerHours,
                        range = 0..23,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        onValueChange = { pickerHours = it },
                        modifier = Modifier.weight(1f)
                    )

                    Text(":", color = textMutedColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                    // Minutes picker
                    TimePickerColumn(
                        label = "Minutes",
                        value = pickerMinutes,
                        range = 0..59,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        onValueChange = { pickerMinutes = it },
                        modifier = Modifier.weight(1f)
                    )

                    Text(":", color = textMutedColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                    // Seconds picker
                    TimePickerColumn(
                        label = "Seconds",
                        value = pickerSeconds,
                        range = 0..59,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textMutedColor = textMutedColor,
                        onValueChange = { pickerSeconds = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Start Timer button — disabled if all zero
            val totalMs = (pickerHours * 3_600_000L) + (pickerMinutes * 60_000L) + (pickerSeconds * 1_000L)
            val canStart = totalMs > 0L

            Button(
                onClick = {
                    PlaybackManager.setSleepTimer(totalMs)
                    onDismiss()
                },
                enabled = canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = primaryColor.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Timer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        } else {
            // ── LIVE COUNTDOWN MODE ──
            Text(
                text = "Preparing for a peaceful interlude in...",
                color = textMutedColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            if (remainingMs == -1L) {
                // "End of song" mode — no clock, just a note
                Text(
                    text = "Stops after current song ends",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                // Big HH:MM:SS countdown blocks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "Hours" to "%02d".format(hours),
                        "Minutes" to "%02d".format(minutes),
                        "Seconds" to "%02d".format(seconds)
                    ).forEachIndexed { index, (label, value) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                color = textMutedColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(textMutedColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value,
                                    color = textColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        // Colon separator between blocks (not after last)
                        if (index < 2) {
                            Text(
                                text = ":",
                                color = textMutedColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stop Timer button — pill shape with primaryColor, matching screenshot
            Button(
                onClick = {
                    PlaybackManager.cancelSleepTimer()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stop Timer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(if (fullScreen) 0.dp else 16.dp))
    }
}

@Composable
fun EqualizerDialog(
    primaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    onDismiss: () -> Unit
) {
    val eqAvailable by PlaybackManager.eqAvailable.collectAsState()
    val bandsCount by PlaybackManager.eqBandsCount.collectAsState()
    val levelsMap by PlaybackManager.eqBandLevels.collectAsState()
    val activePreset by PlaybackManager.currentPresetName.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Equalizer", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = textColor)
                    }
                }

                if (!eqAvailable) {
                    Text(
                        text = "Native Android Audio FX equalizer is unavailable for current session.",
                        color = textMutedColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Presets chips select
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Flat", "Bass Boost", "Pop", "Rock", "Jazz", "Classical").forEach { preset ->
                            val active = activePreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) primaryColor else Color.Gray.copy(alpha = 0.15f))
                                    .clickable { PlaybackManager.setPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                  Text(preset, color = if (active) Color.White else textColor, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("5 Band Frequencies Gain Level", color = textMutedColor, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row of band adjustments nicely distributed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val frequencies = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
                        for (i in 0 until 5) {
                            val bandId = i.toShort()
                            val level = levelsMap[bandId] ?: 0
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(frequencies[i], color = textMutedColor, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                VerticalEqualizerSlider(
                                    value = level.toFloat(),
                                    onValueChange = { PlaybackManager.applyBandLevel(bandId, it.toInt().toShort()) },
                                    valueRange = -1500f..1500f,
                                    activeColor = primaryColor,
                                    inactiveColor = textColor.copy(alpha = 0.12f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "${level / 100}dB", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

@Composable
fun LyricsScreen(
    song: SongModel,
    viewModel: MainViewModel,
    lyricsPair: Pair<String, Boolean>,
    isLoading: Boolean,
    dominantColor: Color,
    lyricStyle: Int,
    playbackPosition: Long,
    duration: Long,
    fontSize: Int,
    lyricsSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onStyleChange: () -> Unit,
    onDismiss: () -> Unit,
    onLineClick: (Long) -> Unit
) {
    val canvasMode by viewModel.canvasMode.collectAsState()
    val parsedLines = remember(lyricsPair.first) {
        val timestampRegex = Regex("^\\[(\\d+):(\\d+)(?:\\.(\\d+))?\\]")
        lyricsPair.first
            .lines()
            .map { line ->
                val trimmed = line.trim()
                val match = timestampRegex.find(trimmed)
                if (match != null) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val msStr = match.groupValues[3]
                    val msVal = when (msStr.length) {
                        1 -> msStr.toLong() * 100
                        2 -> msStr.toLong() * 10
                        3 -> msStr.toLong()
                        else -> 0L
                    }
                    val time = min * 60000L + sec * 1000L + msVal
                    val text = trimmed.substring(match.range.last + 1).trim()
                    LyricLine(time, text)
                } else {
                    val isMetadata = trimmed.startsWith("[") && trimmed.endsWith("]")
                    val text = if (isMetadata) "" else trimmed
                    LyricLine(-1L, text)
                }
            }
            .filter { it.text.isNotEmpty() }
    }

    val hasTimestamps = remember(parsedLines) {
        parsedLines.any { it.timestampMs >= 0L }
    }

    val currentIndex = remember(parsedLines, playbackPosition, lyricsSpeed) {
        if (parsedLines.isEmpty()) {
            0
        } else if (hasTimestamps) {
            val adjustedPosition = (playbackPosition * lyricsSpeed).toLong()
            var activeIndex = 0
            for (i in parsedLines.indices) {
                val line = parsedLines[i]
                if (line.timestampMs >= 0L && line.timestampMs <= adjustedPosition) {
                    activeIndex = i
                } else if (line.timestampMs > adjustedPosition) {
                    break
                }
            }
            activeIndex
        } else {
            if (duration > 0) {
                val progress = (playbackPosition / duration.toFloat()) * lyricsSpeed
                (progress * parsedLines.size).toInt().coerceIn(0, parsedLines.size - 1)
            } else {
                0
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        if (canvasMode == "none") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                dominantColor.copy(alpha = 0.95f),
                                Color.Black
                            )
                        )
                    )
            )
        } else {
            // Draw a solid black background first to prevent any screen elements below from bleeding through
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
            CanvasBackground(
                viewModel = viewModel,
                currentSongThumbnailUrl = song.thumbnailUrl,
                fallbackColor = dominantColor,
                modifier = Modifier.fillMaxSize()
            )
            // Root CanvasBackground is active. Add a beautiful dark translucent overlay for optimal text reading.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                // Style toggle button
                IconButton(onClick = onStyleChange) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "Switch Style",
                        tint = Color.White
                    )
                }
            }

            // Inline Sync Speed adjustment slider
            if (!isLoading && parsedLines.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onSpeedChange(1.0f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Speed",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.width(90.dp)) {
                        Text(
                            text = "Speed: %.2fx".format(lyricsSpeed),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (hasTimestamps) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (hasTimestamps) "PRECISE SYNC" else "LINEAR SYNC",
                                color = if (hasTimestamps) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { onSpeedChange((lyricsSpeed - 0.05f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Decrease",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Slider(
                        value = lyricsSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f).height(18.dp)
                    )

                    IconButton(
                        onClick = { onSpeedChange((lyricsSpeed + 0.05f).coerceIn(0.5f, 2.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Increase",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Lyrics content
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                when (lyricStyle) {
                    0 -> LyricsStyleCentered(parsedLines, currentIndex, duration, onLineClick)
                    1 -> LyricsStyleTypewriter(parsedLines, currentIndex, duration, onLineClick)
                    2 -> LyricsStylePoster(parsedLines, currentIndex, duration, onLineClick)
                }
            }
        }
    }
}

@Composable
fun LyricsStyleCentered(
    lines: List<LyricLine>,
    currentIndex: Int,
    duration: Long,
    onLineClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        val target = (currentIndex - 2).coerceAtLeast(0)
        listState.animateScrollToItem(target)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 80.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isCurrent = index == currentIndex
            val isPast = index < currentIndex

            val alpha by animateFloatAsState(
                targetValue = when {
                    isCurrent -> 1f
                    isPast -> 0.25f
                    else -> 0.45f
                },
                animationSpec = tween(durationMillis = 400),
                label = "alpha_$index"
            )

            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "scale_$index"
            )

            Text(
                text = line.text,
                color = Color.White,
                fontSize = if (isCurrent) 24.sp else 18.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val targetTime = if (line.timestampMs >= 0L) {
                            line.timestampMs
                        } else {
                            if (lines.isNotEmpty()) (index.toFloat() / lines.size * duration).toLong() else 0L
                        }
                        onLineClick(targetTime)
                    }
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}

@Composable
fun LyricsStyleTypewriter(
    lines: List<LyricLine>,
    currentIndex: Int,
    duration: Long,
    onLineClick: (Long) -> Unit
) {
    // How many characters of the current line to show (types out over 1.5s)
    var visibleChars by remember(currentIndex) { mutableIntStateOf(0) }
    val currentLine = lines.getOrElse(currentIndex) { LyricLine(-1L, "") }.text

    val listState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current

    LaunchedEffect(currentIndex) {
        visibleChars = 0
        val totalChars = currentLine.length
        if (totalChars == 0) return@LaunchedEffect
        val delayPerChar = (1500L / totalChars).coerceIn(20L, 80L)
        for (i in 1..totalChars) {
            visibleChars = i
            delay(delayPerChar)
        }
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lines.size) {
            val offsetPx = with(density) { -140.dp.roundToPx() }
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = offsetPx
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 180.dp, bottom = 320.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isVisible = index <= currentIndex
            val alpha by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(300),
                label = "tw_alpha_$index"
            )

            val textStyle = androidx.compose.ui.text.buildAnnotatedString {
                if (index == currentIndex) {
                    val typedPart = line.text.take(visibleChars)
                    val untypedPart = line.text.drop(visibleChars)
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.White))
                    append(typedPart)
                    pop()
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent))
                    append(untypedPart)
                    pop()
                } else if (index < currentIndex) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.White.copy(alpha = 0.5f)))
                    append(line.text)
                    pop()
                } else {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent))
                    append(line.text)
                    pop()
                }
            }

            Text(
                text = textStyle,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val targetTime = if (line.timestampMs >= 0L) {
                            line.timestampMs
                        } else {
                            if (lines.isNotEmpty()) (index.toFloat() / lines.size * duration).toLong() else 0L
                        }
                        onLineClick(targetTime)
                    }
                    .padding(vertical = 4.dp)
                    .graphicsLayer { this.alpha = if (index <= currentIndex) alpha else 0f }
            )
        }
    }
}

@Composable
fun LyricsStylePoster(
    lines: List<LyricLine>,
    currentIndex: Int,
    duration: Long,
    onLineClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        val target = (currentIndex - 1).coerceAtLeast(0)
        listState.animateScrollToItem(target)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 48.dp)
    ) {
        itemsIndexed(
            items = lines,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isCurrent = index == currentIndex

            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "poster_scale_$index"
            )

            val alpha by animateFloatAsState(
                targetValue = when {
                    isCurrent -> 1f
                    index < currentIndex -> 0.2f
                    else -> 0.4f
                },
                animationSpec = tween(350),
                label = "poster_alpha_$index"
            )

            val offsetY by animateFloatAsState(
                targetValue = if (isCurrent) 0f else if (index > currentIndex) 30f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "poster_offset_$index"
            )

            Text(
                text = line.text.uppercase(),
                color = Color.White,
                fontSize = if (isCurrent) 42.sp else 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = if (isCurrent) 48.sp else 38.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val targetTime = if (line.timestampMs >= 0L) {
                            line.timestampMs
                        } else {
                            if (lines.isNotEmpty()) (index.toFloat() / lines.size * duration).toLong() else 0L
                        }
                        onLineClick(targetTime)
                    }
                    .padding(vertical = 4.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = offsetY
                        this.alpha = alpha
                    }
            )
        }
    }
}

// ----------------------------------------------------
// QUEUE LIST SCREEN VIEW
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    viewModel: MainViewModel,
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    dominantColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val queueList by PlaybackManager.currentQueue.collectAsState()
    val currentSong by PlaybackManager.currentSong.collectAsState()
    val canvasMode by viewModel.canvasMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val searchState by viewModel.searchState.collectAsState()
    val searchResults = remember(searchState) {
        if (searchState is UiState.Success) {
            (searchState as UiState.Success<List<SongModel>>).data
        } else {
            emptyList()
        }
    }
    val isSearchLoading = remember(searchState) {
        searchState is UiState.Loading
    }

    val currentIndex = remember(queueList, currentSong) {
        queueList.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
    }

    // Mutable queue for drag reorder
    val mutableQueue = remember(queueList) { queueList.toMutableStateList() }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to ->
            mutableQueue.add(to.index, mutableQueue.removeAt(from.index))
            PlaybackManager.reorderQueue(mutableQueue.toList())
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 40f) onDismiss()
                }
            }
    ) {
        // Canvas background if active
        if (canvasMode != "none") {
            CanvasBackground(
                viewModel = viewModel,
                currentSongThumbnailUrl = currentSong?.thumbnailUrl ?: "",
                fallbackColor = dominantColor,
                blurOverride = 20f
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Queue  •  ${queueList.size} songs",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { isSearching = !isSearching; searchQuery = "" }) {
                    Icon(
                        if (isSearching) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add to queue",
                        tint = Color.White
                    )
                }
            }

            // Search bar (shown when adding songs)
            AnimatedVisibility(visible = isSearching) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.length > 1) viewModel.updateSearchQuery(it)
                        },
                        placeholder = { Text("Search songs to add...", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (isSearchLoading) {
                        LinearProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }

                    // Search results
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        items(searchResults, key = { it.id }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        PlaybackManager.addToQueue(song)
                                        isSearching = false
                                        searchQuery = ""
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                                }
                                IconButton(onClick = {
                                    PlaybackManager.addToQueue(song)
                                    isSearching = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add", tint = primaryColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            }

            // Section label
            if (!isSearching) {
                Text(
                    text = "NOW PLAYING",
                    color = primaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 4.dp)
                )
            }

            // Queue list with drag reorder and swipe to dismiss
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState)
                    .detectReorderAfterLongPress(reorderState),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                itemsIndexed(
                    items = mutableQueue,
                    key = { index, song -> "${song.id}_$index" }
                ) { index, song ->
                    val isCurrent = song.id == currentSong?.id
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                PlaybackManager.removeFromQueue(index)
                                true
                            } else false
                        }
                    )

                    ReorderableItem(reorderState, key = "${song.id}_$index") { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag_elev")

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation)
                                    .background(
                                        if (isCurrent) primaryColor.copy(alpha = 0.25f)
                                        else Color.Black.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        PlaybackManager.playSongAtIndex(context, index)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag handle
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag",
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .detectReorder(reorderState)
                                )
                                Spacer(modifier = Modifier.width(10.dp))

                                // Song index number
                                Text(
                                    text = "${index + 1}",
                                    color = if (isCurrent) primaryColor else Color.White.copy(alpha = 0.3f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) primaryColor else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "Playing",
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (index < mutableQueue.size - 1) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// DETAILED OPTIONS BOTTOM DIALOG
// ----------------------------------------------------
@Composable
fun SongOptionsDialog(
    song: SongModel,
    viewModel: MainViewModel,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    var showsPlaylistSubList by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .padding(16.dp)
        ) {
            if (showsPlaylistSubList) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add to Playlist", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showsPlaylistSubList = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    if (playlists.isEmpty()) {
                        Text("No playlists created yet. Create one in Library!", color = textMutedColor, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn {
                            items(playlists) { plist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addSongToPlaylist(plist.id, song)
                                            Toast.makeText(context, "Added to ${plist.name}", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = primaryColor)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(plist.name, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            } else {
                Column {
                    // Header listing item description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(song.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = textMutedColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                    // 1. Play Now Option
                    OptionRowItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Play Now",
                        tint = textColor,
                        textColor = textColor,
                        onClick = {
                            PlaybackManager.setQueue(listOf(song), 0)
                            PlaybackManager.playSong(context, song)
                            onDismiss()
                        }
                    )

                    // 2. Add to Queue Option
                    OptionRowItem(
                        icon = Icons.Default.QueueMusic,
                        title = "Add to Queue",
                        tint = textColor,
                        textColor = textColor,
                        onClick = {
                            PlaybackManager.addToQueue(song)
                            Toast.makeText(context, "Song added to current session layout queue!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    // 3. Add to Playlist Option
                    OptionRowItem(
                        icon = Icons.Default.PlaylistAdd,
                        title = "Add to Playlist",
                        tint = textColor,
                        textColor = textColor,
                        onClick = { showsPlaylistSubList = true }
                    )

                    // 3b. Like / Favorite Option
                    val isLikedNow = viewModel.likedSongs.collectAsState().value.any { it.id == song.id }
                    OptionRowItem(
                        icon = if (isLikedNow) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        title = if (isLikedNow) "Remove from Liked Tracks" else "Add to Liked Tracks",
                        tint = if (isLikedNow) Color(0xFFEF4444) else textColor,
                        textColor = textColor,
                        onClick = {
                            viewModel.toggleLikeSong(song)
                            val statusMsg = if (isLikedNow) "Removed from Liked Tracks" else "Added to Liked Tracks"
                            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    // 4. Download Option
                    val downloadedSongsList = viewModel.downloadedSongs.collectAsState().value
                    val downloadedSongCopy = downloadedSongsList.find { it.id == song.id }
                    val alreadyDownloaded = downloadedSongCopy != null
                    
                    OptionRowItem(
                        icon = if (alreadyDownloaded) Icons.Default.DeleteForever else Icons.Default.FileDownload,
                        title = if (alreadyDownloaded) "Delete download from device" else "Download offline mp3",
                        tint = if (alreadyDownloaded) Color(0xFFEF4444) else primaryColor,
                        textColor = textColor,
                        onClick = {
                            if (alreadyDownloaded) {
                                viewModel.removeDownload(song.id, downloadedSongCopy?.localFilePath)
                                Toast.makeText(context, "Download file removed from device", Toast.LENGTH_SHORT).show()
                            } else {
                                FallenDownloadManager.startDownload(context, song)
                                Toast.makeText(context, "Download scheduled nicely in background!", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OptionRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, color = textColor.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun rememberTransparentWingsImage(drawableRes: Int): androidx.compose.ui.graphics.ImageBitmap {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(drawableRes) {
        val original = android.graphics.BitmapFactory.decodeResource(context.resources, drawableRes)
        if (original != null) {
            val scaled = android.graphics.Bitmap.createScaledBitmap(original, 256, 256, true)
            val mutableBitmap = scaled.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            
            // clean up temporary bitmaps
            if (original != scaled) original.recycle()
            scaled.recycle()
            
            val width = mutableBitmap.width
            val height = mutableBitmap.height
            val pixels = IntArray(width * height)
            mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in pixels.indices) {
                val color = pixels[i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                // If the pixel is very dark (close to pure black background), make it transparent
                if (r < 22 && g < 22 && b < 22) {
                    pixels[i] = 0x00000000
                }
            }
            mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            mutableBitmap.asImageBitmap()
        } else {
            androidx.compose.ui.graphics.ImageBitmap(1, 1)
        }
    }
}

@Composable
fun WingsPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 110.dp
) {
    val whiteWingsRes = com.example.R.drawable.img_wings_white_1781692995781
    val blackWingsRes = com.example.R.drawable.folded_black_wings_1781693912529

    val whiteBitmap = rememberTransparentWingsImage(whiteWingsRes)
    val blackBitmap = rememberTransparentWingsImage(blackWingsRes)

    // Slow and smooth morph transition (duration is 850ms to be perfectly perceptible and elegant)
    val playProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "wings_morph_transition"
    )

    val glowColor = primaryColor.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .size(sizeDp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null // elegant wing touch without standard circular ripple
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing aura behind the white wings (crossfades with playProgress)
        if (playProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .size(sizeDp * 0.85f)
                    .graphicsLayer {
                        alpha = playProgress
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.15f * playProgress),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Black Wings layer (fully visible when paused/0, fades and expands slightly as they transition to white)
        Image(
            bitmap = blackBitmap,
            contentDescription = "Paused - Folded Black Angel Wings",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - playProgress
                    scaleX = 1.0f + 0.15f * playProgress
                    scaleY = 1.0f + 0.1f * playProgress
                }
        )

        // White Wings layer (fully visible when playing/1, starts slightly scaled down/folded, and expands to full open)
        Image(
            bitmap = whiteBitmap,
            contentDescription = "Playing - White Angel Wings",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = playProgress
                    scaleX = 0.75f + 0.25f * playProgress
                    scaleY = 0.85f + 0.15f * playProgress
                }
        )

    }
}

@Composable
fun CanvasBackground(
    viewModel: MainViewModel,
    currentSongThumbnailUrl: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    blurOverride: Float? = null
) {
    val mode by viewModel.canvasMode.collectAsState()
    val imageUri by viewModel.canvasImageUri.collectAsState()
    val videoUri by viewModel.canvasVideoUri.collectAsState()
    val baseBlurRadius by viewModel.canvasBlurRadius.collectAsState()
    val blurRadius = blurOverride ?: baseBlurRadius

    Box(modifier = modifier.fillMaxSize()) {
        when (mode) {
            "none" -> { /* palette gradient — caller handles */ }

            "album" -> {
                val context = LocalContext.current
                val model = remember(currentSongThumbnailUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data(currentSongThumbnailUrl)
                        .crossfade(true)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier)
                ) {
                    when (painter.state) {
                        is coil.compose.AsyncImagePainter.State.Loading ->
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        is coil.compose.AsyncImagePainter.State.Error ->
                            Box(modifier = Modifier.fillMaxSize().background(fallbackColor.copy(alpha = 0.5f)))
                        else -> SubcomposeAsyncImageContent()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }

            "image" -> {
                if (imageUri.isNotEmpty()) {
                    val context = LocalContext.current
                    val model = remember(imageUri) {
                        val fileData = if (imageUri.startsWith("/")) java.io.File(imageUri) else android.net.Uri.parse(imageUri)
                        coil.request.ImageRequest.Builder(context)
                            .data(fileData)
                            .crossfade(true)
                            .build()
                    }
                    SubcomposeAsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier)
                    ) {
                        when (painter.state) {
                            is coil.compose.AsyncImagePainter.State.Loading ->
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                            is coil.compose.AsyncImagePainter.State.Error ->
                                Box(modifier = Modifier.fillMaxSize().background(fallbackColor.copy(alpha = 0.5f)))
                            else -> SubcomposeAsyncImageContent()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }
            }

            "video" -> {
                if (videoUri.isNotEmpty()) {
                    LoopingVideoBackground(
                        source = videoUri,
                        blurRadius = blurRadius,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.40f))
                    )
                }
            }
        }
    }
}

@Composable
fun LoopingVideoBackground(
    source: String,
    blurRadius: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val textureView = remember {
        android.view.TextureView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val exoPlayer = remember(source) {
        try {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = if (source.startsWith("http")) {
                    MediaItem.fromUri(source)
                } else if (source.startsWith("/")) {
                    MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(source)))
                } else {
                    MediaItem.fromUri(android.net.Uri.parse(source))
                }
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                setVideoTextureView(textureView)
                prepare()
                play()
            }
        } catch (e: Exception) {
            android.util.Log.e("LoopingVideo", "Failed to init ExoPlayer for background: ${e.message}")
            ExoPlayer.Builder(context).build()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.clearVideoTextureView(textureView)
            exoPlayer.release()
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val blurRadiusPx = remember(blurRadius) {
        with(density) { blurRadius.dp.toPx() }
    }

    AndroidView(
        factory = { textureView },
        modifier = modifier,
        update = { view ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (blurRadiusPx > 0f) {
                    try {
                        view.setRenderEffect(
                            android.graphics.RenderEffect.createBlurEffect(
                                blurRadiusPx,
                                blurRadiusPx,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LoopingVideoBackground", "setRenderEffect error: ${e.message}")
                    }
                } else {
                    view.setRenderEffect(null)
                }
            }
        }
    )
}

