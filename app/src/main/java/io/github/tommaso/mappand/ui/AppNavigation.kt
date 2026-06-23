package io.github.tommaso.mappand.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.ui.auth.LoginScreen
import io.github.tommaso.mappand.ui.map.MapScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object Routes {
    const val LOGIN = "login"
    const val MAP = "map"
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = MappandApp.from(context)
    val navController = rememberNavController()

    val startDestination = remember {
        val token = runBlocking { app.authDataStore.tokenFlow.first() }
        if (token != null) Routes.MAP else Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.MAP) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }
        composable(Routes.MAP) {
            MapScreen(onLogout = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.MAP) { inclusive = true }
                }
            })
        }
    }
}
