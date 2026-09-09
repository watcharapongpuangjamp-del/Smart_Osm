package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.viewmodel.PersonViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "หน้าแรก", Icons.Filled.Dashboard)
    object Households : BottomNavItem("households", "ครัวเรือน", Icons.Filled.Home)
    object Map : BottomNavItem("map", "แผนที่", Icons.Filled.LocationOn)
    object Info : BottomNavItem("developer_info", "ข้อมูล อสม.", Icons.Filled.Info)
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: PersonViewModel,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Households,
        BottomNavItem.Map,
        BottomNavItem.Info
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Show bottom bar only on main tabs
            if (currentRoute in items.map { it.route }) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Households.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(BottomNavItem.Households.route) {
                HouseholdListScreen(
                    viewModel = viewModel,
                    onHouseClick = { householdId -> navController.navigate("house_detail/$householdId") },
                    onAddHouseClick = { navController.navigate("household_form/-1") }
                )
            }
            composable(BottomNavItem.Map.route) {
                MapScreen(
                    viewModel = viewModel,
                    onHouseClick = { householdId -> navController.navigate("house_detail/$householdId") }
                )
            }
            composable(BottomNavItem.Info.route) {
                DeveloperInfoScreen()
            }
            
            composable(
                route = "house_detail/{householdId}",
                arguments = listOf(navArgument("householdId") { type = NavType.LongType })
            ) { backStackEntry ->
                val householdId = backStackEntry.arguments?.getLong("householdId") ?: -1L
                HouseDetailScreen(
                    viewModel = viewModel,
                    householdId = householdId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddMemberClick = { navController.navigate("person_form/-1?householdId=$householdId") },
                    onEditMemberClick = { personId -> navController.navigate("person_form/$personId?householdId=$householdId") }
                )
            }

            composable(
                route = "household_form/{householdId}",
                arguments = listOf(navArgument("householdId") { type = NavType.LongType })
            ) { backStackEntry ->
                val householdId = backStackEntry.arguments?.getLong("householdId") ?: -1L
                HouseholdFormScreen(
                    viewModel = viewModel,
                    householdId = householdId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { newId ->
                        navController.popBackStack()
                        navController.navigate("house_detail/$newId")
                    }
                )
            }

            composable(
                route = "person_form/{personId}?householdId={householdId}",
                arguments = listOf(
                    navArgument("personId") { type = NavType.LongType },
                    navArgument("householdId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val personId = backStackEntry.arguments?.getLong("personId") ?: -1L
                val householdId = backStackEntry.arguments?.getLong("householdId") ?: -1L
                PersonFormScreen(
                    viewModel = viewModel,
                    personId = personId,
                    householdId = householdId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
