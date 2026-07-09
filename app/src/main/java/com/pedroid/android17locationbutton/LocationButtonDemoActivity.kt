package com.pedroid.android17locationbutton

import android.annotation.SuppressLint
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.locationbutton.LocationButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

class LocationButtonDemoActivity : AppCompatActivity() {

    private lateinit var button: LocationButton
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

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        statusText = findViewById(R.id.status_text)
        locationText = findViewById(R.id.location_text)
        button = findViewById(R.id.location_button)

        button.setOnPermissionResultListener { granted ->
            statusText.text = if (granted) getString(R.string.status_granted)
                              else getString(R.string.status_denied)
            if (granted) fetchLastKnownLocation() else locationText.visibility = View.GONE
        }
        button.setOnErrorListener { e ->
            statusText.text = getString(R.string.status_error, e.message ?: "Unknown error")
            locationText.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocation() {
        val lm = getSystemService(LocationManager::class.java)
        val location = lm.getProviders(true)
            .asSequence()
            .mapNotNull { lm.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
        locationText.text = if (location != null) {
            "Lat: %.6f\nLng: %.6f\n@ %s".format(
                location.latitude, location.longitude, location.formattedTime()
            )
        } else {
            getString(R.string.location_no_cache)
        }
        locationText.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
