package com.example.grabeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.floatSetOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.grabeat.data.FirebaseRepository
import com.example.grabeat.screens.AuthScreen
import com.example.grabeat.screens.merchant.MerchantMainScreen
import com.example.grabeat.ui.theme.GrabEatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = FirebaseRepository()

        setContent {
            GrabEatTheme {
                var isLoggedIn by remember {
                    mutableStateOf(repository.currentUser() != null)
                }
                var userRole by remember { mutableStateOf<String?>(null) }
                var isLoadingRole by remember { mutableStateOf(false) }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn){
                        isLoadingRole = true
                        val uid = repository.currentUser()?.uid
                        if (uid != null) {
                            repository.getUserRole(uid).onSuccess { role -> userRole = role }.onFailure {
                                userRole = "customer"
                            }
                        }
                        isLoadingRole = false
                    } else {
                        userRole = null
                    }
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    if (isLoggedIn) {
//                        Surface{
//                            TextButton(
//                                onClick = {
//                                    repository.logout()
//                                    isLoggedIn = false
//                                }
//                            ) {
//                                Text("Uitloggen")
//                            }
//                        }
                        if (isLoadingRole){
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                                CircularProgressIndicator()
                            }
                        }else{
                            when (userRole){
                                "merchant" -> {
                                    MerchantMainScreen(onLogout = {
                                        repository.logout()
                                        isLoggedIn = false
                                    })
                                }
                                "customer" -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Welkom")
                                            androidx.compose.material3.Button(onClick = {
                                                repository.logout()
                                                isLoggedIn = false
                                            }) {
                                                Text("Uitloggen")
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Text("Fout rol onbekend")
                                }
                            }
                        }
                    } else {
                        AuthScreen(
                            repository = repository,
                            onLoginSuccess = {
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}
