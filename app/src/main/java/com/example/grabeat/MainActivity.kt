package com.example.grabeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.*
import com.example.grabeat.data.FirebaseRepository
import com.example.grabeat.screens.AuthScreen
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

                Surface(color = MaterialTheme.colorScheme.background) {
                    if (isLoggedIn) {
                        Surface{
                            TextButton(
                                onClick = {
                                    repository.logout()
                                    isLoggedIn = false
                                }
                            ) {
                                Text("Uitloggen")
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
