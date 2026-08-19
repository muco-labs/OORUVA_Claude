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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ooruva.app.data.auth.SessionStore
import com.ooruva.app.data.remote.Supabase
import com.ooruva.app.ui.navigation.OoruvaNavGraph
import com.ooruva.app.ui.theme.OoruvaTheme

class MainActivity : ComponentActivity() {

    /** Invoked by the Map screen, at the moment location is actually needed. */
    val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            val fine = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            android.util.Log.d("OORUVA", "Location granted fine=" + fine + " coarse=" + coarse)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hand the Supabase client a way to fetch the current access token.
        // Read per request rather than captured once: the token lasts an hour,
        // so a value snapshotted at startup would start being rejected mid-session
        // with nothing in the logs to explain it. Returns null when signed out,
        // which correctly leaves the client on the anon key -- public reads work,
        // everything owner-scoped fails closed.
        val sessionStore = SessionStore(this)
        Supabase.tokenProvider = {
            sessionStore.load()?.takeIf { it.isValid }?.accessToken
        }

        // Deliberately not requested here. Asking for location before the
        // person has seen a single vendor is the classic way to get a hard
        // denial; the Map screen asks at the point it can explain why.

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
