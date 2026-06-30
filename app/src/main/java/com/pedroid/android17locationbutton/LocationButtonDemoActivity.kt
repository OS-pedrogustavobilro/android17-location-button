package com.pedroid.android17locationbutton

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.locationbutton.LocationButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LocationButtonDemoActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var locationText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location_button_demo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.demo_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusText = findViewById(R.id.status_text)
        locationText = findViewById(R.id.location_text)

        val locationButton = findViewById<LocationButton>(R.id.location_button)

        locationButton.setOnPermissionResultListener { isGranted ->
            if (isGranted) {
                statusText.text = getString(R.string.status_granted)
                showLastKnownLocation()
            } else {
                statusText.text = getString(R.string.status_denied)
                locationText.visibility = View.GONE
            }
        }

        locationButton.setOnErrorListener { throwable ->
            statusText.text = getString(R.string.status_error, throwable.message ?: "Unknown error")
            locationText.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @SuppressLint("MissingPermission")
    private fun showLastKnownLocation() {
        val locationManager = getSystemService(LocationManager::class.java)
        val providers = locationManager.getProviders(true)
        var location: Location? = null
        for (provider in providers) {
            location = locationManager.getLastKnownLocation(provider)
            if (location != null) break
        }

        if (location != null) {
            locationText.text = "Lat: %.6f\nLng: %.6f".format(location.latitude, location.longitude)
        } else {
            locationText.text = getString(R.string.location_no_cache)
        }
        locationText.visibility = View.VISIBLE
    }
}
