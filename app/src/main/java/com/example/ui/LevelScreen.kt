package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.HorizontalDistribute
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.data.database.AppSettings
import java.util.Locale
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    viewModel: LevelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bind sensor listener lifecycle activity
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startListening()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopListening()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopListening()
        }
    }

    // State bindings
    val currentMode by viewModel.currentMode.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val alignmentState by viewModel.alignmentState.collectAsState()

    val calibratedX by viewModel.calibratedPitch.collectAsState()
    val calibratedY by viewModel.calibratedRoll.collectAsState()
    val calibratedZ by viewModel.calibratedYaw.collectAsState()

    val maxDeviation by viewModel.maxDeviation.collectAsState()
    val rawData by viewModel.rawSensorData.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    // Color definitions
    val perfectColor = Color(0xFF00E676) // Radiant Green
    val nearColor = Color(0xFFFFA000)    // High-vis Amber
    val tiltedColor = Color(0xFFE53935)  // Danger Red
    val backgroundThemeColor = Color(0xFF121418) // Slate Industrial Dark

    val activeStateColor by animateColorAsState(
        targetValue = when (alignmentState) {
            AlignmentState.PERFECT -> perfectColor
            AlignmentState.NEAR -> nearColor
            AlignmentState.NOT_LEVEL -> tiltedColor
        },
        animationSpec = spring(),
        label = "state_color"
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundThemeColor,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(activeStateColor)
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.resetMaxDeviation() },
                            modifier = Modifier.testTag("reset_max_dev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Max Deviation",
                                tint = Color.LightGray
                            )
                        }
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.LightGray
                            )
                        }
                    }
                )
            },
            containerColor = backgroundThemeColor,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Mode Selection tab
                TabRow(
                    selectedTabIndex = currentMode.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentMode.ordinal]),
                            color = activeStateColor
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("mode_tab_row")
                ) {
                    LevelMode.entries.forEach { mode ->
                        val isSelected = currentMode == mode
                        val label = when (mode) {
                            LevelMode.HORIZONTAL -> stringResource(R.string.horizontal_mode)
                            LevelMode.VERTICAL -> stringResource(R.string.vertical_mode)
                            LevelMode.ANGLE_METER -> stringResource(R.string.angle_mode)
                        }
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.selectMode(mode) },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_${mode.name.lowercase(Locale.ROOT)}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Midsection: Spirit Level View Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 450.dp)
                        .aspectRatio(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E222B)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (currentMode) {
                                LevelMode.HORIZONTAL -> {
                                    CircularBubbleLevel(
                                        pitch = calibratedX,
                                        roll = calibratedY,
                                        activeColor = activeStateColor,
                                        tolerancePerfect = appSettings.tolerancePerfect,
                                        toleranceNear = appSettings.toleranceNear
                                    )
                                }
                                LevelMode.VERTICAL -> {
                                    VerticalBubbleLevel(
                                        roll = calibratedY,
                                        activeColor = activeStateColor,
                                        tolerancePerfect = appSettings.tolerancePerfect,
                                        toleranceNear = appSettings.toleranceNear
                                    )
                                }
                                LevelMode.ANGLE_METER -> {
                                    AngularGaugeLevel(
                                        pitch = calibratedX,
                                        roll = calibratedY,
                                        yaw = calibratedZ,
                                        activeColor = activeStateColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Section: Digital Readouts and Quick Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Precision output formatting
                    val decimals = if (appSettings.advancedPrecision) 2 else 1
                    val formatString = "%.${decimals}f°"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Display X reading
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.axis_x),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = String.format(Locale.ROOT, formatString, calibratedX),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.testTag("value_x")
                            )
                        }

                        // Display Y reading
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.axis_y),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = String.format(Locale.ROOT, formatString, calibratedY),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.testTag("value_y")
                            )
                        }

                        // Display Max Deviation
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.deviation_max),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = String.format(Locale.ROOT, formatString, maxDeviation),
                                color = activeStateColor,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.testTag("value_max_dev")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons/Sensor-Type indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Calibration Panel toggle is visually striking
                        Button(
                            onClick = { showCalibrationDialog = !showCalibrationDialog },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2D323E),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("calibration_dialog_trigger_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Calibration",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.calibration), fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Active Sensor Details Box
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E222B)
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Sensor standard",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = rawData.sensorTypeUsed,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Interactive Expandable Calibration Dialog card
                AnimatedVisibility(visible = showCalibrationDialog) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF232731)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("calibration_expanded_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.calibrate),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.calibrate_desc),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.calibrateZero()
                                        showCalibrationDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = perfectColor, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("calibrate_confirm_button")
                                ) {
                                    Text(stringResource(R.string.calibrate))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.resetCalibration()
                                        showCalibrationDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("calibrate_reset_button")
                                ) {
                                    Text(stringResource(R.string.reset_calibration))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expanded advanced Settings Bottom Sheet Drawer
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF1C1E24),
                contentColor = Color.White,
                modifier = Modifier.testTag("settings_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = "drag",
                        tint = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Haptic Switch Control
                    SettingsToggleRow(
                        title = stringResource(R.string.haptics_settings_title),
                        description = stringResource(R.string.haptics_settings_desc),
                        checked = appSettings.hapticEnabled,
                        onCheckedChange = { viewModel.toggleHaptic(it) },
                        testTag = "haptic_switch"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Audio Switch Control
                    SettingsToggleRow(
                        title = stringResource(R.string.audio_settings_title),
                        description = stringResource(R.string.audio_settings_desc),
                        checked = appSettings.audioEnabled,
                        onCheckedChange = { viewModel.toggleAudio(it) },
                        testTag = "audio_switch"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Double-decimal Precision switch selector
                    SettingsToggleRow(
                        title = stringResource(R.string.precision_settings_title),
                        description = stringResource(R.string.precision_settings_desc),
                        checked = appSettings.advancedPrecision,
                        onCheckedChange = { viewModel.togglePrecision(it) },
                        testTag = "precision_switch"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Sensor filter smoothing tuning slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.filter_settings_title),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = when {
                                    appSettings.sensorSmoothing <= 0.05f -> "Ultra Smooth"
                                    appSettings.sensorSmoothing <= 0.15f -> "Standard"
                                    else -> "Instant Response"
                                },
                                color = activeStateColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = appSettings.sensorSmoothing,
                            onValueChange = { viewModel.changeSmoothing(it) },
                            valueRange = 0.02f..0.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = activeStateColor,
                                activeTrackColor = activeStateColor,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("smoothing_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Reset settings button
                    Button(
                        onClick = { viewModel.resetAllSettings() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reset_settings_button")
                    ) {
                        Text(stringResource(R.string.reset_settings), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E676),
                checkedTrackColor = Color(0xFF1E5c33),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

// 1. Circular/Bullseye Bubble drawing logic
@Composable
fun CircularBubbleLevel(
    pitch: Float,
    roll: Float,
    activeColor: Color,
    tolerancePerfect: Float,
    toleranceNear: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .testTag("circular_level_canvas")
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.width / 2

        // Determine max angle range that represents the edge boundary (e.g. 10 degrees is the edge)
        val maxAngle = 10f
        val maxDisplacementRadius = outerRadius * 0.75f // outer boundary
        val bubbleRadius = outerRadius * 0.15f

        // Physics-correct displacement
        // Roll represents tilt on X axis, Pitch represents tilt on Y axis
        // We invert appropriately so the bubble moves towards the high side
        val rawDispX = -(roll / maxAngle) * maxDisplacementRadius
        val rawDispY = -(pitch / maxAngle) * maxDisplacementRadius

        // Clamp distance to bounds
        val currentDistance = sqrt(rawDispX * rawDispX + rawDispY * rawDispY)
        val limitRadius = maxDisplacementRadius - bubbleRadius
        val (dispX, dispY) = if (currentDistance > limitRadius) {
            val scale = limitRadius / currentDistance
            Pair(rawDispX * scale, rawDispY * scale)
        } else {
            Pair(rawDispX, rawDispY)
        }

        // Draw deep level dial background
        drawCircle(
            color = Color(0xFF13171F),
            radius = outerRadius
        )

        // Draw outer ring boundary
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = outerRadius,
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw secondary guiding rings (representing angle thresholds)
        // 1. Near Level indicator ring (1.0 degree)
        drawCircle(
            color = Color.Gray.copy(alpha = 0.25f),
            radius = (toleranceNear / maxAngle) * maxDisplacementRadius,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 2. Perfect Level center core ring (0.2 degree)
        drawCircle(
            color = activeColor.copy(alpha = 0.4f),
            radius = (tolerancePerfect / maxAngle) * maxDisplacementRadius,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw dynamic axis crosshairs
        // Horizontal bar
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(outerRadius * 0.15f, center.y),
            end = Offset(outerRadius * 1.85f, center.y),
            strokeWidth = 2.dp.toPx()
        )
        // Vertical bar
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(center.x, outerRadius * 0.15f),
            end = Offset(center.x, outerRadius * 1.85f),
            strokeWidth = 2.dp.toPx()
        )

        // Draw the fluid neon bubble with a shiny radial gradient
        val bubbleCenter = Offset(center.x + dispX, center.y + dispY)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    activeColor,
                    activeColor.copy(alpha = 0.7f)
                ),
                center = Offset(bubbleCenter.x - bubbleRadius * 0.25f, bubbleCenter.y - bubbleRadius * 0.25f),
                radius = bubbleRadius
            ),
            radius = bubbleRadius,
            center = bubbleCenter
        )

        // Draw glossy bubble highlight ring
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = bubbleRadius * 0.85f,
            center = bubbleCenter,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// 2. Vertical Linear vial bubble level
@Composable
fun VerticalBubbleLevel(
    roll: Float, // horizontal misalignment while holding vertical
    activeColor: Color,
    tolerancePerfect: Float,
    toleranceNear: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("vertical_level_canvas")
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val vialWidth = size.width * 0.22f
        val vialHeight = size.height * 0.9f

        val halfVialHeight = vialHeight / 2
        val maxAngle = 10f
        val bubbleRadius = vialWidth * 0.35f
        val maxDisplacement = halfVialHeight * 0.8f

        // Correct physics displacement
        val rawDisp = -(roll / maxAngle) * maxDisplacement
        val dispY = rawDisp.coerceIn(-maxDisplacement + bubbleRadius, maxDisplacement - bubbleRadius)

        // Vial container corner shape drawing
        drawRoundRect(
            color = Color(0xFF13171F),
            topLeft = Offset(center.x - vialWidth / 2, center.y - halfVialHeight),
            size = Size(vialWidth, vialHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(vialWidth / 2, vialWidth / 2)
        )

        // Outer contour line
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(center.x - vialWidth / 2, center.y - halfVialHeight),
            size = Size(vialWidth, vialHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(vialWidth / 2, vialWidth / 2),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw center range marks
        val markWidth = vialWidth * 0.8f
        // Perfect balance marks
        drawLine(
            color = activeColor.copy(alpha = 0.4f),
            start = Offset(center.x - markWidth / 2, center.y - (tolerancePerfect / maxAngle * maxDisplacement)),
            end = Offset(center.x + markWidth / 2, center.y - (tolerancePerfect / maxAngle * maxDisplacement)),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = activeColor.copy(alpha = 0.4f),
            start = Offset(center.x - markWidth / 2, center.y + (tolerancePerfect / maxAngle * maxDisplacement)),
            end = Offset(center.x + markWidth / 2, center.y + (tolerancePerfect / maxAngle * maxDisplacement)),
            strokeWidth = 2.dp.toPx()
        )

        // Near balance marks
        drawLine(
            color = Color.Gray.copy(alpha = 0.25f),
            start = Offset(center.x - markWidth / 2, center.y - (toleranceNear / maxAngle * maxDisplacement)),
            end = Offset(center.x + markWidth / 2, center.y - (toleranceNear / maxAngle * maxDisplacement)),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = Color.Gray.copy(alpha = 0.25f),
            start = Offset(center.x - markWidth / 2, center.y + (toleranceNear / maxAngle * maxDisplacement)),
            end = Offset(center.x + markWidth / 2, center.y + (toleranceNear / maxAngle * maxDisplacement)),
            strokeWidth = 1.5.dp.toPx()
        )

        // Draw the moving linear vial bubble
        val bubbleCenter = Offset(center.x, center.y + dispY)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    activeColor,
                    activeColor.copy(alpha = 0.7f)
                ),
                center = Offset(bubbleCenter.x - bubbleRadius * 0.2f, bubbleCenter.y - bubbleRadius * 0.2f),
                radius = bubbleRadius
            ),
            radius = bubbleRadius,
            center = bubbleCenter
        )

        // Glass reflection shine highlights
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = bubbleRadius * 0.8f,
            center = bubbleCenter,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// 3. Angular circular detailed dial
@Composable
fun AngularGaugeLevel(
    pitch: Float,
    roll: Float,
    yaw: Float,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("angular_gauge_canvas")
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.width / 2
        val strokeWidth = 12.dp.toPx()

        // Dial base backing
        drawCircle(
            color = Color(0xFF13171F),
            radius = outerRadius
        )

        // Draw arc representing pitch angle range
        val angleSweepRange = 360f
        val startAngle = -90f
        // Sweep proportional to Roll tilt
        val rollTiltSweep = roll % 360f

        drawArc(
            color = Color.Gray.copy(alpha = 0.15f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )

        drawArc(
            color = activeColor,
            startAngle = startAngle,
            sweepAngle = rollTiltSweep,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Horizontal baseline pointer
        drawLine(
            color = Color.LightGray.copy(alpha = 0.3f),
            start = Offset(center.x - outerRadius * 0.8f, center.y),
            end = Offset(center.x + outerRadius * 0.8f, center.y),
            strokeWidth = 1.5.dp.toPx()
        )

        // Live rotated incline line indicator matching device roll
        val angleRad = Math.toRadians(roll.toDouble())
        val indicatorLength = outerRadius * 0.85f
        val cos = kotlin.math.cos(angleRad).toFloat()
        val sin = kotlin.math.sin(angleRad).toFloat()

        drawLine(
            color = activeColor,
            start = Offset(center.x - indicatorLength * cos, center.y - indicatorLength * sin),
            end = Offset(center.x + indicatorLength * cos, center.y + indicatorLength * sin),
            strokeWidth = 3.dp.toPx()
        )

        // Dynamic center indicator ring
        drawCircle(
            color = activeColor,
            radius = 8.dp.toPx()
        )
    }
}
