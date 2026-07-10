package com.pedroid.android17locationbutton.composeapp

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pedroid.android17locationbutton.compose.LocationButtonView
import com.pedroid.android17locationbutton.compose.rememberLocationButtonStateHolder
import com.pedroid.android17locationbutton.core.LocationStrategy
import androidx.core.locationbutton.compose.LocationButtonTextType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class LocationButtonPlaygroundActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PlaygroundScreen(onBack = { finish() })
            }
        }
    }
}

private data class PositionOption(val label: String, val alignment: Alignment?)
private data class StrategyOption(val label: String, val strategy: LocationStrategy)
private data class TextTypeOption(val label: String, val textType: LocationButtonTextType)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaygroundScreen(onBack: () -> Unit) {
    val stateHolder = rememberLocationButtonStateHolder()

    // ── Location result ───────────────────────────────────────────────────────

    var locationText by remember { mutableStateOf("") }
    var showStopButton by remember { mutableStateOf(false) }
    // Resolve string resources here in composable scope; they cannot be called
    // inside plain (non-composable) lambdas like onLocation / onPermissionResult.
    val noCacheText = stringResource(R.string.location_no_cache)
    stateHolder.onPermissionResult = { granted ->
        if (!granted) locationText = ""
        if (granted && stateHolder.locationStrategy == LocationStrategy.UPDATES) {
            showStopButton = true
        }
    }
    stateHolder.onLocation = { loc ->
        locationText = if (loc != null)
            "Lat: %.6f  |  Lng: %.6f  |  ${loc.formattedTime()}".format(loc.latitude, loc.longitude)
        else noCacheText
    }
    LaunchedEffect(stateHolder) {
        stateHolder.locationUpdatesFlow.collect { showStopButton = true }
    }

    // ── Position state ────────────────────────────────────────────────────────

    val positions = remember {
        listOf(
            PositionOption("Center",        Alignment.Center),
            PositionOption("Top",           Alignment.TopCenter),
            PositionOption("Bottom",        Alignment.BottomCenter),
            PositionOption("Top Start",     Alignment.TopStart),
            PositionOption("Top End",       Alignment.TopEnd),
            PositionOption("Bottom Start",  Alignment.BottomStart),
            PositionOption("Bottom End",    Alignment.BottomEnd),
            PositionOption("Center Start",  Alignment.CenterStart),
            PositionOption("Center End",    Alignment.CenterEnd),
            PositionOption("Custom offset", null),
        )
    }
    var selectedPositionIndex by remember { mutableIntStateOf(0) }
    var offsetXDp by remember { mutableFloatStateOf(0f) }
    var offsetYDp by remember { mutableFloatStateOf(0f) }

    // ── Location strategy state ───────────────────────────────────────────────

    val strategies = remember {
        listOf(
            StrategyOption("Current Location",  LocationStrategy.CURRENT),
            StrategyOption("Location Updates",  LocationStrategy.UPDATES),
        )
    }
    var selectedStrategyIndex by remember { mutableIntStateOf(0) }
    var updateIntervalSec by remember { mutableFloatStateOf(5f) }
    var minDistanceM by remember { mutableFloatStateOf(0f) }

    // ── Text & shape state ────────────────────────────────────────────────────

    val textTypes = remember {
        listOf(
            TextTypeOption("Precise Location",         LocationButtonTextType.PreciseLocation),
            TextTypeOption("Use Precise Location",     LocationButtonTextType.UsePreciseLocation),
            TextTypeOption("Share Precise Location",   LocationButtonTextType.SharePreciseLocation),
            TextTypeOption("Near My Precise Location", LocationButtonTextType.NearMyPreciseLocation),
            TextTypeOption("None",                     LocationButtonTextType.None),
        )
    }
    var selectedTextTypeIndex by remember { mutableIntStateOf(0) }
    var cornerRadiusDp by remember { mutableFloatStateOf(50f) }
    var pressedCornerRadiusDp by remember { mutableFloatStateOf(50f) }
    var strokeWidthDp by remember { mutableFloatStateOf(0f) }

    // ── Color state ───────────────────────────────────────────────────────────

    var backgroundColorHex by remember { mutableStateOf("") }
    var textColorHex by remember { mutableStateOf("") }
    var iconTintHex by remember { mutableStateOf("") }
    var strokeColorHex by remember { mutableStateOf("") }
    var backgroundColor by remember { mutableStateOf<Color?>(null) }
    var textColor by remember { mutableStateOf<Color?>(null) }
    var iconTint by remember { mutableStateOf<Color?>(null) }
    var strokeColor by remember { mutableStateOf<Color?>(null) }

    val currentPosition = positions[selectedPositionIndex]

    // ── Screen layout ─────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playground_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {

            // ── Preview box ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val alignment = currentPosition.alignment
                val btnModifier = if (alignment != null)
                    Modifier.align(alignment)
                else
                    Modifier.offset(offsetXDp.dp, offsetYDp.dp)

                LocationButtonView(
                    stateHolder = stateHolder,
                    modifier = btnModifier,
                    textType = textTypes[selectedTextTypeIndex].textType,
                    backgroundColor = backgroundColor ?: Color.Unspecified,
                    textColor = textColor ?: Color.Unspecified,
                    iconTint = iconTint ?: Color.Unspecified,
                    cornerRadius = cornerRadiusDp.dp,
                    pressedCornerRadius = pressedCornerRadiusDp.dp,
                    strokeColor = strokeColor ?: Color.Unspecified,
                    strokeWidth = strokeWidthDp.dp,
                )

                if (locationText.isNotEmpty()) {
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            HorizontalDivider()

            // ── Controls ──────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                // Button state
                item {
                    SectionHeader("Button State")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { stateHolder.show() }) { Text("Show") }
                        OutlinedButton(onClick = { stateHolder.hide() }) { Text("Hide") }
                        OutlinedButton(onClick = { stateHolder.reset() }) { Text("Reset") }
                    }
                    Text(
                        text = "State: ${stateHolder.state}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                item { HorizontalDivider() }

                // Position
                item {
                    SectionHeader("Position")
                    DropdownControl(
                        label = "Alignment",
                        options = positions.map { it.label },
                        selectedIndex = selectedPositionIndex,
                        onSelect = { selectedPositionIndex = it },
                    )
                    if (currentPosition.alignment == null) {
                        Spacer(Modifier.height(8.dp))
                        SliderControl(
                            label = "X offset",
                            value = offsetXDp,
                            valueRange = -200f..200f,
                            unit = "dp",
                            onValueChange = { offsetXDp = it },
                        )
                        SliderControl(
                            label = "Y offset",
                            value = offsetYDp,
                            valueRange = -200f..200f,
                            unit = "dp",
                            onValueChange = { offsetYDp = it },
                        )
                    }
                }

                item { HorizontalDivider() }

                // Location strategy
                item {
                    SectionHeader("Location")
                    DropdownControl(
                        label = "Strategy",
                        options = strategies.map { it.label },
                        selectedIndex = selectedStrategyIndex,
                        onSelect = { index ->
                            selectedStrategyIndex = index
                            stateHolder.locationStrategy = strategies[index].strategy
                            val isUpdates = strategies[index].strategy == LocationStrategy.UPDATES
                            if (!isUpdates) {
                                stateHolder.stopLocationUpdates()
                                showStopButton = false
                            }
                        },
                    )
                    if (strategies[selectedStrategyIndex].strategy == LocationStrategy.UPDATES) {
                        Spacer(Modifier.height(8.dp))
                        SliderControl(
                            label = "Update interval",
                            value = updateIntervalSec,
                            valueRange = 1f..30f,
                            unit = "s",
                            onValueChange = {
                                updateIntervalSec = it
                                stateHolder.locationUpdateIntervalMs = it.roundToInt() * 1_000L
                            },
                        )
                        SliderControl(
                            label = "Min distance",
                            value = minDistanceM,
                            valueRange = 0f..50f,
                            unit = "m",
                            onValueChange = {
                                minDistanceM = it
                                stateHolder.locationUpdateMinDistanceM = it
                            },
                        )
                        if (showStopButton) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    stateHolder.stopLocationUpdates()
                                    showStopButton = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                            ) { Text("Stop Updates") }
                        }
                    }
                }

                item { HorizontalDivider() }

                // Text & shape
                item {
                    SectionHeader("Text & Shape")
                    DropdownControl(
                        label = "Text type",
                        options = textTypes.map { it.label },
                        selectedIndex = selectedTextTypeIndex,
                        onSelect = { selectedTextTypeIndex = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    SliderControl(
                        label = "Corner radius",
                        value = cornerRadiusDp,
                        valueRange = 0f..50f,
                        unit = "dp",
                        onValueChange = { cornerRadiusDp = it },
                    )
                    SliderControl(
                        label = "Pressed corner radius",
                        value = pressedCornerRadiusDp,
                        valueRange = 0f..50f,
                        unit = "dp",
                        onValueChange = { pressedCornerRadiusDp = it },
                    )
                    SliderControl(
                        label = "Stroke width",
                        value = strokeWidthDp,
                        valueRange = 0f..3f,
                        unit = "dp",
                        onValueChange = { strokeWidthDp = it },
                    )
                }

                item { HorizontalDivider() }

                // Colors
                item {
                    SectionHeader("Colors")
                    ColorInputRow(
                        label = "Background",
                        hex = backgroundColorHex,
                        previewColor = backgroundColor,
                        onHexChange = { backgroundColorHex = it },
                    )
                    ColorInputRow(
                        label = "Text",
                        hex = textColorHex,
                        previewColor = textColor,
                        onHexChange = { textColorHex = it },
                    )
                    ColorInputRow(
                        label = "Icon tint",
                        hex = iconTintHex,
                        previewColor = iconTint,
                        onHexChange = { iconTintHex = it },
                    )
                    ColorInputRow(
                        label = "Stroke",
                        hex = strokeColorHex,
                        previewColor = strokeColor,
                        onHexChange = { strokeColorHex = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            backgroundColor = parseHexColor(backgroundColorHex)
                            textColor = parseHexColor(textColorHex)
                            iconTint = parseHexColor(iconTintHex)
                            strokeColor = parseHexColor(strokeColorHex)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Apply Colors") }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Reusable control composables ──────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = options[selectedIndex],
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
) {
    val display = if (valueRange.endInclusive <= 10f) "%.1f $unit".format(value) else "${value.roundToInt()} $unit"
    Text(
        text = "$label: $display",
        style = MaterialTheme.typography.bodySmall,
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColorInputRow(
    label: String,
    hex: String,
    previewColor: Color?,
    onHexChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    previewColor ?: Color.Transparent,
                    RoundedCornerShape(4.dp),
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = hex,
            onValueChange = onHexChange,
            label = { Text("$label (#RRGGBB)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color? {
    val trimmed = hex.trim()
    if (trimmed.isEmpty()) return null
    return runCatching {
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        val argb = android.graphics.Color.parseColor(withHash)
        Color(argb)
    }.getOrNull()
}

private fun Location.formattedTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))
