package com.example.grabeat.screens.merchant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.grabeat.R
import com.example.grabeat.data.FirebaseRepository

sealed class MerchantScreen(val route: String, val title: String, val iconResId: Int){
    object Home : MerchantScreen("home", "Home", R.drawable.home)
    object AddShop : MerchantScreen("add_shop", "Winkel", R.drawable.store)
    object AddPackage : MerchantScreen("add_package", "Pakket", R.drawable.package_2)
    object Profile : MerchantScreen("profile", "Profiel", R.drawable.account_circle)
}

@Composable
fun MerchantMainScreen(repository: FirebaseRepository, onLogout: () -> Unit){
    val navController = rememberNavController()
    val items = listOf(
        MerchantScreen.Home,
        MerchantScreen.AddShop,
        MerchantScreen.AddPackage,
        MerchantScreen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(painter = painterResource(id = screen.iconResId),
                            contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MerchantScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MerchantScreen.Home.route) { PlaceholderScreen("Home Pagina") }
            composable(MerchantScreen.AddShop.route) { AddShopScreen(repository = repository) }
            composable(MerchantScreen.AddPackage.route) { AddPackageScreen(repository = repository) }
            composable(MerchantScreen.Profile.route) {
                ProfilePlaceholderScreen("Profiel", onLogout)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun ProfilePlaceholderScreen(title: String, onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onLogout, modifier = Modifier.padding(top = 16.dp)) {
                Text("Uitloggen")
            }
        }
    }
}
