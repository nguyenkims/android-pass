package me.proton.android.pass.featurecreateitem.impl.alias.mailboxes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.pass.featurecreateitem.impl.alias.SelectedAliasMailboxUiModel

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun SelectMailboxesDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    mailboxes: List<SelectedAliasMailboxUiModel>,
    onMailboxesChanged: (List<SelectedAliasMailboxUiModel>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SelectMailboxesDialogViewModel = hiltViewModel()
) {
    if (!show) return

    LaunchedEffect(Unit) {
        viewModel.setMailboxes(mailboxes)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SelectMailboxesDialogContent(
        modifier = modifier,
        state = uiState,
        onConfirm = {
            onMailboxesChanged(uiState.mailboxes)
        },
        onDismiss = onDismiss,
        onMailboxToggled = { viewModel.onMailboxChanged(it) }
    )
}