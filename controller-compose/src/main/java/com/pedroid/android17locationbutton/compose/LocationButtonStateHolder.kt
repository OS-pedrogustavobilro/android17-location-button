package com.pedroid.android17locationbutton.compose

import android.content.Context
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pedroid.android17locationbutton.core.LocationButtonCallback
import com.pedroid.android17locationbutton.core.LocationButtonState
import com.pedroid.android17locationbutton.core.LocationFetcher
import com.pedroid.android17locationbutton.core.LocationStrategy
import kotlinx.coroutines.flow.SharedFlow

/**
 * Compose implementation of the LocationButton controller.
 *
 * Holds observable state for [LocationButtonView] and delegates all location-fetching
 * to [LocationFetcher] from the `controller-core` module.
 *
 * Obtain a lifecycle-aware instance via [rememberLocationButtonStateHolder].
 *
 * For Views apps, see `controller` and `LocationButtonController`.
 *
 * **Interop:**
 * - Embed [LocationButtonView] in a Views layout via `ComposeView`.
 * - Use [LocationButtonController] inside a Compose layout via `AndroidView`.
 */
class LocationButtonStateHolder(context: Context) {

    private val fetcher = LocationFetcher(context)

    var state: LocationButtonState by mutableStateOf(LocationButtonState.IDLE)
        private set

    var isVisible: Boolean by mutableStateOf(true)
        private set

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

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /**
     * Optional lambda-style callback for permission results.
     * Prefer collecting [locationUpdatesFlow] or reading [state] in a composable.
     */
    var onPermissionResult: ((Boolean) -> Unit)? = null

    /**
     * Optional lambda-style callback for location delivery.
     * Prefer collecting [locationUpdatesFlow] for continuous updates.
     */
    var onLocation: ((Location?) -> Unit)? = null

    // ── Read-only state ───────────────────────────────────────────────────────

    /**
     * Emits every location update received in [LocationStrategy.UPDATES] mode.
     * Also emits the last-known fix immediately when updates start.
     */
    val locationUpdatesFlow: SharedFlow<Location> = fetcher.locationUpdatesFlow

    init {
        fetcher.onLocation = { location -> onLocation?.invoke(location) }
    }

    // ── Internal — called by LocationButtonView ───────────────────────────────

    internal fun onPermissionGranted(granted: Boolean) {
        state = if (granted) LocationButtonState.GRANTED else LocationButtonState.DENIED
        if (granted && autoHideOnGranted) hide()
        onPermissionResult?.invoke(granted)
        if (granted && fetchLocationOnGrant) fetcher.deliver()
    }

    // ── Visibility & state ────────────────────────────────────────────────────

    fun show() { isVisible = true }
    fun hide() { isVisible = false }

    fun reset() {
        state = LocationButtonState.IDLE
        isVisible = true
    }

    // ── Location ──────────────────────────────────────────────────────────────

    fun stopLocationUpdates() = fetcher.stop()
    fun startLocationUpdates() = fetcher.deliver()

    fun dispose() = fetcher.stop()
}

/**
 * Creates and remembers a [LocationButtonStateHolder] that is automatically
 * disposed when it leaves the composition.
 */
@Composable
fun rememberLocationButtonStateHolder(
    context: Context = LocalContext.current,
): LocationButtonStateHolder {
    val holder = remember { LocationButtonStateHolder(context) }
    DisposableEffect(Unit) {
        onDispose { holder.dispose() }
    }
    return holder
}

/**
 * Converts this holder to a [LocationButtonCallback] for code that still uses the
 * callback interface (e.g. shared utility functions).
 */
fun LocationButtonStateHolder.asCallback(): LocationButtonCallback {
    val holder = this
    return object : LocationButtonCallback {
        override fun onPermissionResult(granted: Boolean) = holder.onPermissionGranted(granted)
        override fun onLocation(location: Location?) { holder.onLocation?.invoke(location) }
    }
}
