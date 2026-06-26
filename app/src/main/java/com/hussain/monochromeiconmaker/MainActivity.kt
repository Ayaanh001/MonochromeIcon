package com.hussain.monochromeiconmaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.monochromeiconmaker.ui.theme.MonochromeIconMakerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val sharedUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        setContent { MonochromeIconMakerTheme { App(sharedUri.value) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let {
                sharedUri.value = it
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(initialUri: Uri? = null) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var currentUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var monochromeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var makeBlack by rememberSaveable { mutableStateOf(true) }
    var scale by rememberSaveable { mutableStateOf(1f) }
    var offsetX by rememberSaveable { mutableStateOf(0f) }
    var offsetY by rememberSaveable { mutableStateOf(0f) }

    var showInfoSheet by rememberSaveable { mutableStateOf(false) }

    // Update monochrome bitmap when source changes
    LaunchedEffect(sourceBitmap) {
        monochromeBitmap = withContext(Dispatchers.Default) {
            sourceBitmap?.toSmartMonochromeBlack()
        }
    }

    // Handle shared image from intent
    LaunchedEffect(initialUri) {
        if (initialUri != null && initialUri != currentUri) {
            currentUri = initialUri
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Load bitmap when currentUri changes
    LaunchedEffect(currentUri) {
        sourceBitmap = withContext(Dispatchers.IO) {
            currentUri?.let { loadBitmapFromUri(context, it) }
        }
    }

    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentUri = it
                scale = 1f
                offsetX = 0f
                offsetY = 0f
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Monochrome Icon Maker",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showInfoSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(20.dp))

            // Preview card — no gestures here
            AdaptiveIconPreviewCard(
                sourceBitmap = sourceBitmap,
                monochromeBitmap = monochromeBitmap,
                makeBlack = makeBlack,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            )

            Spacer(Modifier.height(20.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (sourceBitmap == null) Arrangement.Center else Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pickImage.launch("image/*")
                    },
                    modifier = if (sourceBitmap == null) Modifier.fillMaxWidth().height(56.dp) else Modifier,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Import Image",
                        style = if (sourceBitmap == null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge
                    )
                }

                if (sourceBitmap != null) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentUri = null
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("Clear") }
                }
            }

            if (sourceBitmap != null) {
                Spacer(Modifier.height(16.dp))

                // ── Merged Adjustments Card ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Section header
                    Text(
                        text = "Adjustments",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Monochrome toggle row — integrated into same card
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Monochrome",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (makeBlack) "Tinted to palette color" else "Original colors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = makeBlack,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                makeBlack = it
                            },
                            thumbContent = {
                                AnimatedContent(
                                    targetState = makeBlack,
                                    transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                                    label = "switch_thumb_icon"
                                ) { isChecked ->
                                    Icon(
                                        imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedIconColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(Modifier.height(4.dp))

                    // Sliders
                    SliderWithReset(
                        label = "Scale",
                        value = scale,
                        defaultValue = 1f,
                        valueRange = 0.2f..5f,
                        valueFormatter = { "%.2f×".format(it) },
                        onValueChange = { scale = it },
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scale = 1f
                        },
                        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    )

                    SliderWithReset(
                        label = "Offset X",
                        value = offsetX,
                        defaultValue = 0f,
                        valueRange = -100f..100f,
                        valueFormatter = { "%.0f".format(it) },
                        onValueChange = { offsetX = it },
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            offsetX = 0f
                        },
                        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    )

                    SliderWithReset(
                        label = "Offset Y",
                        value = offsetY,
                        defaultValue = 0f,
                        valueRange = -100f..100f,
                        valueFormatter = { "%.0f".format(it) },
                        onValueChange = { offsetY = it },
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            offsetY = 0f
                        },
                        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val uri = withContext(Dispatchers.IO) {
                                        exportSingleFile(
                                            context = context,
                                            sourceBitmap = sourceBitmap!!,
                                            makeBlack = makeBlack,
                                            scale = scale,
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            exportScale = 4,
                                            format = "png"
                                        )
                                    }
                                    if (uri != null) {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Icon exported as PNG!",
                                            actionLabel = "View",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "image/png")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(viewIntent, "Open with")
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Export PNG", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    val uri = withContext(Dispatchers.IO) {
                                        exportSingleFile(
                                            context = context,
                                            sourceBitmap = sourceBitmap!!,
                                            makeBlack = makeBlack,
                                            scale = scale,
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            exportScale = 8, // Higher scale for SVG for better quality
                                            format = "svg"
                                        )
                                    }
                                    if (uri != null) {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Icon exported as SVG!",
                                            actionLabel = "View",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "image/svg+xml")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(
                                                Intent.createChooser(viewIntent, "Open with")
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Export SVG", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showInfoSheet) {
        InfoSheet(onDismiss = { showInfoSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Identity Section
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(SquircleShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_code),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Monochrome Icon Maker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Text(
                    text = "v1.0",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))

            // Unified Developer & Links Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ayaanh001"))
                            context.startActivity(intent)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ah_logo),
                        contentDescription = "Developer Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Developed by",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ayaan Hussain",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ayaanh001/MonochromeIcon"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_code), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("GitHub", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Source Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Ahacd1"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_telegram), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Telegram", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Support Chat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Made with ❤️ for Android Developers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun SliderWithReset(
    label: String,
    value: Float,
    defaultValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String = { "%.2f".format(it) },
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    onHaptic: () -> Unit = {}
) {
    val isModified = value != defaultValue
    val iconTint = if (isModified) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label + value
            Column(modifier = Modifier.width(80.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Slider(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    onHaptic()
                },
                onValueChangeFinished = {
                    // Haptic on release for satisfying feel
                    onHaptic()
                },
                valueRange = valueRange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            IconButton(
                onClick = onReset,
                enabled = isModified,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Reset $label",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Data & Shapes ─────────────────────────────────────────────────────────────

data class IconPalette(
    val lightBg: Color,
    val lightFg: Color,
    val darkBg: Color,
    val darkFg: Color
)

val palettes = listOf(
    IconPalette(
        lightBg = Color(0xFFD7F0D4), lightFg = Color(0xFF116B15),
        darkBg  = Color(0xFF0F5312), darkFg  = Color(0xFFC3EFBE)
    ),
    IconPalette(
        lightBg = Color(0xFFDDEEFE), lightFg = Color(0xFF00639A),
        darkBg  = Color(0xFF004B76), darkFg  = Color(0xFFC4E8FF)
    ),
    IconPalette(
        lightBg = Color(0xFFF3E6FC), lightFg = Color(0xFF6E2E9D),
        darkBg  = Color(0xFF531082), darkFg  = Color(0xFFE9D2FA)
    ),
    IconPalette(
        lightBg = Color(0xFFFFEED8), lightFg = Color(0xFF8E4A00),
        darkBg  = Color(0xFF683500), darkFg  = Color(0xFFFFDCC0)
    )
)

class SquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(
            Path().apply {
                val w = size.width
                val h = size.height
                val cW = w * 0.1f
                val cH = h * 0.1f
                moveTo(w / 2f, 0f)
                cubicTo(cW, 0f, 0f, cH, 0f, h / 2f)
                cubicTo(0f, h - cH, cW, h, w / 2f, h)
                cubicTo(w - cW, h, w, h - cH, w, h / 2f)
                cubicTo(w, cH, w - cW, 0f, w / 2f, 0f)
                close()
            }
        )
    }
}

// ── Preview Card (no gesture handling) ───────────────────────────────────────

@Composable
fun AdaptiveIconPreviewCard(
    sourceBitmap: android.graphics.Bitmap?,
    monochromeBitmap: android.graphics.Bitmap?,
    makeBlack: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    var isDarkMode by rememberSaveable { mutableStateOf(false) }
    var selectedPaletteIndex by rememberSaveable { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current

    val currentPalette = palettes[selectedPaletteIndex]
    val bgColor = if (isDarkMode) currentPalette.darkBg else currentPalette.lightBg
    val fgColor = if (isDarkMode) currentPalette.darkFg else currentPalette.lightFg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Adaptive Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Palette dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    palettes.forEachIndexed { index, palette ->
                        val isSelected = index == selectedPaletteIndex
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) palette.darkBg else palette.lightBg)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedPaletteIndex = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) palette.darkFg else palette.lightFg)
                                )
                            }
                        }
                    }
                }

                // Dark/light toggle
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isDarkMode = !isDarkMode
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isDarkMode) R.drawable.dark_mode else R.drawable.light_mode
                        ),
                        contentDescription = "Toggle Dark Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Icon shape previews
        val shapes = remember {
            listOf(
                "Square"  to RectangleShape,
                "Rounded" to RoundedCornerShape(16.dp),
                "Squircle" to SquircleShape(),
                "Circle"  to CircleShape,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayBitmap = if (makeBlack) (monochromeBitmap ?: sourceBitmap) else sourceBitmap
            shapes.forEach { (name, shape) ->
                IconPreviewItem(
                    name = name,
                    shape = shape,
                    bgColor = bgColor,
                    fgColor = fgColor,
                    displayBitmap = displayBitmap,
                    makeBlack = makeBlack,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
            }
        }
    }
}

// ── Individual Icon Preview ────────────────────────────────────────────────────

@Composable
fun IconPreviewItem(
    name: String,
    shape: Shape,
    bgColor: Color,
    fgColor: Color,
    displayBitmap: android.graphics.Bitmap?,
    makeBlack: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val colorFilter by remember(makeBlack, fgColor) {
        derivedStateOf {
            if (makeBlack) {
                androidx.compose.ui.graphics.ColorFilter.tint(
                    fgColor,
                    androidx.compose.ui.graphics.BlendMode.SrcIn
                )
            } else null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            val bmp = displayBitmap
            if (bmp != null) {
                val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val fitScale = minOf(
                        canvasW / bmp.width.toFloat(),
                        canvasH / bmp.height.toFloat()
                    )
                    val drawW = bmp.width * fitScale * scale
                    val drawH = bmp.height * fitScale * scale

                    val relativeScale = canvasW / 72f
                    val scaledOffsetX = offsetX * relativeScale
                    val scaledOffsetY = offsetY * relativeScale

                    val cx = canvasW / 2f + scaledOffsetX
                    val cy = canvasH / 2f + scaledOffsetY
                    val left = cx - drawW / 2f
                    val top  = cy - drawH / 2f

                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                        dstSize   = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt()),
                        colorFilter = colorFilter
                    )
                }
            }
        }

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}