package com.pedroid.android17locationbutton.composeapp

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pedroid.android17locationbutton.compose.LocationButtonView
import com.pedroid.android17locationbutton.compose.rememberLocationButtonStateHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationButtonDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DemoScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen(onBack: () -> Unit) {
    val stateHolder = rememberLocationButtonStateHolder()
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    stateHolder.onLocation = { lastLocation = it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.demo_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Tap the button to grant permission and get your location.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "State: ${stateHolder.state}",
                style = MaterialTheme.typography.bodyMedium,
            )
            LocationButtonView(stateHolder = stateHolder)
            lastLocation?.let { loc ->
                Text(
                    text = "Lat: %.6f\nLng: %.6f\n${loc.formattedTime()}".format(
                        loc.latitude,
                        loc.longitude,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun Location.formattedTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))
