package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.InternetArchiveHeader
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdateEmail
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdatePassword
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InternetArchiveLoginScreen(space: Space, onResult: (IAResult) -> Unit) {
    val viewModel: InternetArchiveLoginViewModel = koinViewModel {
        parametersOf(space)
    }

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

                is Action.Cancel -> onResult(IAResult.Cancelled)

                is Action.LoginSuccess -> onResult(IAResult.Saved)

                else -> Unit
            }
        }
    }

    InternetArchiveLoginContent(state, viewModel::dispatch)
}

@Composable
private fun InternetArchiveLoginContent(
    state: InternetArchiveLoginState,
    dispatch: Dispatch<Action>
) {

    LaunchedEffect(state.isLoginError) {
        while (state.isLoginError) {
            delay(3000)
            dispatch(Action.ErrorFade)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            InternetArchiveHeader(
                modifier = Modifier.padding(bottom = 24.dp),
                titleSize = 32.sp
            )

            TextField(
                value = state.email,
                enabled = !state.isBusy,
                onValueChange = { dispatch(UpdateEmail(it)) },
                label = {
                    Text(
                        text = stringResource(id = R.string.prompt_email),
                        color = colorResource(id = R.color.colorPrimary)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Email
                ),
                isError = state.isEmailError,
                colors = colorResource(id = R.color.colorPrimary).let {
                    TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = it,
                        focusedLabelColor = it,
                        cursorColor = it
                    )
                }
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                value = state.password,
                enabled = !state.isBusy,
                onValueChange = { dispatch(UpdatePassword(it)) },
                label = {
                    Text(
                        stringResource(id = R.string.prompt_password),
                        color = colorResource(id = R.color.colorPrimary)
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false,
                    imeAction = ImeAction.Go
                ),
                isError = state.isPasswordError,
                colors = colorResource(id = R.color.colorPrimary).let {
                    TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = it,
                        focusedLabelColor = it,
                        cursorColor = it
                    )
                }
            )

            AnimatedVisibility(
                modifier = Modifier.padding(top = 20.dp),
                visible = state.isLoginError,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Text(
                    text = stringResource(id = R.string.error_incorrect_username_or_password),
                    color = MaterialTheme.colors.error
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                OutlinedButton(
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
                    onClick = { dispatch(Action.Cancel) }
                ) {
                    Text(stringResource(id = R.string.action_cancel))
                }
                Button(
                    enabled = !state.isBusy,
                    onClick = { dispatch(Login) },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.colorPrimary),
                        contentColor = colorResource(id = R.color.colorBackground)
                    )
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(color = colorResource(id = R.color.colorBackground))
                    } else {
                        Text(
                            text = stringResource(id = R.string.title_activity_login),
                            fontSize = 18.sp,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No account?"
                )
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = colorResource(id = R.color.colorPrimary)),
                    onClick = { dispatch(CreateLogin) }) {
                    Text(text = "Create Login", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun InternetArchiveLoginPreview() {
    InternetArchiveLoginContent(
        state = InternetArchiveLoginState(
            email = "user@example.org",
            password = "abc123"
        )
    ) {}
}
