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
import com.ooruva.app.BuildConfig
import com.ooruva.app.data.models.UserRole
import com.ooruva.app.data.models.Vendor
import com.ooruva.app.ui.components.FabNavigationGroup
import com.ooruva.app.ui.components.customerDestinations
import com.ooruva.app.ui.screens.AdminDashboardScreen
import com.ooruva.app.ui.screens.BusinessDetailScreen
import com.ooruva.app.ui.screens.CommunityScreen
import com.ooruva.app.ui.screens.CustomerAuthScreen
import com.ooruva.app.ui.screens.GroupFinderScreen
import com.ooruva.app.ui.screens.HomeScreen
import com.ooruva.app.ui.screens.MapScreen
import com.ooruva.app.ui.screens.ProfileScreen
import com.ooruva.app.ui.screens.RewardsScreen
import com.ooruva.app.ui.screens.RoleSelectionScreen
import com.ooruva.app.ui.screens.VendorAnalyticsScreen
import com.ooruva.app.ui.screens.VendorAuthScreen
import com.ooruva.app.ui.screens.VendorBusinessInfoScreen
import com.ooruva.app.ui.screens.VendorHomeScreen
import com.ooruva.app.ui.screens.VendorPhotosScreen
import com.ooruva.app.ui.screens.VendorProfileScreen
import com.ooruva.app.ui.screens.getMockVendors

sealed class Screen(val route: String) {
    // Entry
    data object RoleSelection : Screen("role_selection")
    data object CustomerAuth : Screen("customer_auth")
    data object VendorAuth : Screen("vendor_auth")

    // Customer
    data object CustomerHome : Screen("customer_home")
    data object BusinessDetail : Screen("business_detail/{vendorId}") {
        fun createRoute(vendorId: String) = "business_detail/" + vendorId
    }
    data object GroupFinder : Screen("group_finder")
    data object Map : Screen("map")
    data object Community : Screen("community")
    data object Rewards : Screen("rewards")
    data object CustomerProfile : Screen("customer_profile")

    // Vendor
    data object VendorHome : Screen("vendor_home")
    data object VendorBusinessInfo : Screen("vendor_business_info")
    data object VendorPhotos : Screen("vendor_photos")
    data object VendorAnalytics : Screen("vendor_analytics")
    data object VendorProfile : Screen("vendor_profile")

    // Platform
    data object AdminDashboard : Screen("admin_dashboard")
}

/** Routes that carry the floating navigation. */
private val customerRoutes = customerDestinations.map { it.route }.toSet()

/**
 * Each shipped app opens in its own front door. The customer build goes straight
 * to customer sign-in, the vendor build to vendor sign-in; the role gate is only
 * for a combined build where the audience is not known at compile time.
 */
private fun startForAudience(): String = when (BuildConfig.AUDIENCE) {
    "CUSTOMER" -> Screen.CustomerAuth.route
    "VENDOR" -> Screen.VendorAuth.route
    else -> Screen.RoleSelection.route
}

@Composable
fun OoruvaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = startForAudience()
) {
    val allVendors = remember { getMockVendors() }
    var selectedVendor by remember { mutableStateOf<Vendor?>(null) }

    val context = LocalContext.current
    fun activityFinish() { (context as? android.app.Activity)?.finish() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showFab = currentRoute in customerRoutes

    fun switchTab(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.CustomerHome.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun signOut() {
        navController.navigate(startForAudience()) {
            popUpTo(0) { inclusive = true }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination) {

            // ── Entry ──────────────────────────────────────────────────────
            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(
                    onRoleSelected = { role ->
                        navController.navigate(
                            if (role == UserRole.CUSTOMER) Screen.CustomerAuth.route
                            else Screen.VendorAuth.route
                        )
                    }
                )
            }

            composable(Screen.CustomerAuth.route) {
                CustomerAuthScreen(
                    onBack = { if (!navController.popBackStack()) activityFinish() },
                    onLoginSuccess = {
                        navController.navigate(Screen.CustomerHome.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.VendorAuth.route) {
                VendorAuthScreen(
                    onBack = { if (!navController.popBackStack()) activityFinish() },
                    onLoginSuccess = {
                        navController.navigate(Screen.VendorHome.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Customer ───────────────────────────────────────────────────
            composable(Screen.CustomerHome.route) {
                HomeScreen(
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

            composable(Screen.CustomerProfile.route) {
                ProfileScreen(
                    onOpenVendorPortal = { navController.navigate(Screen.VendorHome.route) },
                    onOpenAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                    onLogout = { signOut() }
                )
            }

            // ── Vendor ─────────────────────────────────────────────────────
            composable(Screen.VendorHome.route) {
                VendorHomeScreen(
                    onOpenBusinessInfo = { navController.navigate(Screen.VendorBusinessInfo.route) },
                    onOpenPhotos = { navController.navigate(Screen.VendorPhotos.route) },
                    onOpenAnalytics = { navController.navigate(Screen.VendorAnalytics.route) },
                    onOpenProfile = { navController.navigate(Screen.VendorProfile.route) }
                )
            }
            composable(Screen.VendorBusinessInfo.route) {
                VendorBusinessInfoScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.VendorPhotos.route) {
                VendorPhotosScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.VendorAnalytics.route) {
                VendorAnalyticsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.VendorProfile.route) {
                VendorProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onLogout = { signOut() }
                )
            }

            // ── Platform ───────────────────────────────────────────────────
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(onBackClick = { navController.popBackStack() })
            }
        }

        if (showFab) {
            FabNavigationGroup(
                destinations = customerDestinations,
                currentRoute = currentRoute,
                onNavigate = { route -> switchTab(route) }
            )
        }
    }
}
