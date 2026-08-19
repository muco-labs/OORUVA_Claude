package com.ooruva.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ooruva.app.data.auth.AuthRepository
import com.ooruva.app.data.auth.SessionStore
import com.ooruva.app.data.models.UserRole
import com.ooruva.app.ui.onboarding.OnboardingViewModel
import com.ooruva.app.ui.onboarding.VendorOnboardingScreen
import com.ooruva.app.ui.components.FloatingNavBar
import com.ooruva.app.ui.components.vendorNavDestinations
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
    data object Onboarding : Screen("vendor_onboarding")
    data object Home : Screen("vendor_home")
    data object BusinessInfo : Screen("vendor_business_info")
    data object Photos : Screen("vendor_photos")
    data object Analytics : Screen("vendor_analytics")
    data object Profile : Screen("vendor_profile")
}

private val chromeRoutes = vendorNavDestinations.map { it.route }.toSet()

@Composable
fun OoruvaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Auth.route
) {
    val context = LocalContext.current
    fun closeApp() { (context as? android.app.Activity)?.finish() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in chromeRoutes

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
                    },
                    onNeedsOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
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

            composable(Screen.Onboarding.route) {
                val context = LocalContext.current
                val session = remember { SessionStore(context).load() }
                if (session == null) {
                    // No session means the token expired while the vendor was
                    // away. Send them back to sign in rather than starting a
                    // draft that cannot be saved to anything.
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Auth.route) { popUpTo(0) { inclusive = true } }
                    }
                } else {
                    val vm: OnboardingViewModel = viewModel(
                        factory = onboardingFactory(session.userId)
                    )
                    VendorOnboardingScreen(
                        viewModel = vm,
                        onFinished = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        },
                        onExit = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
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

        if (showChrome) {
            FloatingNavBar(
                destinations = vendorNavDestinations,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

/**
 * OnboardingViewModel needs the signed-in vendor's OORUVA user id, which is not
 * something a no-arg ViewModel constructor can reach. A minimal factory is less
 * machinery than pulling in a DI framework for one screen.
 */
private fun onboardingFactory(vendorUserId: String) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        OnboardingViewModel(vendorUserId) as T
}
