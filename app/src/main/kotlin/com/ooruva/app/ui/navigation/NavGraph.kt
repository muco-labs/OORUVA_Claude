package com.ooruva.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ooruva.app.data.models.Vendor
import com.ooruva.app.ui.screens.AdminDashboardScreen
import com.ooruva.app.ui.screens.AuthScreen
import com.ooruva.app.ui.screens.BusinessDetailScreen
import com.ooruva.app.ui.screens.CommunityScreen
import com.ooruva.app.ui.screens.GroupFinderScreen
import com.ooruva.app.ui.screens.HomeScreen
import com.ooruva.app.ui.screens.MapScreen
import com.ooruva.app.ui.screens.ProfileScreen
import com.ooruva.app.ui.screens.RewardsScreen
import com.ooruva.app.ui.screens.VendorPortalScreen
import com.ooruva.app.ui.screens.getMockVendors

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object BusinessDetail : Screen("business_detail/{vendorId}") {
        fun createRoute(vendorId: String) = "business_detail/" + vendorId
    }
    data object Community : Screen("community")
    data object Profile : Screen("profile")
    data object GroupFinder : Screen("group_finder")
    data object Map : Screen("map")
    data object Rewards : Screen("rewards")
    data object VendorPortal : Screen("vendor_portal")
    data object AdminDashboard : Screen("admin_dashboard")
}

data class BottomTab(val screen: Screen, val label: String, val icon: ImageVector)

val bottomTabs = listOf(
    BottomTab(Screen.Home, "Home", Icons.Default.Home),
    BottomTab(Screen.GroupFinder, "Groups", Icons.Default.Groups),
    BottomTab(Screen.Map, "Map", Icons.Default.Map),
    BottomTab(Screen.Community, "Feed", Icons.AutoMirrored.Filled.Chat),
    BottomTab(Screen.Rewards, "Rewards", Icons.Default.CardGiftcard),
    BottomTab(Screen.Profile, "You", Icons.Default.Person),
)

@Composable
fun OoruvaNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Auth.route
) {
    val allVendors = remember { getMockVendors() }
    var selectedVendor by remember { mutableStateOf<Vendor?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTabbedScreen = bottomTabs.any { it.screen.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (onTabbedScreen) {
                OoruvaBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Screens own their headers and draw under the status bar, so only the
        // bottom inset is consumed here.
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
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

            composable(Screen.Community.route) { CommunityScreen() }
            composable(Screen.GroupFinder.route) { GroupFinderScreen() }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Rewards.route) { RewardsScreen() }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenVendorPortal = { navController.navigate(Screen.VendorPortal.route) },
                    onOpenAdmin = { navController.navigate(Screen.AdminDashboard.route) }
                )
            }

            composable(Screen.VendorPortal.route) {
                VendorPortalScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun OoruvaBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                selected = selected,
                onClick = { onTabSelected(tab.screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
