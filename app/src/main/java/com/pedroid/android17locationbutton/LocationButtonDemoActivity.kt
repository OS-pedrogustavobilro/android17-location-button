package com.pedroid.android17locationbutton

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LocationButtonDemoActivity : AppCompatActivity(), LocationButtonController.Callback {

    private lateinit var statusText: TextView
    private lateinit var locationText: TextView
    private lateinit var controller: LocationButtonController

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

        controller = LocationButtonController
            .attach(findViewById(R.id.location_button_container))
            .setCallback(this)
            .also { it.fetchLocationOnGrant = true }
    }

    // ── LocationButtonController.Callback ─────────────────────────────────────

    override fun onPermissionResult(granted: Boolean) {
        statusText.text = if (granted) getString(R.string.status_granted)
                          else getString(R.string.status_denied)
        if (!granted) locationText.visibility = View.GONE
    }

    override fun onLocation(location: Location?) {
        locationText.text = if (location != null) {
            "Lat: %.6f\nLng: %.6f".format(location.latitude, location.longitude)
        } else {
            getString(R.string.location_no_cache)
        }
        locationText.visibility = View.VISIBLE
    }

    override fun onError(throwable: Throwable) {
        statusText.text = getString(R.string.status_error, throwable.message ?: "Unknown error")
        locationText.visibility = View.GONE
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.detach()
    }
}
