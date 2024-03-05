package net.opendasharchive.openarchive.features.internetarchive.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.core.state.Dispatch
import net.opendasharchive.openarchive.db.Space
import net.opendasharchive.openarchive.features.internetarchive.presentation.InternetArchiveViewModel.Action
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun InternetArchiveScreen(space: Space) {
    val viewModel: InternetArchiveViewModel = koinViewModel {
        parametersOf(space)
    }

    val state by viewModel.state.collectAsState()

    InternetArchiveContent(state, viewModel::dispatch)
}

@Composable
private fun InternetArchiveContent(state: InternetArchiveState, dispatch: Dispatch<Action>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                text = stringResource(id = R.string.prompt_email),
                style = MaterialTheme.typography.caption
            )
            Text(
                text = state.email,
            )
            Text(
                text = "Username",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = state.username
            )

            Text(
                text = "Expires",
                style = MaterialTheme.typography.caption
            )

            Text(
                text = state.expires
            )
        }
    }
}
