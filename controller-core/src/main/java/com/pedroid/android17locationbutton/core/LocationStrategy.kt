package com.pedroid.android17locationbutton.core

enum class LocationStrategy {
    /** Single fresh fix via [androidx.core.location.LocationManagerCompat.getCurrentLocation]. */
    CURRENT,

    /** Continuous stream via [androidx.core.location.LocationManagerCompat.requestLocationUpdates]. */
    UPDATES,
}
