package com.pedroid.android17locationbutton

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.locationbutton.LocationButton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manages a [LocationButton] injected at runtime into a [FrameLayout] container.
 *
 * Responsibilities:
 *  - Creates and attaches the [LocationButton] programmatically (no XML required)
 *  - Owns permission-result and error wiring
 *  - Supports three [LocationStrategy] modes for resolving the location after a button tap
 *  - Tracks a [State] and exposes [show] / [hide] / [reset] controls
 *  - Provides dp-based fluent setters for every public [LocationButton] API
 *
 * Obtain an instance via [attach].
 */
class LocationButtonController private constructor(
    private val container: FrameLayout,
) {

    // ── Public API types ──────────────────────────────────────────────────────

    enum class State { IDLE, GRANTED, DENIED, ERROR }

    /**
     * Determines how the controller resolves the device location once the
     * [LocationButton] grants permission.
     */
    enum class LocationStrategy {
        /**
         * Reads the last cached fix from any enabled provider.
         * Fast but may return a stale or null result.
         */
        LAST_KNOWN,

        /**
         * Requests a single fresh fix via [LocationManagerCompat.getCurrentLocation].
         * Delivers null if no fix arrives before the system cancels the request.
         * A previous in-flight request is cancelled when a new button tap occurs.
         */
        CURRENT,

        /**
         * Starts continuous updates via [LocationManagerCompat.requestLocationUpdates].
         * Each update fires [Callback.onLocation] **and** emits to [locationUpdatesFlow].
         * Call [stopLocationUpdates] to cancel.
         */
        UPDATES,
    }

    interface Callback {
        fun onPermissionResult(granted: Boolean)
        fun onLocation(location: Location?) {}
        fun onError(throwable: Throwable) {}
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private val context: Context = container.context
    private val density: Float = context.resources.displayMetrics.density

    private val button: LocationButton = LocationButton(context).apply {
        id = View.generateViewId()
    }

    private var _state: State = State.IDLE
    private var callback: Callback? = null

    // Active request handles
    private var locationListener: LocationListenerCompat? = null
    private var cancellationSignal: CancellationSignal? = null

    // Flow for UPDATES mode — replay=0 so late collectors don't get stale data
    private val _locationUpdatesFlow = MutableSharedFlow<Location>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // ── Configuration ─────────────────────────────────────────────────────────

    /** Strategy used to resolve the location after the button grants permission. */
    var locationStrategy: LocationStrategy = LocationStrategy.LAST_KNOWN

    /** Desired update interval for [LocationStrategy.UPDATES] mode (milliseconds). */
    var locationUpdateIntervalMs: Long = 5_000L

    /** Minimum displacement (metres) between updates in [LocationStrategy.UPDATES] mode. */
    var locationUpdateMinDistanceM: Float = 0f

    /** When true, hides the button automatically after permission is granted. */
    var autoHideOnGranted: Boolean = false

    /** When true, fetches / starts location after permission is granted. */
    var fetchLocationOnGrant: Boolean = false

    // ── Read-only state ───────────────────────────────────────────────────────

    val state: State get() = _state
    val isVisible: Boolean get() = button.visibility == View.VISIBLE

    /**
     * Emits every location update received in [LocationStrategy.UPDATES] mode.
     * Callers who prefer coroutines can collect this instead of (or alongside)
     * [Callback.onLocation].
     */
    val locationUpdatesFlow: SharedFlow<Location> = _locationUpdatesFlow.asSharedFlow()

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
        fun attach(container: FrameLayout): LocationButtonController =
            LocationButtonController(container)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun setCallback(callback: Callback?): LocationButtonController {
        this.callback = callback
        return this
    }

    fun detach() {
        stopLocationUpdates()
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

    fun reset(): LocationButtonController {
        _state = State.IDLE
        return show()
    }

    // ── Location delivery ─────────────────────────────────────────────────────

    /**
     * Cancels any in-flight [LocationStrategy.CURRENT] request or active
     * [LocationStrategy.UPDATES] stream. Safe to call at any time.
     */
    fun stopLocationUpdates() {
        cancellationSignal?.cancel()
        cancellationSignal = null
        locationListener?.let {
            context.getSystemService(LocationManager::class.java).removeUpdates(it)
        }
        locationListener = null
    }

    private fun deliverLocation() {
        when (locationStrategy) {
            LocationStrategy.LAST_KNOWN -> deliverLastKnown()
            LocationStrategy.CURRENT    -> deliverCurrent()
            LocationStrategy.UPDATES    -> startUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun deliverLastKnown() {
        val lm = context.getSystemService(LocationManager::class.java)
        callback?.onLocation(getLastKnown(lm))
    }

    @SuppressLint("MissingPermission")
    private fun deliverCurrent() {
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = getBestProvider(lm) ?: run {
            // No active provider — fall back to whatever is cached
            callback?.onLocation(getLastKnown(lm))
            return
        }
        cancellationSignal?.cancel()
        cancellationSignal = CancellationSignal()
        LocationManagerCompat.getCurrentLocation(
            lm,
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(context),
        ) { freshLocation ->
            // getCurrentLocation delivers null when no fix arrives before the internal timeout.
            // Fall back to the most-recent cached fix so the caller always gets something.
            callback?.onLocation(freshLocation ?: getLastKnown(lm))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        stopLocationUpdates()
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = getBestProvider(lm) ?: return

        // Deliver the last cached fix immediately so the caller has something to show
        // while waiting for the first real update to arrive.
        getLastKnown(lm)?.let { cached ->
            _locationUpdatesFlow.tryEmit(cached)
            callback?.onLocation(cached)
        }

        val request = LocationRequestCompat.Builder(locationUpdateIntervalMs)
            .setMinUpdateDistanceMeters(locationUpdateMinDistanceM)
            .build()
        val listener = LocationListenerCompat { location ->
            _locationUpdatesFlow.tryEmit(location)
            callback?.onLocation(location)
        }
        LocationManagerCompat.requestLocationUpdates(
            lm,
            provider,
            request,
            ContextCompat.getMainExecutor(context),
            listener,
        )
        locationListener = listener
    }

    /**
     * Returns the most-recently cached location across all currently active providers,
     * or null if no fix has ever been recorded.
     */
    @SuppressLint("MissingPermission")
    private fun getLastKnown(lm: LocationManager): Location? =
        lm.getProviders(true)
            .asSequence()
            .mapNotNull { lm.getLastKnownLocation(it) }
            .maxByOrNull { it.time }

    private fun getBestProvider(lm: LocationManager): String? {
        val active = lm.getProviders(true)
        return when {
            LocationManager.GPS_PROVIDER in active     -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in active -> LocationManager.NETWORK_PROVIDER
            else -> active.firstOrNull()
        }
    }

    // ── Position ──────────────────────────────────────────────────────────────

    fun setGravityPosition(gravity: Int): LocationButtonController {
        updateParams {
            it.gravity = gravity
            it.setMargins(0, 0, 0, 0)
        }
        return this
    }

    fun setCustomPosition(xPx: Int, yPx: Int): LocationButtonController {
        updateParams {
            it.gravity = Gravity.TOP or Gravity.START
            it.setMargins(xPx, yPx, 0, 0)
        }
        return this
    }

    fun setMarginDp(dp: Int): LocationButtonController {
        val px = (dp * density).toInt()
        updateParams { it.setMargins(px, px, px, px) }
        return this
    }

    // ── Dimensions ────────────────────────────────────────────────────────────

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

    private fun updateParams(block: (FrameLayout.LayoutParams) -> Unit) {
        val params = button.layoutParams as FrameLayout.LayoutParams
        block(params)
        button.layoutParams = params
    }
}
