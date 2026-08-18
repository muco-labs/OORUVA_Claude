package com.ooruva.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ooruva.app.ui.navigation.OoruvaNavGraph
import com.ooruva.app.ui.theme.OoruvaTheme

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            android.util.Log.d("OORUVA", "Location granted fine=" + fine + " coarse=" + coarse)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Nearby vendors and the map screen need location, so ask on startup.
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            OoruvaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OoruvaNavGraph()
                }
            }
        }
    }
}
