package net.opendasharchive.openarchive.features.internetarchive.presentation.login

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.presentation.theme.ThemeColors
import net.opendasharchive.openarchive.core.presentation.theme.ThemeDimensions
import net.opendasharchive.openarchive.core.presentation.theme.textFieldColors
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.InternetArchiveHeader
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.CreateLogin
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.Login
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdatePassword
import net.opendasharchive.openarchive.features.internetarchive.presentation.login.InternetArchiveLoginViewModel.Action.UpdateUsername
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InternetArchiveLoginScreen(space: Space, onResult: (IAResult) -> Unit) {
    val viewModel: InternetArchiveLoginViewModel = koinViewModel {
        parametersOf(space)
    }

    val state by viewModel.state.collectAsState()

    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {})

    LaunchedEffect(Unit) {
        viewModel.effects.collect { action ->
            when (action) {
                is CreateLogin -> launcher.launch(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse(CreateLogin.URI)
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
    state: InternetArchiveLoginState, dispatch: Dispatch<Action>
) {

    LaunchedEffect(state.isLoginError) {
        while (state.isLoginError) {
            delay(3000)
            dispatch(Action.ErrorClear)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ThemeDimensions.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        InternetArchiveHeader(
            modifier = Modifier.padding(bottom = ThemeDimensions.spacing.large)
        )

        val colors = textFieldColors()

        OutlinedTextField(
            value = state.username,
            enabled = !state.isBusy,
            onValueChange = { dispatch(UpdateUsername(it)) },
            label = {
                Text(
                    text = stringResource(id = R.string.label_username),
                    color = ThemeColors.material.onBackground
                )
            },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.placeholder_email_or_username),
                    color = ThemeColors.material.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                autoCorrect = false,
                keyboardType = KeyboardType.Email
            ),
            isError = state.isUsernameError,
            colors = colors
        )

        Spacer(Modifier.height(ThemeDimensions.spacing.large))

        OutlinedTextField(
            value = state.password,
            enabled = !state.isBusy,
            onValueChange = { dispatch(UpdatePassword(it)) },
            label = {
                Text(
                    stringResource(id = R.string.label_password),
                    color = ThemeColors.material.onBackground
                )
            },
            placeholder = {
                Text(
                    stringResource(id = R.string.placeholder_password),
                    color = ThemeColors.material.onSurfaceVariant
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
            colors = colors,
        )

        AnimatedVisibility(
            visible = state.isLoginError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = stringResource(id = R.string.error_incorrect_username_or_password),
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier
                .padding(top = ThemeDimensions.spacing.medium)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.prompt_no_account),
                color = ThemeColors.material.onSurface
            )
            TextButton(
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(id = R.color.colorPrimary)
                ),
                onClick = { dispatch(CreateLogin) }) {
                Text(
                    text = stringResource(id = R.string.label_create_login),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ThemeDimensions.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(ThemeDimensions.spacing.small),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { dispatch(Action.Cancel) }) {
                Text(
                    text = stringResource(id = R.string.action_cancel)
                )
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .padding(ThemeDimensions.spacing.small),
                enabled = !state.isBusy && state.isValid,
                shape = RoundedCornerShape(ThemeDimensions.roundedCorner),
                onClick = { dispatch(Login) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeColors.material.primaryContainer,
                    contentColor = ThemeColors.material.onPrimaryContainer,
                    disabledContainerColor = ThemeColors.disabledContainer,
                    disabledContentColor = ThemeColors.onDisabledContainer
                )
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(color = colorResource(id = R.color.colorPrimary))
                } else {
                    Text(
                        text = stringResource(id = R.string.label_login),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun InternetArchiveLoginPreview() {
    InternetArchiveLoginContent(
        state = InternetArchiveLoginState(
            username = "user@example.org", password = "abc123"
        )
    ) {}
}
