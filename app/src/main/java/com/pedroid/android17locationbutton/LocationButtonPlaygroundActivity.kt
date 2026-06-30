package com.pedroid.android17locationbutton

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.locationbutton.LocationButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LocationButtonPlaygroundActivity : AppCompatActivity() {

    private var locationButton: LocationButton? = null
    private lateinit var buttonContainer: FrameLayout

    // Position controls
    private lateinit var positionSpinner: Spinner
    private lateinit var customCoordsRow: LinearLayout
    private lateinit var xCoordEdit: EditText
    private lateinit var yCoordEdit: EditText
    private lateinit var paddingLabel: TextView
    private lateinit var paddingSeekBar: SeekBar

    // Text & shape controls
    private lateinit var textTypeSpinner: Spinner
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

    private val density get() = resources.displayMetrics.density

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
        "Precise Location"            to LocationButton.TEXT_TYPE_PRECISE_LOCATION,
        "Use Precise Location"        to LocationButton.TEXT_TYPE_USE_PRECISE_LOCATION,
        "Share Precise Location"      to LocationButton.TEXT_TYPE_SHARE_PRECISE_LOCATION,
        "Near My Precise Location"    to LocationButton.TEXT_TYPE_NEAR_MY_PRECISE_LOCATION,
        "Near Your Precise Location"  to LocationButton.TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION,
        "None"                        to LocationButton.TEXT_TYPE_NONE,
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

        bindViews()
        injectLocationButton()
        setupPositionControls()
        setupTextShapeControls()
        setupColorControls()
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews() {
        buttonContainer      = findViewById(R.id.button_container)
        positionSpinner      = findViewById(R.id.position_spinner)
        customCoordsRow      = findViewById(R.id.custom_coords_row)
        xCoordEdit           = findViewById(R.id.x_coord_edit)
        yCoordEdit           = findViewById(R.id.y_coord_edit)
        paddingLabel         = findViewById(R.id.padding_label)
        paddingSeekBar       = findViewById(R.id.padding_seekbar)
        textTypeSpinner      = findViewById(R.id.text_type_spinner)
        cornerRadiusLabel    = findViewById(R.id.corner_radius_label)
        cornerRadiusSeekBar  = findViewById(R.id.corner_radius_seekbar)
        pressedCornerLabel   = findViewById(R.id.pressed_corner_label)
        pressedCornerSeekBar = findViewById(R.id.pressed_corner_seekbar)
        strokeWidthLabel     = findViewById(R.id.stroke_width_label)
        strokeWidthSeekBar   = findViewById(R.id.stroke_width_seekbar)
        bgColorPreview       = findViewById(R.id.bg_color_preview)
        bgColorEdit          = findViewById(R.id.bg_color_edit)
        textColorPreview     = findViewById(R.id.text_color_preview)
        textColorEdit        = findViewById(R.id.text_color_edit)
        iconTintPreview      = findViewById(R.id.icon_tint_preview)
        iconTintEdit         = findViewById(R.id.icon_tint_edit)
        strokeColorPreview   = findViewById(R.id.stroke_color_preview)
        strokeColorEdit      = findViewById(R.id.stroke_color_edit)
    }

    // ── Dynamic injection ─────────────────────────────────────────────────────

    private fun injectLocationButton() {
        val btn = LocationButton(this)
        // ID is required for the ActivityResultRegistry key on pre-API-37 fallback
        btn.id = View.generateViewId()

        btn.setOnPermissionResultListener { isGranted ->
            val msgRes = if (isGranted) R.string.toast_location_granted else R.string.toast_location_denied
            Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show()
        }
        btn.setOnErrorListener { e ->
            Toast.makeText(this, getString(R.string.toast_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
        }

        buttonContainer.addView(
            btn,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        )
        locationButton = btn
    }

    // ── Position controls ─────────────────────────────────────────────────────

    private fun setupPositionControls() {
        positionSpinner.adapter = simpleAdapter(positionGravities.map { it.first })
        positionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val gravity = positionGravities[pos].second
                customCoordsRow.visibility = if (gravity == GRAVITY_CUSTOM) View.VISIBLE else View.GONE
                if (gravity != GRAVITY_CUSTOM) applyGravityPosition(gravity)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        findViewById<Button>(R.id.apply_position_button).setOnClickListener { applyCustomPosition() }

        paddingLabel.text = getString(R.string.label_padding, 0)
        paddingSeekBar.setOnSeekBarChangeListener(
            seekBarListener(paddingLabel, R.string.label_padding) { progress ->
                val px = (progress * density).toInt()
                locationButton?.setPadding(px, px, px, px)
            }
        )
    }

    private fun applyGravityPosition(gravity: Int) {
        val btn = locationButton ?: return
        val params = btn.layoutParams as FrameLayout.LayoutParams
        params.gravity = gravity
        params.leftMargin = 0
        params.topMargin = 0
        params.rightMargin = 0
        params.bottomMargin = 0
        btn.layoutParams = params
    }

    private fun applyCustomPosition() {
        val btn = locationButton ?: return
        val x = xCoordEdit.text.toString().toIntOrNull() ?: 0
        val y = yCoordEdit.text.toString().toIntOrNull() ?: 0
        val params = btn.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = x
        params.topMargin = y
        params.rightMargin = 0
        params.bottomMargin = 0
        btn.layoutParams = params
    }

    // ── Text & shape controls ─────────────────────────────────────────────────

    private fun setupTextShapeControls() {
        textTypeSpinner.adapter = simpleAdapter(textTypes.map { it.first })
        textTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                locationButton?.setTextType(textTypes[pos].second)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        cornerRadiusLabel.text = getString(R.string.label_corner_radius, 0)
        cornerRadiusSeekBar.setOnSeekBarChangeListener(
            seekBarListener(cornerRadiusLabel, R.string.label_corner_radius) { progress ->
                locationButton?.setCornerRadius(progress * density)
            }
        )

        pressedCornerLabel.text = getString(R.string.label_pressed_corner_radius, 0)
        pressedCornerSeekBar.setOnSeekBarChangeListener(
            seekBarListener(pressedCornerLabel, R.string.label_pressed_corner_radius) { progress ->
                locationButton?.setPressedCornerRadius(progress * density)
            }
        )

        strokeWidthLabel.text = getString(R.string.label_stroke_width, 0)
        strokeWidthSeekBar.setOnSeekBarChangeListener(
            seekBarListener(strokeWidthLabel, R.string.label_stroke_width) { progress ->
                locationButton?.setStrokeWidth((progress * density).toInt())
            }
        )
    }

    // ── Color controls ────────────────────────────────────────────────────────

    private fun setupColorControls() {
        findViewById<Button>(R.id.apply_colors_button).setOnClickListener { applyColors() }
    }

    private fun applyColors() {
        applyColorField(bgColorEdit, bgColorPreview) { locationButton?.setBackgroundColor(it) }
        applyColorField(textColorEdit, textColorPreview) { locationButton?.setTextColor(it) }
        applyColorField(iconTintEdit, iconTintPreview) { locationButton?.setIconTint(it) }
        applyColorField(strokeColorEdit, strokeColorPreview) { locationButton?.setStrokeColor(it) }
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

    companion object {
        private const val GRAVITY_CUSTOM = -1
    }
}
