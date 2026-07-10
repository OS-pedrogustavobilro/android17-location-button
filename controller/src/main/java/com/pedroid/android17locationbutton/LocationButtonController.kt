package com.pedroid.android17locationbutton

import android.content.Context
import android.location.Location
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.locationbutton.LocationButton
import androidx.core.view.setPadding
import com.pedroid.android17locationbutton.core.LocationButtonCallback
import com.pedroid.android17locationbutton.core.LocationButtonState
import com.pedroid.android17locationbutton.core.LocationFetcher
import com.pedroid.android17locationbutton.core.LocationStrategy
import kotlinx.coroutines.flow.SharedFlow

/**
 * Views implementation of the LocationButton controller.
 *
 * Manages a [LocationButton] injected at runtime into a [FrameLayout] container.
 * Location fetching is delegated to [LocationFetcher] from the `controller-core` module,
 * which is shared with the Compose implementation.
 *
 * For Compose apps, see `controller-compose` and `LocationButtonStateHolder`.
 *
 * Obtain an instance via [attach].
 */
class LocationButtonController private constructor(
    private val container: FrameLayout,
) {

    /** Controls which label text the [LocationButton] displays. */
    enum class TextType(internal val constant: Int) {
        NONE(LocationButton.TEXT_TYPE_NONE),
        PRECISE_LOCATION(LocationButton.TEXT_TYPE_PRECISE_LOCATION),
        USE_PRECISE_LOCATION(LocationButton.TEXT_TYPE_USE_PRECISE_LOCATION),
        SHARE_PRECISE_LOCATION(LocationButton.TEXT_TYPE_SHARE_PRECISE_LOCATION),
        NEAR_MY_PRECISE_LOCATION(LocationButton.TEXT_TYPE_NEAR_MY_PRECISE_LOCATION),
        NEAR_YOUR_PRECISE_LOCATION(LocationButton.TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION),
    }

    private val context: Context = container.context
    private val button: LocationButton = LocationButton(context).apply {
        id = View.generateViewId()
    }
    private val fetcher = LocationFetcher(context)

    private var _state: LocationButtonState = LocationButtonState.IDLE
    private var callback: LocationButtonCallback? = null

    // ── Configuration ─────────────────────────────────────────────────────────

    var locationStrategy: LocationStrategy
        get() = fetcher.strategy
        set(value) { fetcher.strategy = value }

    var locationUpdateIntervalMs: Long
        get() = fetcher.updateIntervalMs
        set(value) { fetcher.updateIntervalMs = value }

    var locationUpdateMinDistanceM: Float
        get() = fetcher.updateMinDistanceM
        set(value) { fetcher.updateMinDistanceM = value }

    /** When true, hides the button automatically after permission is granted. */
    var autoHideOnGranted: Boolean = false

    /** When true, fetches / starts location after permission is granted. */
    var fetchLocationOnGrant: Boolean = false

    // ── Read-only state ───────────────────────────────────────────────────────

    val state: LocationButtonState get() = _state
    val isVisible: Boolean get() = button.visibility == View.VISIBLE

    /**
     * Emits every location update received in [LocationStrategy.UPDATES] mode.
     * Collect this as an alternative to (or alongside) [LocationButtonCallback.onLocation].
     */
    val locationUpdatesFlow: SharedFlow<Location> = fetcher.locationUpdatesFlow

    // ── Initialisation ────────────────────────────────────────────────────────

    init {
        fetcher.onLocation = { location -> callback?.onLocation(location) }

        button.setOnPermissionResultListener { granted ->
            _state = if (granted) LocationButtonState.GRANTED else LocationButtonState.DENIED
            if (granted && autoHideOnGranted) hide()
            callback?.onPermissionResult(granted)
            if (granted && fetchLocationOnGrant) fetcher.deliver()
        }
        button.setOnErrorListener { e ->
            _state = LocationButtonState.ERROR
            callback?.onError(e)
        }
        button.setPadding((4 * context.resources.displayMetrics.density).toInt())
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

    fun setCallback(callback: LocationButtonCallback?): LocationButtonController {
        this.callback = callback
        return this
    }

    fun detach() {
        fetcher.stop()
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
        _state = LocationButtonState.IDLE
        return show()
    }

    // ── Location ──────────────────────────────────────────────────────────────

    fun stopLocationUpdates(): LocationButtonController {
        fetcher.stop()
        return this
    }

    // ── Position ──────────────────────────────────────────────────────────────

    fun setGravityPosition(gravity: Int): LocationButtonController {
        updateParams {
            it.gravity = gravity
            it.marginStart = 0; it.topMargin = 0; it.marginEnd = 0; it.bottomMargin = 0
        }
        return this
    }

    fun setMargin(px: Int): LocationButtonController = setMargin(px, px, px, px)

    fun setMargin(start: Int, top: Int, end: Int, bottom: Int): LocationButtonController {
        updateParams { it.marginStart = start; it.topMargin = top; it.marginEnd = end; it.bottomMargin = bottom }
        return this
    }

    // ── Dimensions ────────────────────────────────────────────────────────────

    fun setWidth(px: Int): LocationButtonController {
        updateParams { it.width = if (px == 0) FrameLayout.LayoutParams.WRAP_CONTENT else px }
        return this
    }

    // ── Appearance ────────────────────────────────────────────────────────────

    fun setTextType(textType: TextType): LocationButtonController {
        button.setTextType(textType.constant)
        return this
    }

    fun setCornerRadius(px: Float): LocationButtonController {
        button.setCornerRadius(px)
        return this
    }

    fun setPressedCornerRadius(px: Float): LocationButtonController {
        button.setPressedCornerRadius(px)
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

    /** [px] is capped internally by the library at MAX_STROKE_WIDTH_DP (3 dp). */
    fun setStrokeWidth(px: Float): LocationButtonController {
        button.setStrokeWidth(px.toInt())
        return this
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun updateParams(block: (FrameLayout.LayoutParams) -> Unit) {
        val params = button.layoutParams as FrameLayout.LayoutParams
        block(params)
        button.layoutParams = params
    }
}
