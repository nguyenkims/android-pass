/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.autofill.ui.bottomsheet.itemoptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import proton.android.pass.composecomponents.impl.uievents.IsLoadingState
import proton.android.pass.data.api.usecases.TrashItem
import proton.android.pass.log.api.PassLogger
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.notifications.api.SnackbarDispatcher
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId
import javax.inject.Inject

@HiltViewModel
class AutofillItemOptionsViewModel @Inject constructor(
    private val trashItem: TrashItem,
    private val savedStateHandle: SavedStateHandle,
    private val snackbarDispatcher: SnackbarDispatcher
) : ViewModel() {

    private val shareId = ShareId(getNavArg(CommonNavArgId.ShareId.key))
    private val itemId = ItemId(getNavArg(CommonNavArgId.ItemId.key))

    private val eventFlow: MutableStateFlow<AutofillItemOptionsEvent> =
        MutableStateFlow(AutofillItemOptionsEvent.Unknown)
    private val loadingFlow: MutableStateFlow<IsLoadingState> =
        MutableStateFlow(IsLoadingState.NotLoading)

    val state: StateFlow<AutofillItemOptionsUiState> = combine(
        eventFlow,
        loadingFlow
    ) { event, loading ->
        AutofillItemOptionsUiState(
            isLoading = loading,
            event = event
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AutofillItemOptionsUiState.Initial
    )

    fun onTrash() = viewModelScope.launch {
        loadingFlow.update { IsLoadingState.Loading }
        runCatching { trashItem(shareId = shareId, itemId = itemId) }
            .onSuccess {
                eventFlow.update { AutofillItemOptionsEvent.Close }
                snackbarDispatcher(AutofillItemOptionsSnackbarMessage.SentToTrashSuccess)
            }
            .onFailure {
                PassLogger.w(TAG, it, "Error sending item to trash")
                snackbarDispatcher(AutofillItemOptionsSnackbarMessage.SentToTrashError)
            }
        loadingFlow.update { IsLoadingState.NotLoading }
    }

    private fun getNavArg(name: String): String =
        savedStateHandle.get<String>(name)
            ?: throw IllegalStateException("Missing $name nav argument")

    companion object {
        private const val TAG = "AutofillItemOptionsViewModel"
    }

}


