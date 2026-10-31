package com.example.grabeat.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.grabeat.data.FirebaseRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(repository: FirebaseRepository, onLoginSuccess: () -> Unit){
    var isRegistering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("customer") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading: Boolean by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column( modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "Grab&Eat",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRegistering) "Maak een account aan" else "Welkom terug!",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isRegistering){
            Text(
                text = "Ik wil Grab&Eat gebruiken als:",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Naam") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = role == "customer",
                    onClick = { role = "customer"}
                )
                Text("Klant")

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = role == "merchant",
                    onClick = { role = "merchant"}
                )
                Text("Handelaar")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Wachtwoord") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                errorMessage = ""

                if (email.trim().isEmpty() || password.isEmpty()) {
                    errorMessage = "Vul je e-mail en wachtwoord in."
                }else if(isRegistering && name.trim().isEmpty()) {
                    errorMessage = "Vul je naam in."
                } else if (isRegistering && password.length < 6) {
                    errorMessage = "Je wachtwoord moet minstens 6 tekens bevatten."
                } else {
                    scope.launch {
                        isLoading = true

                        val result = if (isRegistering) {
                            repository.register(name, email, password, role)
                        } else {
                            repository.login(email, password)
                        }

                        isLoading = false

                        result.onSuccess {
                            onLoginSuccess()
                        }.onFailure { error ->
                            errorMessage = error.localizedMessage ?: "Er is iets misgegaan."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading){
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isRegistering) "Registreren" else "Inloggen")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                isRegistering = !isRegistering
                errorMessage = ""
            }
        ) {
            Text(
                if (isRegistering) {
                    "heb je al een account? Log in"
                } else {
                    "Nog geen account? Registreer"
                }
            )
        }
    }






}