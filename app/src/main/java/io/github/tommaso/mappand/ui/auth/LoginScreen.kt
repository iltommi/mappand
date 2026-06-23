package io.github.tommaso.mappand.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.data.auth.AuthDataStore

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val app = MappandApp.from(context)
    val vm: LoginViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(app) as T
    })
    val state by vm.state.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var tfaToken by remember { mutableStateOf<String?>(null) }
    var isEu by remember { mutableStateOf(true) }

    LaunchedEffect(state) {
        when (val s = state) {
            is LoginState.Success -> onLoginSuccess()
            is LoginState.NeedTotp -> tfaToken = s.tfaToken
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Mappand", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Sign in with your pCloud account", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isEu,
                onClick = { isEu = true; vm.selectedHost = AuthDataStore.EU_HOST },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("EU") },
            )
            SegmentedButton(
                selected = !isEu,
                onClick = { isEu = false; vm.selectedHost = AuthDataStore.US_HOST },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("US") },
            )
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is LoginState.Loading,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is LoginState.Loading,
        )

        if (tfaToken != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = totp, onValueChange = { totp = it },
                label = { Text("Authenticator code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.Loading,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (state is LoginState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    val tfa = tfaToken
                    if (tfa != null) vm.submitTotp(tfa, totp)
                    else vm.login(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (tfaToken != null) "Verify" else "Sign in") }
        }

        if (state is LoginState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                (state as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
