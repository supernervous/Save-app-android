package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.LoginSuccess
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdateEmail
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdatePassword
import org.koin.androidx.compose.koinViewModel

@Composable
fun InternetArchiveLoginScreen() {
    val viewModel: InternetArchiveLoginViewModel = koinViewModel()

    val state by viewModel.state.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        viewModel.effects.collect { action ->
            when (action) {
                is CreateLogin -> launcher.launch(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(CreateLogin.URI)
                    )
                )

                else -> Unit
            }
        }
    }

    InternetArchiveLoginContent(state, viewModel::dispatch)
}

@Composable
private fun InternetArchiveLoginContent(state: InternetArchiveLoginState, dispatch: Dispatch<Action>) {

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            state.auth?.let { auth ->
                Text(text = "${auth.access}:${auth.secret}",
                    modifier = Modifier.padding(bottom = 20.dp),
                    color = Color.Red)
            }

            Text(
                text = stringResource(id = R.string.internet_archive),
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            TextField(
                value = state.email,
                onValueChange = { dispatch(UpdateEmail(it)) },
                label = { Text(stringResource(id = R.string.prompt_email)) },
                placeholder = { Text(stringResource(id = R.string.prompt_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Email
                ),
                isError = state.isEmailError
            )

            TextField(
                value = state.password, onValueChange = { dispatch(UpdatePassword(it)) },
                label = { Text(stringResource(id = R.string.prompt_password)) },
                placeholder = { Text(stringResource(id = R.string.prompt_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false,
                    imeAction = ImeAction.Go
                ),
                isError = state.isPasswordError
            )

            if (state.isLoginError) {
                Text(
                    modifier = Modifier.padding(top = 20.dp),
                    text = stringResource(id = R.string.error_incorrect_username_or_password),
                    color = MaterialTheme.colors.error
                )
            }

            Button(
                modifier = Modifier.padding(top = 20.dp),
                onClick = { dispatch(Login) }) {
                Text(stringResource(id = R.string.title_activity_login))
            }

            TextButton(onClick = { dispatch(CreateLogin) }) {
                Text("Create Login")
            }
        }
    }
}

@Composable
@Preview
private fun InternetArchiveLoginPreview() {
    InternetArchiveLoginContent(
        state = InternetArchiveLoginState(
            email = "user@example.org",
            password = "123abc"
        )
    ) {}
}
