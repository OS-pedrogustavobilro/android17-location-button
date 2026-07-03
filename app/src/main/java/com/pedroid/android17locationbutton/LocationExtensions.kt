package com.pedroid.android17locationbutton

import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

/** Returns the location's timestamp formatted as HH:mm:ss in the device locale. */
fun Location.formattedTime(): String = timeFormatter.format(Date(time))
