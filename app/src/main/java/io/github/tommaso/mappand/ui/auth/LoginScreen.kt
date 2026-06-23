package io.github.tommaso.mappand.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
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

    val tfaToken = (state as? LoginState.NeedTotp)?.tfaToken

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var isEu by remember { mutableStateOf(true) }

    val totpFocus = remember { FocusRequester() }

    LaunchedEffect(state) {
        if (state is LoginState.Success) onLoginSuccess()
    }

    LaunchedEffect(tfaToken) {
        if (tfaToken != null) totpFocus.requestFocus()
    }

    val loading = state is LoginState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))

        Text("Mappand", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text("Sign in with your pCloud account", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

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
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (tfaToken == null) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (tfaToken == null) vm.login(email, password) },
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )

        if (tfaToken != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = totp,
                onValueChange = { totp = it },
                label = { Text("Authenticator code") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { vm.submitTotp(tfaToken, totp) },
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(totpFocus),
                enabled = !loading,
            )
        }
        Spacer(Modifier.height(24.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (tfaToken != null) vm.submitTotp(tfaToken, totp)
                    else vm.login(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (tfaToken != null) "Verify" else "Sign in")
            }
        }

        if (state is LoginState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                (state as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
