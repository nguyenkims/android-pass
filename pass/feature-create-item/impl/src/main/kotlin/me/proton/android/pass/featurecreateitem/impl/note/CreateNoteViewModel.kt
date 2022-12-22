package me.proton.android.pass.featurecreateitem.impl.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.pass.composecomponents.impl.uievents.IsLoadingState
import me.proton.android.pass.crypto.api.context.EncryptionContextProvider
import me.proton.android.pass.data.api.repositories.ItemRepository
import me.proton.android.pass.data.api.usecases.GetShareById
import me.proton.android.pass.featurecreateitem.impl.ItemSavedState
import me.proton.android.pass.featurecreateitem.impl.note.NoteSnackbarMessage.ItemCreationError
import me.proton.android.pass.log.api.PassLogger
import me.proton.android.pass.notifications.api.SnackbarMessageRepository
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.pass.common.api.onError
import me.proton.pass.common.api.onSuccess
import me.proton.pass.commonui.api.toUiModel
import me.proton.pass.domain.ShareId
import javax.inject.Inject

@HiltViewModel
class CreateNoteViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getShare: GetShareById,
    private val itemRepository: ItemRepository,
    private val snackbarMessageRepository: SnackbarMessageRepository,
    private val encryptionContextProvider: EncryptionContextProvider,
    savedStateHandle: SavedStateHandle
) : BaseNoteViewModel(snackbarMessageRepository, savedStateHandle) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        PassLogger.e(TAG, throwable)
    }

    fun createNote(shareId: ShareId) = viewModelScope.launch(coroutineExceptionHandler) {
        val noteItem = noteItemState.value
        val noteItemValidationErrors = noteItem.validate()
        if (noteItemValidationErrors.isNotEmpty()) {
            noteItemValidationErrorsState.update { noteItemValidationErrors }
        } else {
            isLoadingState.update { IsLoadingState.Loading }
            val userId = accountManager.getPrimaryUserId()
                .first { userId -> userId != null }
            if (userId != null) {
                getShare(userId, shareId)
                    .onSuccess { share ->
                        requireNotNull(share)
                        val itemContents = noteItem.toItemContents()
                        itemRepository.createItem(userId, share, itemContents)
                            .onSuccess { item ->
                                isItemSavedState.update {
                                    encryptionContextProvider.withEncryptionContext {
                                        ItemSavedState.Success(
                                            item.id,
                                            item.toUiModel(this@withEncryptionContext)
                                        )
                                    }
                                }
                            }
                            .onError {
                                val defaultMessage = "Create item error"
                                PassLogger.i(
                                    TAG,
                                    it ?: Exception(defaultMessage),
                                    defaultMessage
                                )
                                snackbarMessageRepository.emitSnackbarMessage(ItemCreationError)
                            }
                    }
                    .onError {
                        val defaultMessage = "Get share error"
                        PassLogger.i(TAG, it ?: Exception(defaultMessage), defaultMessage)
                        snackbarMessageRepository.emitSnackbarMessage(ItemCreationError)
                    }
            } else {
                PassLogger.i(TAG, "Empty User Id")
                snackbarMessageRepository.emitSnackbarMessage(ItemCreationError)
            }
            isLoadingState.update { IsLoadingState.NotLoading }
        }
    }

    companion object {
        private const val TAG = "CreateNoteViewModel"
    }
}
