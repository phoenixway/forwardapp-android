package com.romankozak.forwardappmobile.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
private const val TAG = "AuthFlowDebug"
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {

    Log.d(TAG, "AuthScreen Composable успішно викликано.")

    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("my-android-device") }

    LaunchedEffect(uiState.isAuthSuccess) {
        if (uiState.isAuthSuccess) {
            onAuthSuccess()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Passkey Автентифікація", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Ім'я користувача (ID пристрою)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick =
                        {
                            Log.d(TAG, "Кнопка 'Зареєструвати' натиснута.")
                        viewModel.register(username)
                              },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. Зареєструвати цей пристрій (Passkey)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.login(username) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. Увійти з Passkey")
                }
            }

            uiState.error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}