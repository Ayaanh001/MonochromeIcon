package com.hussain.monochromeiconmaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.hussain.monochromeiconmaker.ui.theme.MonochromeIconMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MonochromeIconMakerTheme {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var makeBlack by remember { mutableStateOf(true) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sourceBitmap = loadBitmapFromUri(context, it)
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monochrome Icon Maker") },
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

            // Preview Box
            val borderDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            Box(
                Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(108.dp)
                        .background(Color.Transparent)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                RoundedCornerShape(6.dp)
                            )
                    )

                    val gestureModifier = Modifier
                        .matchParentSize()
                        .pointerInput(sourceBitmap, scale, offsetX, offsetY) {
                            if (sourceBitmap != null) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.2f, 5f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                        }

                    Canvas(modifier = gestureModifier) {
                        val bmp = sourceBitmap
                        if (bmp != null) {
                            val canvasW = size.width
                            val canvasH = size.height
                            val frameSize = 72f
                            val frameLeft = (canvasW - frameSize) / 2f
                            val frameTop = (canvasH - frameSize) / 2f

                            val fitScale = minOf(
                                frameSize / bmp.width.toFloat(),
                                frameSize / bmp.height.toFloat()
                            )
                            val drawW = bmp.width * fitScale * scale
                            val drawH = bmp.height * fitScale * scale
                            val cx = canvasW / 2f + offsetX
                            val cy = canvasH / 2f + offsetY
                            val left = cx - drawW / 2f
                            val top = cy - drawH / 2f

                            val displayBitmap = if (makeBlack) {
                                bmp.toSmartMonochromeBlack()
                            } else bmp

                            drawImage(
                                image = displayBitmap.asImageBitmap(),
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    left.toInt(),
                                    top.toInt()
                                ),
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    drawW.toInt(),
                                    drawH.toInt()
                                )
                            )

                            drawRect(
                                color = Color.Transparent,
                                size = androidx.compose.ui.geometry.Size(72f, 72f),
                                topLeft = androidx.compose.ui.geometry.Offset(frameLeft, frameTop),
                                style = Stroke(width = 1f, pathEffect = borderDash)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Row - positioned below preview box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { pickImage.launch("image/*") },
                    shape = RoundedCornerShape(20.dp)
                ) { Text("Import Image") }

                if (sourceBitmap != null) {
                    OutlinedButton(
                        onClick = {
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Monochrome",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = makeBlack,
                            onCheckedChange = { makeBlack = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Adjustments box
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(16.dp)
                ) {
                    SliderWithReset(
                        label = "Scale",
                        value = scale,
                        defaultValue = 1f,
                        valueRange = 0.2f..5f,
                        valueFormatter = { "%.2f".format(it) },
                        onValueChange = { scale = it },
                        onReset = { scale = 1f }
                    )

                    SliderWithReset(
                        label = "Offset X",
                        value = offsetX,
                        defaultValue = 0f,
                        valueRange = -100f..100f,
                        valueFormatter = { "%.0f".format(it) },
                        onValueChange = { offsetX = it },
                        onReset = { offsetX = 0f }
                    )

                    SliderWithReset(
                        label = "Offset Y",
                        value = offsetY,
                        defaultValue = 0f,
                        valueRange = -100f..100f,
                        valueFormatter = { "%.0f".format(it) },
                        onValueChange = { offsetY = it },
                        onReset = { offsetY = 0f }
                    )

                    Button(
                        onClick = {
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
                                        message = "Exported successfully!",
                                        actionLabel = "View",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "image/png")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                viewIntent,
                                                "Open with"
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Export as PNG")
                    }
                }
            }
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
    onReset: () -> Unit
) {
    val isModified = value != defaultValue
    val iconTint = if (isModified) MaterialTheme.colorScheme.primary else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = label,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        // Slider
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Reset Button
        IconButton(
            onClick = onReset,
            enabled = isModified
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Reset $label",
                tint = iconTint
            )
        }
    }
}