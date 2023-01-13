package proton.android.pass.data.fakes.usecases

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import proton.android.pass.data.api.usecases.ObserveActiveShare
import proton.android.pass.common.api.Result
import proton.pass.domain.ShareId
import javax.inject.Inject

class TestObserveActiveShare @Inject constructor() : ObserveActiveShare {

    private val activeShareFlow: MutableSharedFlow<Result<ShareId?>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun invoke(): Flow<Result<ShareId?>> = activeShareFlow

    fun sendShare(result: Result<ShareId?>) = activeShareFlow.tryEmit(result)
}