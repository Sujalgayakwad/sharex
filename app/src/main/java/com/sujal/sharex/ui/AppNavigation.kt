package com.sujal.sharex.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sujal.sharex.nearby.NearbyManager
import com.sujal.sharex.transfer.TransferManager
import com.sujal.sharex.database.TransferDatabase

@Composable
fun AppNavigation(transferManager: TransferManager, nearbyManager: NearbyManager, database: TransferDatabase) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, database) }
        composable("send") { SendScreen(navController, transferManager, nearbyManager) }
        composable("receive") { ReceiveScreen(navController, transferManager, nearbyManager) }
        composable("settings") { SettingsScreen(navController, database) }
    }
}
