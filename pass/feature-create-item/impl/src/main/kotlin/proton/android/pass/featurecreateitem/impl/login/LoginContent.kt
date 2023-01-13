package proton.android.pass.featurecreateitem.impl.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import proton.android.pass.commonuimodels.api.ItemUiModel
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.common.api.toOption
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId
import proton.android.pass.featurecreateitem.impl.login.LoginSnackbarMessages.EmptyShareIdError
import proton.android.pass.featurecreateitem.impl.login.bottomsheet.LoginBottomSheet
import proton.android.pass.featurecreateitem.impl.login.bottomsheet.LoginBottomSheetContent
import proton.android.pass.composecomponents.impl.uievents.IsLoadingState
import proton.android.pass.featurecreateitem.impl.ItemSavedState

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun LoginContent(
    modifier: Modifier = Modifier,
    @StringRes topBarTitle: Int,
    @StringRes topBarActionName: Int,
    uiState: CreateUpdateLoginUiState,
    showCreateAliasButton: Boolean,
    canDelete: Boolean,
    onUpClick: () -> Unit,
    onSuccess: (ShareId, ItemId, ItemUiModel) -> Unit,
    onSubmit: (ShareId) -> Unit,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onWebsiteChange: OnWebsiteChange,
    onNoteChange: (String) -> Unit,
    onEmitSnackbarMessage: (LoginSnackbarMessages) -> Unit,
    onCreateAliasClick: (ShareId, Option<String>) -> Unit,
    onRemoveAliasClick: () -> Unit,
    onDeleteItemClick: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()

    val (regeneratePassword, setRegeneratePassword) = remember { mutableStateOf(true) }
    val (bottomSheetContent, setBottomSheetContent) = remember {
        mutableStateOf<LoginBottomSheetContent>(LoginBottomSheetContent.GeneratePassword)
    }
    val (showRemoveAliasDialog, setShowRemoveAliasDialog) = remember { mutableStateOf(false) }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            LoginBottomSheet(
                content = bottomSheetContent,
                regeneratePassword = regeneratePassword,
                setRegeneratePassword = setRegeneratePassword,
                onPasswordChange = { password -> onPasswordChange(password) },
                hideBottomSheet = { scope.launch { bottomSheetState.hide() } },
                onRemoveAliasClick = {
                    scope.launch {
                        setShowRemoveAliasDialog(true)
                        bottomSheetState.hide()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                LoginTopBar(
                    shareId = uiState.shareId.value(),
                    topBarTitle = topBarTitle,
                    topBarActionName = topBarActionName,
                    isLoadingState = uiState.isLoadingState,
                    onUpClick = onUpClick,
                    onSubmit = onSubmit,
                    onSnackbarMessage = onEmitSnackbarMessage
                )
            }
        ) { padding ->
            LoginItemForm(
                modifier = modifier.padding(padding),
                loginItem = uiState.loginItem,
                showCreateAliasButton = showCreateAliasButton,
                canUpdateUsername = uiState.canUpdateUsername,
                canDelete = canDelete,
                onTitleChange = onTitleChange,
                onTitleRequiredError = uiState.validationErrors.contains(LoginItemValidationErrors.BlankTitle),
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onWebsiteChange = onWebsiteChange,
                focusLastWebsite = uiState.focusLastWebsite,
                isEditAllowed = uiState.isLoadingState == IsLoadingState.NotLoading,
                doesWebsiteIndexHaveError = { idx ->
                    uiState.validationErrors.any {
                        if (it is LoginItemValidationErrors.InvalidUrl) {
                            it.index == idx
                        } else {
                            false
                        }
                    }
                },
                onNoteChange = onNoteChange,
                onGeneratePasswordClick = {
                    scope.launch {
                        setBottomSheetContent(LoginBottomSheetContent.GeneratePassword)
                        setRegeneratePassword(true)
                        bottomSheetState.show()
                    }
                },
                onCreateAliasClick = {
                    if (uiState.shareId is Some) {
                        onCreateAliasClick(
                            uiState.shareId.value,
                            uiState.loginItem.title.toOption()
                        )
                    }
                },
                onAliasOptionsClick = {
                    scope.launch {
                        setBottomSheetContent(LoginBottomSheetContent.AliasOptions)
                        bottomSheetState.show()
                    }
                },
                onDeleteClick = onDeleteItemClick
            )

            if (showRemoveAliasDialog) {
                ConfirmRemoveAliasDialog(
                    onDismiss = { setShowRemoveAliasDialog(false) },
                    onCancel = { setShowRemoveAliasDialog(false) },
                    onConfirm = {
                        setShowRemoveAliasDialog(false)
                        onRemoveAliasClick()
                    }
                )
            }

            LaunchedEffect(uiState.isItemSaved is ItemSavedState.Success) {
                val isItemSaved = uiState.isItemSaved
                if (isItemSaved is ItemSavedState.Success) {
                    when (uiState.shareId) {
                        None -> onEmitSnackbarMessage(EmptyShareIdError)
                        is Some -> onSuccess(
                            uiState.shareId.value,
                            isItemSaved.itemId,
                            isItemSaved.item
                        )
                    }
                }
            }
        }
    }
}