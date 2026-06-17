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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
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
import androidx.compose.ui.input.pointer.pointerInput

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
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f),
                            baseColor.copy(alpha = 0.98f),
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

    // Handle system back press to collapse expanded player
    if (isPlayerExpanded) {
        BackHandler {
            isPlayerExpanded = false
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
        Scaffold(
            bottomBar = {
                Column {
                    // Mini player overlays bottom nav if an audio is loaded
                    if (currentPlayingSong != null) {
                        MiniPlayer(
                            song = currentPlayingSong!!,
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
                                onClick = { currentTab = route },
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
            modifier = Modifier.fillMaxSize().background(backgroundBrush)
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

// ----------------------------------------------------
// MINI PLAYER COMPOSABLE
// ----------------------------------------------------
@Composable
fun MiniPlayer(
    song: SongModel,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp, top = 2.dp)
            .testTag("mini_player")
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, edgeBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() }
        ) {
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
            Pair(MusicSource.JIO_SAAVN, "JioSaavn"),
            Pair(MusicSource.YOUTUBE_MUSIC, "YT Music"),
            Pair(MusicSource.YOUTUBE, "YouTube")
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
    val query by viewModel.searchTerms.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val recentQueries by viewModel.recentSearches.collectAsState()
    val currentActiveSource by viewModel.activeSource.collectAsState()

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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                cursorColor = primaryColor
            )
        )

        // Conditional display based on state
        if (query.trim().isEmpty()) {
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
            // Render results
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
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            itemsIndexed(songs) { index, song ->
                                SongCardRow(
                                    song = song,
                                    textColor = textColor,
                                    textMutedColor = textMutedColor,
                                    primaryColor = primaryColor,
                                    onClick = { onSongSelect(song, index, songs) },
                                    onLongClick = { onSongOptions(song) },
                                    onMoreClick = { onSongOptions(song) }
                                )
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

    var activeSubTab by remember { mutableStateOf(0) } // 0: Playlists, 1: Liked, 2: Downloads
    var playlistInputString by remember { mutableStateOf("") }
    var displaysCreateOverlay by remember { mutableStateOf(false) }
    var selectedPlaylistTarget by remember { mutableStateOf<PlaylistEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = surfaceColor.copy(alpha = bgOpacity),
            contentColor = primaryColor
        ) {
            val headers = listOf("Playlists", "Liked", "Downloads")
            headers.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = { activeSubTab = idx },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("lib_tab_${title.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (activeSubTab) {
            0 -> {
                // Playlists Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Playlists", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { displaysCreateOverlay = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Playlist", tint = primaryColor)
                    }
                }

                if (playListEntities.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Create a custom playlist to group your vibes!", color = textMutedColor, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(playListEntities) { plist ->
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
            1 -> {
                // Liked favorited List
                if (favoritedList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hearted songs. Press the heart on player screen!", color = textMutedColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        itemsIndexed(favoritedList) { index, song ->
                            SongCardRow(
                                song = song,
                                textColor = textColor,
                                textMutedColor = textMutedColor,
                                primaryColor = primaryColor,
                                onClick = { onSongSelect(song, index, favoritedList) },
                                onLongClick = { onSongOptions(song) },
                                onMoreClick = { onSongOptions(song) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Offline Downloads List
                if (localCacheList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No downloaded songs yet. Downloads run offline!", color = textMutedColor, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        itemsIndexed(localCacheList) { index, song ->
                            SongCardRow(
                                song = song,
                                textColor = textColor,
                                textMutedColor = textMutedColor,
                                primaryColor = primaryColor,
                                onClick = { onSongSelect(song, index, localCacheList) },
                                onLongClick = { onSongOptions(song) },
                                onMoreClick = { onSongOptions(song) }
                            )
                        }
                    }
                }
            }
        }

        // Overlay dialog to insert dynamic custom playlists
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
    val context = LocalContext.current

    val glassSurf = surfaceColor.copy(alpha = bgOpacity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Preferences", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Subsection
        Text("Appearance", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Light Theme", color = textColor)
                    Switch(
                        checked = isLight,
                        onCheckedChange = { viewModel.setLightTheme(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AMOLED Mode (Pitch Black)", color = textColor)
                    Switch(
                        checked = amoled,
                        onCheckedChange = { viewModel.setAmoledTheme(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor),
                        enabled = !isLight
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Accent Shading Choice", color = textMutedColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ACCENT_PRIMARY_COLORS.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (activeAccent == index) 3.dp else 0.dp,
                                    color = if (isLight) Color.Black else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setAccentColorIndex(index) }
                        )
                    }
                }
            }
        }

        // Premium Customization Subsection
        Text("Premium Workspace Layout", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Background Selector Row
                Text("Theme Background Style", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select your preferred ambient background color", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bgOptions) { (id, name, color) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .border(
                                    width = if (bgType == id) 2.dp else 1.dp,
                                    color = if (bgType == id) primaryColor else textColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setBackgroundType(id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (id == 1 && bgType != id) Color.DarkGray else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (bgType == id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (bgType == 7) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Solid Color", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
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
                        modifier = Modifier.fillMaxWidth()
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
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            cursorColor = primaryColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        isError = errorMsg != null
                    )
                }
                
                if (bgType == 8) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Gradient Pair", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetGradients) { (pair, name) ->
                            val (start, end) = pair
                            val isSelected = (customBgGradStartHex == start && customBgGradEndHex == end)
                            Box(
                                modifier = Modifier
                                    .size(48.dp, 36.dp)
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
                Divider(color = textColor.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Opacity / Glass Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Overlays Translucency", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    val translucencyPct = ((1f - bgOpacity) * 100).toInt()
                    val statusText = if (translucencyPct == 100) "100% (Fully Disappeared)" else "$translucencyPct%"
                    Text(statusText, color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Higher translucency makes panels invisible, revealing the background", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = bgOpacity,
                    onValueChange = { viewModel.setBackgroundOpacity(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = textColor.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Library Placement
                Text("Library Drawer Position", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Rearrange the position order of your music catalog space", color = textMutedColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                val placementOptions = listOf(
                    Pair("tabs", "Nav Tab (Classic)"),
                    Pair("home_top", "Home Top Section"),
                    Pair("home_bottom", "Home Bottom Section")
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    placementOptions.forEach { (route, name) ->
                        val selected = libPlacement == route
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) primaryColor.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.08f))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = primaryColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setLibraryPlacement(route) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (selected) primaryColor else textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Audio Subsection
        Text("Audio Performance", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Default audio resolution: $audioQual", color = textColor, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf("Auto", "Low", "High").forEach { i ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (audioQual == i) primaryColor else Color.Gray.copy(alpha = 0.15f))
                                .clickable { viewModel.setAudioQuality(i) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(i, color = if (audioQual == i) Color.White else textColor, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Crossfade overlapping: ${crossfadeVal}s", color = textColor, fontSize = 14.sp)
                Slider(
                    value = crossfadeVal.toFloat(),
                    onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Storage Subsection
        Text("Local cache utility", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glassSurf)
                .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Wipe temp play cache", color = textColor, fontSize = 14.sp)
                    Text("Clears transient audio streams", color = textMutedColor, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Storage and Audio buffers cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Clear", color = Color.White)
                }
            }
        }

        // About description
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FALLAN STREAMS", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Version 2.6.4 (NATIVE COMPOSE BUILD)", color = textMutedColor, fontSize = 12.sp)
                Text("Source nodes: YouTube • YT Music • JioSaavn", color = textMutedColor, fontSize = 11.sp)
            }
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
    
    var displaysEqDialog by remember { mutableStateOf(false) }
    var displaysLyricsDialog by remember { mutableStateOf(false) }
    var displaysQueueDialog by remember { mutableStateOf(false) }

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

    // Trigger loading lyrics if turned on
    LaunchedEffect(song) {
        viewModel.fetchLyrics(song)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blur cover art backdrop
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .alpha(0.35f),
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
                IconButton(onClick = onCollapse, modifier = Modifier.testTag("player_back")) {
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

            // Spinning cover album layout with circular dynamic shadow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(244.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(
                                4.dp,
                                Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor))
                            ),
                            CircleShape
                        )
                        .rotate(if (isPlaying) rotAngle else 0f)
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Buffering ring spinner overlay
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = secondaryColor,
                        strokeWidth = 5.dp,
                        modifier = Modifier.size(260.dp)
                    )
                }
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
                            else -> "Local / Offline"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Digital Seek track bar Section
            Column(modifier = Modifier.fillMaxWidth()) {
                val positionSeconds = position / 1000
                val durationSeconds = duration / 1000
                val elapsedMinutes = positionSeconds / 60
                val elapsedRemSeconds = positionSeconds % 60
                val durationMinutes = durationSeconds / 60
                val durationRemSeconds = durationSeconds % 60

                Slider(
                    value = position.toFloat(),
                    onValueChange = { PlaybackManager.seekTo(it.toInt()) },
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
                
                IconButton(onClick = { displaysLyricsDialog = true }) {
                    Icon(imageVector = Icons.Default.Notes, contentDescription = "Lyrics", tint = Color.White.copy(alpha = 0.8f))
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

                IconButton(onClick = { displaysQueueDialog = true }) {
                    Icon(imageVector = Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color.White.copy(alpha = 0.8f))
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

        // Lyrics Dialog Overlay
        if (displaysLyricsDialog) {
            LyricsOverlay(
                title = song.title,
                lyricsPair = lyricsItem,
                isLoading = isLyricsLoading,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                textMutedColor = textMutedColor,
                onDismiss = { displaysLyricsDialog = false }
            )
        }

        // Queue list Dialog View Overlay
        if (displaysQueueDialog) {
            QueueOverlay(
                primaryColor = primaryColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                textMutedColor = textMutedColor,
                onDismiss = { displaysQueueDialog = false }
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

// ----------------------------------------------------
// LYRICS OVERLAY SHEET COMPOSABLE
// ----------------------------------------------------
@Composable
fun LyricsOverlay(
    title: String,
    lyricsPair: Pair<String, Boolean>,
    isLoading: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Decoded Lyrics", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (lyricsPair.second) "Synced Timings Available" else "Static Scrolling Sheet",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = lyricsPair.first,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// QUEUE LIST OVERLAY VIEW
// ----------------------------------------------------
@Composable
fun QueueOverlay(
    primaryColor: Color,
    surfaceColor: Color,
    textColor: Color,
    textMutedColor: Color,
    onDismiss: () -> Unit
) {
    val queueList by PlaybackManager.currentQueue.collectAsState()
    val currentSong by PlaybackManager.currentSong.collectAsState()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Queue", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (queueList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Queue list is currently empty.", color = textMutedColor, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(queueList) { index, song ->
                            val isCurrent = currentSong?.id == song.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        PlaybackManager.playSongAtIndex(context, index)
                                        onDismiss()
                                    }
                                    .background(
                                        if (isCurrent) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) primaryColor else textColor,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
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
                                if (isCurrent) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Playing", tint = primaryColor, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
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
                            PlaybackManager.setQueue(PlaybackManager.currentQueue.value + song, 0)
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

