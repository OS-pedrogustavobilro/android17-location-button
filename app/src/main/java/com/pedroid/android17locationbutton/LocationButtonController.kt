package com.pedroid.android17locationbutton

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.locationbutton.LocationButton

/**
 * Manages a [LocationButton] injected at runtime into a [FrameLayout] container.
 *
 * Responsibilities:
 *  - Creates and attaches the [LocationButton] programmatically (no XML required)
 *  - Owns permission-result and error wiring
 *  - Optionally fetches the last-known location after the grant and delivers it via [Callback]
 *  - Tracks a [State] and exposes show / hide / reset controls
 *  - Provides dp-based fluent setters for every public [LocationButton] API
 *
 * Obtain an instance via [attach].
 */
class LocationButtonController private constructor(
    private val container: FrameLayout,
) {

    // ── Public API types ──────────────────────────────────────────────────────

    enum class State { IDLE, GRANTED, DENIED, ERROR }

    interface Callback {
        fun onPermissionResult(granted: Boolean)
        fun onLocation(location: Location?) {}
        fun onError(throwable: Throwable) {}
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private val context: Context = container.context
    private val density: Float = context.resources.displayMetrics.density

    private val button: LocationButton = LocationButton(context).apply {
        // ID is required for the ActivityResultRegistry key used by the pre-API-37 fallback
        id = View.generateViewId()
    }

    private var _state: State = State.IDLE
    private var callback: Callback? = null

    // ── Configuration ─────────────────────────────────────────────────────────

    /** When true, hides the button automatically after permission is granted. */
    var autoHideOnGranted: Boolean = false

    /** When true, fetches the last-known location after permission is granted and
     *  delivers it via [Callback.onLocation]. */
    var fetchLocationOnGrant: Boolean = false

    // ── Read-only state ───────────────────────────────────────────────────────

    val state: State get() = _state
    val isVisible: Boolean get() = button.visibility == View.VISIBLE

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        button.setOnPermissionResultListener { granted ->
            _state = if (granted) State.GRANTED else State.DENIED
            if (granted && autoHideOnGranted) hide()
            callback?.onPermissionResult(granted)
            if (granted && fetchLocationOnGrant) deliverLocation()
        }
        button.setOnErrorListener { e ->
            _state = State.ERROR
            callback?.onError(e)
        }
        container.addView(
            button,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            )
        )
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        /** Creates a controller and immediately injects the [LocationButton] into [container]. */
        fun attach(container: FrameLayout): LocationButtonController =
            LocationButtonController(container)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun setCallback(callback: Callback?): LocationButtonController {
        this.callback = callback
        return this
    }

    /** Removes the button from the container and clears the callback reference. */
    fun detach() {
        container.removeView(button)
        callback = null
    }

    // ── Visibility & state ────────────────────────────────────────────────────

    fun show(): LocationButtonController {
        button.visibility = View.VISIBLE
        return this
    }

    fun hide(): LocationButtonController {
        button.visibility = View.GONE
        return this
    }

    /** Resets state to [State.IDLE] and makes the button visible again. */
    fun reset(): LocationButtonController {
        _state = State.IDLE
        return show()
    }

    // ── Position ──────────────────────────────────────────────────────────────

    /**
     * Positions the button using a [Gravity] constant (e.g. [Gravity.CENTER],
     * [Gravity.TOP] or [Gravity.BOTTOM] or [Gravity.START]).
     * Resets all margins to 0; call [setMarginDp] afterward to add spacing.
     */
    fun setGravityPosition(gravity: Int): LocationButtonController {
        updateParams {
            it.gravity = gravity
            it.setMargins(0, 0, 0, 0)
        }
        return this
    }

    /**
     * Positions the button at an absolute [xPx]/[yPx] pixel offset from the
     * top-start corner of the container (gravity is set to TOP|START).
     */
    fun setCustomPosition(xPx: Int, yPx: Int): LocationButtonController {
        updateParams {
            it.gravity = Gravity.TOP or Gravity.START
            it.setMargins(xPx, yPx, 0, 0)
        }
        return this
    }

    /** Applies a uniform margin (in dp) on all four sides of the button. */
    fun setMarginDp(dp: Int): LocationButtonController {
        val px = (dp * density).toInt()
        updateParams { it.setMargins(px, px, px, px) }
        return this
    }

    // ── Dimensions ────────────────────────────────────────────────────────────

    /**
     * Sets an explicit button width in dp.
     * Pass 0 to restore [FrameLayout.LayoutParams.WRAP_CONTENT].
     */
    fun setWidthDp(dp: Int): LocationButtonController {
        updateParams {
            it.width = if (dp == 0) FrameLayout.LayoutParams.WRAP_CONTENT
                       else (dp * density).toInt()
        }
        return this
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    fun setTextType(textType: Int): LocationButtonController {
        button.setTextType(textType)
        return this
    }

    fun setCornerRadiusDp(dp: Float): LocationButtonController {
        button.setCornerRadius(dp * density)
        return this
    }

    fun setPressedCornerRadiusDp(dp: Float): LocationButtonController {
        button.setPressedCornerRadius(dp * density)
        return this
    }

    fun setBackgroundColor(@ColorInt color: Int): LocationButtonController {
        button.setBackgroundColor(color)
        return this
    }

    fun setTextColor(@ColorInt color: Int): LocationButtonController {
        button.setTextColor(color)
        return this
    }

    fun setIconTint(@ColorInt color: Int): LocationButtonController {
        button.setIconTint(color)
        return this
    }

    fun setStrokeColor(@ColorInt color: Int): LocationButtonController {
        button.setStrokeColor(color)
        return this
    }

    /** [dp] is capped internally by the library at MAX_STROKE_WIDTH_DP (3 dp). */
    fun setStrokeWidthDp(dp: Int): LocationButtonController {
        button.setStrokeWidth((dp * density).toInt())
        return this
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun deliverLocation() {
        val locationManager = context.getSystemService(LocationManager::class.java)
        val location = locationManager.getProviders(true)
            .asSequence()
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .firstOrNull()
        callback?.onLocation(location)
    }

    private fun updateParams(block: (FrameLayout.LayoutParams) -> Unit) {
        val params = button.layoutParams as FrameLayout.LayoutParams
        block(params)
        button.layoutParams = params
    }
}
