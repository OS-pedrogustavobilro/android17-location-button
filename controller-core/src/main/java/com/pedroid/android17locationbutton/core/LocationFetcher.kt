package com.pedroid.android17locationbutton.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Handles all location-fetching logic independently of the UI layer.
 * Used by both [com.pedroid.android17locationbutton.LocationButtonController] (Views) and
 * [com.pedroid.android17locationbutton.compose.LocationButtonStateHolder] (Compose).
 */
class LocationFetcher(private val context: Context) {

    var strategy: LocationStrategy = LocationStrategy.CURRENT
    var updateIntervalMs: Long = 5_000L
    var updateMinDistanceM: Float = 0f

    private var locationListener: LocationListenerCompat? = null
    private var cancellationSignal: CancellationSignal? = null

    private val _locationUpdatesFlow = MutableSharedFlow<Location>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val locationUpdatesFlow: SharedFlow<Location> = _locationUpdatesFlow.asSharedFlow()

    /** Invoked on every delivered location (last-known, current, or update). */
    var onLocation: ((Location?) -> Unit)? = null

    fun deliver() {
        when (strategy) {
            LocationStrategy.CURRENT -> deliverCurrent()
            LocationStrategy.UPDATES -> startUpdates()
        }
    }

    fun stop() {
        cancellationSignal?.cancel()
        cancellationSignal = null
        locationListener?.let {
            context.getSystemService(LocationManager::class.java).removeUpdates(it)
        }
        locationListener = null
    }

    @SuppressLint("MissingPermission")
    private fun deliverCurrent() {
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = getBestProvider(lm) ?: run {
            onLocation?.invoke(getLastKnown(lm))
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
            onLocation?.invoke(freshLocation ?: getLastKnown(lm))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        stop()
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = getBestProvider(lm) ?: return

        // Deliver the last cached fix immediately so the caller has something to show
        // while waiting for the first real update to arrive.
        getLastKnown(lm)?.let { cached ->
            _locationUpdatesFlow.tryEmit(cached)
            onLocation?.invoke(cached)
        }

        val request = LocationRequestCompat.Builder(updateIntervalMs)
            .setMinUpdateDistanceMeters(updateMinDistanceM)
            .build()
        val listener = LocationListenerCompat { location ->
            _locationUpdatesFlow.tryEmit(location)
            onLocation?.invoke(location)
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
            else                                       -> active.firstOrNull()
        }
    }
}
