package com.ooruva.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    BottomTab(Screen.Community, "Community", Icons.AutoMirrored.Filled.Chat),
    BottomTab(Screen.Rewards, "Rewards", Icons.Default.CardGiftcard),
    BottomTab(Screen.Profile, "Profile", Icons.Default.Person),
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
    val activeTab = bottomTabs.firstOrNull { it.screen.route == currentRoute }

    Scaffold(
        topBar = {
            if (activeTab != null) {
                OoruvaTopBar(
                    title = activeTab.label,
                    onMenuNavigate = { route -> navController.navigate(route) }
                )
            }
        },
        bottomBar = {
            if (activeTab != null) {
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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
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
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.GroupFinder.route) { GroupFinderScreen() }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Rewards.route) { RewardsScreen() }
            composable(Screen.VendorPortal.route) { VendorPortalScreen() }
            composable(Screen.AdminDashboard.route) { AdminDashboardScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OoruvaTopBar(
    title: String,
    onMenuNavigate: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Vendor Portal") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onMenuNavigate(Screen.VendorPortal.route)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Admin Dashboard") },
                    leadingIcon = {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onMenuNavigate(Screen.AdminDashboard.route)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
}

@Composable
fun OoruvaBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 10.sp) },
                selected = currentRoute == tab.screen.route,
                onClick = { onTabSelected(tab.screen.route) }
            )
        }
    }
}
