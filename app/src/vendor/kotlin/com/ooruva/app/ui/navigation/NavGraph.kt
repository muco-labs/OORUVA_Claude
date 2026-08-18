package com.ooruva.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composable as ComposableAlias
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ooruva.app.data.models.UserRole
import com.ooruva.app.ui.screens.PhoneAuthScreen
import com.ooruva.app.ui.screens.VendorAnalyticsScreen
import com.ooruva.app.ui.screens.VendorBusinessInfoScreen
import com.ooruva.app.ui.screens.VendorHomeScreen
import com.ooruva.app.ui.screens.VendorPhotosScreen
import com.ooruva.app.ui.screens.VendorProfileScreen

/**
 * Vendor navigation. Compiled into the vendor build only — no customer
 * discovery, rewards, community or admin destination exists in this binary.
 */
sealed class Screen(val route: String) {
    data object Auth : Screen("vendor_auth")
    data object Home : Screen("vendor_home")
    data object BusinessInfo : Screen("vendor_business_info")
    data object Photos : Screen("vendor_photos")
    data object Analytics : Screen("vendor_analytics")
    data object Profile : Screen("vendor_profile")
}

@Composable
fun OoruvaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Auth.route
) {
    val context = LocalContext.current
    fun closeApp() { (context as? android.app.Activity)?.finish() }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination) {

            composable(Screen.Auth.route) {
                PhoneAuthScreen(
                    role = UserRole.VENDOR,
                    headline = "Mind the\nshop.",
                    blurb = "Sign in with the number your customers already call.",
                    onBack = { closeApp() },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                VendorHomeScreen(
                    onOpenBusinessInfo = { navController.navigate(Screen.BusinessInfo.route) },
                    onOpenPhotos = { navController.navigate(Screen.Photos.route) },
                    onOpenAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onOpenProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.BusinessInfo.route) {
                VendorBusinessInfoScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.Photos.route) {
                VendorPhotosScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.Analytics.route) {
                VendorAnalyticsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) {
                VendorProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
