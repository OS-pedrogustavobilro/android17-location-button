package com.pedroid.android17locationbutton.core

import android.location.Location

interface LocationButtonCallback {
    fun onPermissionResult(granted: Boolean)
    fun onLocation(location: Location?) {}
    fun onError(throwable: Throwable) {}
}
