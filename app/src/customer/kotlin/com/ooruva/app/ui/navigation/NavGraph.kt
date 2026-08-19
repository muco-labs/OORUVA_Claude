package com.ooruva.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ooruva.app.data.mock.getMockVendors
import com.ooruva.app.data.models.UserRole
import com.ooruva.app.data.models.Vendor
import com.ooruva.app.ui.components.FloatingNavBar
import com.ooruva.app.ui.components.customerNavDestinations
import com.ooruva.app.ui.screens.BusinessDetailScreen
import com.ooruva.app.ui.screens.CommunityScreen
import com.ooruva.app.ui.screens.GroupFinderScreen
import com.ooruva.app.ui.screens.HomeScreen
import com.ooruva.app.ui.screens.MapScreen
import com.ooruva.app.ui.screens.PhoneAuthScreen
import com.ooruva.app.ui.screens.ProfileScreen
import com.ooruva.app.ui.screens.RewardsScreen

/**
 * Customer navigation. This graph is compiled into the customer build only —
 * vendor and admin destinations are not hidden here, they do not exist in this
 * binary at all (spec §4: enforce separation, do not gate with conditionals).
 */
sealed class Screen(val route: String) {
    data object Auth : Screen("customer_auth")
    data object Home : Screen("customer_home")
    data object BusinessDetail : Screen("business_detail/{vendorId}") {
        fun createRoute(vendorId: String) = "business_detail/" + vendorId
    }
    data object GroupFinder : Screen("group_finder")
    data object Map : Screen("map")
    data object Community : Screen("community")
    data object Rewards : Screen("rewards")
    data object Profile : Screen("customer_profile")
}

private val chromeRoutes = customerNavDestinations.map { it.route }.toSet()

@Composable
fun OoruvaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Auth.route
) {
    val allVendors = remember { getMockVendors() }
    var selectedVendor by remember { mutableStateOf<Vendor?>(null) }

    val context = LocalContext.current
    fun closeApp() { (context as? android.app.Activity)?.finish() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in chromeRoutes

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination) {

            composable(Screen.Auth.route) {
                PhoneAuthScreen(
                    role = UserRole.CUSTOMER,
                    headline = "Welcome to\nOoruva.",
                    blurb = "The chai stalls, samosa carts and juice corners of your street — kept.",
                    onBack = { closeApp() },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenGroupFinder = { navController.navigate(Screen.GroupFinder.route) },
                    onVendorClick = { vendor ->
                        selectedVendor = vendor
                        navController.navigate(Screen.BusinessDetail.createRoute(vendor.id))
                    }
                )
            }

            composable(Screen.BusinessDetail.route) { entry ->
                val vendorId = entry.arguments?.getString("vendorId")
                val vendor = selectedVendor ?: allVendors.firstOrNull { it.id == vendorId }
                if (vendor != null) {
                    BusinessDetailScreen(
                        vendor = vendor,
                        onBackClick = { navController.popBackStack() },
                        onCheckIn = {
                            android.util.Log.d("OORUVA", "Checked in at " + vendor.name)
                        }
                    )
                }
            }

            composable(Screen.GroupFinder.route) { GroupFinderScreen() }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Community.route) { CommunityScreen() }
            composable(Screen.Rewards.route) { RewardsScreen() }

            composable(Screen.Profile.route) {
                ProfileScreen(
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
                destinations = customerNavDestinations,
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
