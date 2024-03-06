package net.opendasharchive.openarchive.features.internetarchive.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveViewModel.Action
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.IAResult
import net.opendasharchive.openarchive.features.internetarchive.presentation.components.InternetArchiveHeader
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Date

@Composable
fun InternetArchiveScreen(space: Space, onResult: (IAResult) -> Unit) {
    val viewModel: InternetArchiveViewModel = koinViewModel {
        parametersOf(space)
    }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { action ->
            when (action) {
                is Action.Remove ->  onResult(IAResult.Deleted)
                is Action.Cancel ->  onResult(IAResult.Cancelled)
            }
        }
    }

    InternetArchiveContent(state, viewModel::dispatch)
}

@Composable
private fun InternetArchiveContent(state: InternetArchiveState, dispatch: Dispatch<Action>) {
    
    var isRemoving: Boolean by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Column {

            InternetArchiveHeader()

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(id = R.string.prompt_email),
                style = MaterialTheme.typography.caption
            )
            Text(
                text = state.email,
                fontSize = 18.sp
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "Username",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = state.username,
                fontSize = 18.sp
            )

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "Expires",
                style = MaterialTheme.typography.caption
            )

            Text(
                text = state.expires,
                fontSize = 18.sp
            )

            Button(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    isRemoving = true
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text(stringResource(id = R.string.menu_delete))
            }
        }
    }
    
    if (isRemoving) {
        RemoveInternetArchiveDialog(onDismiss = { isRemoving = false }) {
            dispatch(Action.Remove)
        }
    }
}

@Composable
private fun RemoveInternetArchiveDialog(onDismiss: () -> Unit, onRemove: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.remove_from_app))},
        text = { Text(stringResource(id = R.string.are_you_sure_you_want_to_remove_this_server_from_the_app))},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }, confirmButton = {
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text(stringResource(id = R.string.remove))
            }
    })
}

@Composable
@Preview(showBackground = true)
private fun InternetArchiveScreenPreview() {
    InternetArchiveContent(
        state = InternetArchiveState(
            email = "abc@example.com",
            username = "@abc_name",
            expires = Date().toString()
        )
    ) {}
}

@Composable
@Preview(showBackground = true)
private fun RemoveInternetArchiveDialogPreview() {
    RemoveInternetArchiveDialog(onDismiss = { }) {}
}
