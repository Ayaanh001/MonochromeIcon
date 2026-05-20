package com.hussain.monochromeiconmaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.monochromeiconmaker.ui.theme.MonochromeIconMakerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sharedUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent { MonochromeIconMakerTheme { App(sharedUri.value) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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

    var sourceBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var makeBlack by remember { mutableStateOf(true) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Handle shared image from intent
    LaunchedEffect(initialUri) {
        initialUri?.let {
            sourceBitmap = loadBitmapFromUri(context, it)
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    val pickImage =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                sourceBitmap = loadBitmapFromUri(context, it)
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
                makeBlack = makeBlack,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY
            )

            Spacer(Modifier.height(20.dp))

            // Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pickImage.launch("image/*")
                    },
                    shape = RoundedCornerShape(20.dp)
                ) { Text("Import Image") }

                if (sourceBitmap != null) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sourceBitmap = null
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
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
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

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val uri = exportSingleFile(
                                context = context,
                                sourceBitmap = sourceBitmap!!,
                                makeBlack = makeBlack,
                                scale = scale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                exportScale = 4,
                                format = "png"
                            )
                            if (uri != null) {
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Icon exported successfully!",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Export as PNG", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
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
    makeBlack: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedPaletteIndex by remember { mutableStateOf(0) }
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
            shapes.forEach { (name, shape) ->
                IconPreviewItem(
                    name = name,
                    shape = shape,
                    bgColor = bgColor,
                    fgColor = fgColor,
                    sourceBitmap = sourceBitmap,
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
    sourceBitmap: android.graphics.Bitmap?,
    makeBlack: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    // Stable derived values — avoids recomposing text layer when bitmap changes
    val displayBitmap by remember(sourceBitmap, makeBlack) {
        derivedStateOf {
            if (sourceBitmap != null && makeBlack) sourceBitmap.toSmartMonochromeBlack()
            else sourceBitmap
        }
    }

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