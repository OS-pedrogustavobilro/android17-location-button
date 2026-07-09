package com.pedroid.android17locationbutton

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class LocationButtonPlaygroundActivity : AppCompatActivity() {

    private lateinit var controller: LocationButtonController

    // Location strategy controls
    private lateinit var locationStrategySpinner: Spinner
    private lateinit var updatesOptionsRow: LinearLayout
    private lateinit var updateIntervalLabel: TextView
    private lateinit var updateIntervalSeekBar: SeekBar
    private lateinit var minDistanceLabel: TextView
    private lateinit var minDistanceSeekBar: SeekBar
    private lateinit var stopUpdatesButton: Button
    private lateinit var locationResultText: TextView

    // Position controls
    private lateinit var positionSpinner: Spinner
    private lateinit var customCoordsRow: LinearLayout
    private lateinit var xCoordEdit: EditText
    private lateinit var yCoordEdit: EditText
    private lateinit var marginStartLabel: TextView
    private lateinit var marginStartSeekBar: SeekBar
    private lateinit var marginTopLabel: TextView
    private lateinit var marginTopSeekBar: SeekBar
    private lateinit var marginEndLabel: TextView
    private lateinit var marginEndSeekBar: SeekBar
    private lateinit var marginBottomLabel: TextView
    private lateinit var marginBottomSeekBar: SeekBar

    // Text & shape controls
    private lateinit var textTypeSpinner: Spinner
    private lateinit var widthLabel: TextView
    private lateinit var widthSeekBar: SeekBar
    private lateinit var cornerRadiusLabel: TextView
    private lateinit var cornerRadiusSeekBar: SeekBar
    private lateinit var pressedCornerLabel: TextView
    private lateinit var pressedCornerSeekBar: SeekBar
    private lateinit var strokeWidthLabel: TextView
    private lateinit var strokeWidthSeekBar: SeekBar

    // Color controls
    private lateinit var bgColorPreview: View
    private lateinit var bgColorEdit: EditText
    private lateinit var textColorPreview: View
    private lateinit var textColorEdit: EditText
    private lateinit var iconTintPreview: View
    private lateinit var iconTintEdit: EditText
    private lateinit var strokeColorPreview: View
    private lateinit var strokeColorEdit: EditText

    private val locationStrategies = listOf(
        "Last Known"       to LocationButtonController.LocationStrategy.LAST_KNOWN,
        "Current Location" to LocationButtonController.LocationStrategy.CURRENT,
        "Location Updates" to LocationButtonController.LocationStrategy.UPDATES,
    )

    private val positionGravities = listOf(
        "Center"       to Gravity.CENTER,
        "Top"          to (Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        "Bottom"       to (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL),
        "Top Left"     to (Gravity.TOP or Gravity.START),
        "Top Right"    to (Gravity.TOP or Gravity.END),
        "Bottom Left"  to (Gravity.BOTTOM or Gravity.START),
        "Bottom Right" to (Gravity.BOTTOM or Gravity.END),
        "Center Left"  to (Gravity.CENTER_VERTICAL or Gravity.START),
        "Center Right" to (Gravity.CENTER_VERTICAL or Gravity.END),
        "Custom (px)"  to GRAVITY_CUSTOM,
    )

    private val textTypes = listOf(
        "Precise Location"           to LocationButtonController.TextType.PRECISE_LOCATION,
        "Use Precise Location"       to LocationButtonController.TextType.USE_PRECISE_LOCATION,
        "Share Precise Location"     to LocationButtonController.TextType.SHARE_PRECISE_LOCATION,
        "Near My Precise Location"   to LocationButtonController.TextType.NEAR_MY_PRECISE_LOCATION,
        "Near Your Precise Location" to LocationButtonController.TextType.NEAR_YOUR_PRECISE_LOCATION,
        "None"                       to LocationButtonController.TextType.NONE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location_button_playground)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.playground_root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bindViews()
        attachController()
        collectLocationUpdatesFlow()
        setupLocationStrategyControls()
        setupPositionControls()
        setupTextShapeControls()
        setupColorControls()
    }

    // ── Controller attachment ─────────────────────────────────────────────────

    private fun attachController() {
        controller = LocationButtonController
            .attach(findViewById(R.id.button_container))
            .also { it.fetchLocationOnGrant = true }
            .setCallback(object : LocationButtonController.Callback {
                override fun onPermissionResult(granted: Boolean) {
                    val msgRes = if (granted) R.string.toast_location_granted
                                 else R.string.toast_location_denied
                    Toast.makeText(this@LocationButtonPlaygroundActivity, msgRes, Toast.LENGTH_SHORT).show()
                    if (granted && controller.locationStrategy == LocationButtonController.LocationStrategy.UPDATES) {
                        stopUpdatesButton.visibility = View.VISIBLE
                    }
                }
                override fun onLocation(location: Location?) {
                    // Fired for LAST_KNOWN and CURRENT; also fired per-update for UPDATES
                    locationResultText.text = if (location != null) {
                        "Lat: %.6f  |  Lng: %.6f  |  %s".format(location.latitude, location.longitude, location.formattedTime())
                    } else {
                        getString(R.string.location_no_cache)
                    }
                    locationResultText.visibility = View.VISIBLE
                }
                override fun onError(throwable: Throwable) {
                    Toast.makeText(
                        this@LocationButtonPlaygroundActivity,
                        getString(R.string.toast_error, throwable.message ?: "Unknown"),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            })
    }

    /**
     * Collects [LocationButtonController.locationUpdatesFlow] to keep the Stop button
     * visible while updates are streaming. This demonstrates the Flow-based alternative
     * to the [LocationButtonController.Callback.onLocation] callback for UPDATES mode.
     */
    private fun collectLocationUpdatesFlow() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                controller.locationUpdatesFlow.collect {
                    stopUpdatesButton.visibility = View.VISIBLE
                }
            }
        }
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews() {
        locationStrategySpinner  = findViewById(R.id.location_strategy_spinner)
        updatesOptionsRow        = findViewById(R.id.updates_options_row)
        updateIntervalLabel      = findViewById(R.id.update_interval_label)
        updateIntervalSeekBar    = findViewById(R.id.update_interval_seekbar)
        minDistanceLabel         = findViewById(R.id.min_distance_label)
        minDistanceSeekBar       = findViewById(R.id.min_distance_seekbar)
        stopUpdatesButton        = findViewById(R.id.stop_updates_button)
        locationResultText       = findViewById(R.id.location_result_text)
        positionSpinner          = findViewById(R.id.position_spinner)
        customCoordsRow          = findViewById(R.id.custom_coords_row)
        xCoordEdit               = findViewById(R.id.x_coord_edit)
        yCoordEdit               = findViewById(R.id.y_coord_edit)
        marginStartLabel         = findViewById(R.id.margin_start_label)
        marginStartSeekBar       = findViewById(R.id.margin_start_seekbar)
        marginTopLabel           = findViewById(R.id.margin_top_label)
        marginTopSeekBar         = findViewById(R.id.margin_top_seekbar)
        marginEndLabel           = findViewById(R.id.margin_end_label)
        marginEndSeekBar         = findViewById(R.id.margin_end_seekbar)
        marginBottomLabel        = findViewById(R.id.margin_bottom_label)
        marginBottomSeekBar      = findViewById(R.id.margin_bottom_seekbar)
        textTypeSpinner          = findViewById(R.id.text_type_spinner)
        widthLabel               = findViewById(R.id.width_label)
        widthSeekBar             = findViewById(R.id.width_seekbar)
        cornerRadiusLabel        = findViewById(R.id.corner_radius_label)
        cornerRadiusSeekBar      = findViewById(R.id.corner_radius_seekbar)
        pressedCornerLabel       = findViewById(R.id.pressed_corner_label)
        pressedCornerSeekBar     = findViewById(R.id.pressed_corner_seekbar)
        strokeWidthLabel         = findViewById(R.id.stroke_width_label)
        strokeWidthSeekBar       = findViewById(R.id.stroke_width_seekbar)
        bgColorPreview           = findViewById(R.id.bg_color_preview)
        bgColorEdit              = findViewById(R.id.bg_color_edit)
        textColorPreview         = findViewById(R.id.text_color_preview)
        textColorEdit            = findViewById(R.id.text_color_edit)
        iconTintPreview          = findViewById(R.id.icon_tint_preview)
        iconTintEdit             = findViewById(R.id.icon_tint_edit)
        strokeColorPreview       = findViewById(R.id.stroke_color_preview)
        strokeColorEdit          = findViewById(R.id.stroke_color_edit)
    }

    // ── Location strategy controls ────────────────────────────────────────────

    private fun setupLocationStrategyControls() {
        locationStrategySpinner.adapter = simpleAdapter(locationStrategies.map { it.first })
        locationStrategySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val strategy = locationStrategies[pos].second
                controller.locationStrategy = strategy
                val isUpdates = strategy == LocationButtonController.LocationStrategy.UPDATES
                updatesOptionsRow.visibility = if (isUpdates) View.VISIBLE else View.GONE
                if (!isUpdates) {
                    controller.stopLocationUpdates()
                    stopUpdatesButton.visibility = View.GONE
                    locationResultText.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        updateIntervalLabel.text = getString(R.string.label_update_interval, 5)
        updateIntervalSeekBar.setOnSeekBarChangeListener(
            seekBarListener(updateIntervalLabel, R.string.label_update_interval) { progress ->
                controller.locationUpdateIntervalMs = maxOf(1, progress) * 1_000L
            }
        )

        minDistanceLabel.text = getString(R.string.label_min_distance, 0)
        minDistanceSeekBar.setOnSeekBarChangeListener(
            seekBarListener(minDistanceLabel, R.string.label_min_distance) { progress ->
                controller.locationUpdateMinDistanceM = progress.toFloat()
            }
        )

        stopUpdatesButton.setOnClickListener {
            controller.stopLocationUpdates()
            stopUpdatesButton.visibility = View.GONE
        }
    }

    // ── Position controls ─────────────────────────────────────────────────────

    private fun setupPositionControls() {
        positionSpinner.adapter = simpleAdapter(positionGravities.map { it.first })
        positionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val gravity = positionGravities[pos].second
                val isCustom = gravity == GRAVITY_CUSTOM
                customCoordsRow.visibility = if (isCustom) View.VISIBLE else View.GONE
                setMarginSeekBarsEnabled(!isCustom)
                if (!isCustom) {
                    controller.setGravityPosition(gravity)
                    applyCurrentMargins()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        findViewById<Button>(R.id.apply_position_button).setOnClickListener {
            val x = xCoordEdit.text.toString().toIntOrNull() ?: 0
            val y = yCoordEdit.text.toString().toIntOrNull() ?: 0
            controller.setCustomPosition(x, y)
        }

        initMarginSeekBar(marginStartLabel,  R.string.label_margin_start,  marginStartSeekBar)
        initMarginSeekBar(marginTopLabel,    R.string.label_margin_top,    marginTopSeekBar)
        initMarginSeekBar(marginEndLabel,    R.string.label_margin_end,    marginEndSeekBar)
        initMarginSeekBar(marginBottomLabel, R.string.label_margin_bottom, marginBottomSeekBar)
    }

    private fun initMarginSeekBar(label: TextView, @StringRes labelRes: Int, seekBar: SeekBar) {
        label.text = getString(labelRes, 0)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                label.text = getString(labelRes, progress)
                if (sb.isEnabled) applyCurrentMargins()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun applyCurrentMargins() {
        controller.setMarginDp(
            marginStartSeekBar.progress,
            marginTopSeekBar.progress,
            marginEndSeekBar.progress,
            marginBottomSeekBar.progress,
        )
    }

    private fun setMarginSeekBarsEnabled(enabled: Boolean) {
        marginStartSeekBar.isEnabled  = enabled
        marginTopSeekBar.isEnabled    = enabled
        marginEndSeekBar.isEnabled    = enabled
        marginBottomSeekBar.isEnabled = enabled
    }

    // ── Text & shape controls ─────────────────────────────────────────────────

    private fun setupTextShapeControls() {
        textTypeSpinner.adapter = simpleAdapter(textTypes.map { it.first })
        textTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                controller.setTextType(textTypes[pos].second)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        widthLabel.text = getString(R.string.label_width_wrap)
        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                widthLabel.text = if (progress == 0) getString(R.string.label_width_wrap)
                                  else getString(R.string.label_width_dp, progress)
                controller.setWidthDp(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        cornerRadiusLabel.text = getString(R.string.label_corner_radius, 0)
        cornerRadiusSeekBar.setOnSeekBarChangeListener(
            seekBarListener(cornerRadiusLabel, R.string.label_corner_radius) { progress ->
                controller.setCornerRadiusDp(progress.toFloat())
            }
        )

        pressedCornerLabel.text = getString(R.string.label_pressed_corner_radius, 0)
        pressedCornerSeekBar.setOnSeekBarChangeListener(
            seekBarListener(pressedCornerLabel, R.string.label_pressed_corner_radius) { progress ->
                controller.setPressedCornerRadiusDp(progress.toFloat())
            }
        )

        strokeWidthLabel.text = getString(R.string.label_stroke_width, 0)
        strokeWidthSeekBar.setOnSeekBarChangeListener(
            seekBarListener(strokeWidthLabel, R.string.label_stroke_width) { progress ->
                controller.setStrokeWidthDp(progress.toFloat())
            }
        )
    }

    // ── Color controls ────────────────────────────────────────────────────────

    private fun setupColorControls() {
        findViewById<Button>(R.id.apply_colors_button).setOnClickListener { applyColors() }
    }

    private fun applyColors() {
        applyColorField(bgColorEdit, bgColorPreview) { controller.setBackgroundColor(it) }
        applyColorField(textColorEdit, textColorPreview) { controller.setTextColor(it) }
        applyColorField(iconTintEdit, iconTintPreview) { controller.setIconTint(it) }
        applyColorField(strokeColorEdit, strokeColorPreview) { controller.setStrokeColor(it) }
    }

    private fun applyColorField(edit: EditText, preview: View, apply: (Int) -> Unit) {
        val input = edit.text.toString().trim()
        if (input.isEmpty()) return
        try {
            val color = Color.parseColor(input)
            preview.setBackgroundColor(color)
            apply(color)
            edit.error = null
        } catch (e: IllegalArgumentException) {
            edit.error = getString(R.string.error_invalid_color)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun simpleAdapter(items: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun seekBarListener(
        label: TextView,
        @StringRes labelRes: Int,
        onProgress: (Int) -> Unit,
    ): SeekBar.OnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
            label.text = getString(labelRes, progress)
            onProgress(progress)
        }
        override fun onStartTrackingTouch(sb: SeekBar) {}
        override fun onStopTrackingTouch(sb: SeekBar) {}
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.detach()
    }

    companion object {
        private const val GRAVITY_CUSTOM = -1
    }
}
