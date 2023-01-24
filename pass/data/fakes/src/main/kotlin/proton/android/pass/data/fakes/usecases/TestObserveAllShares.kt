package proton.android.pass.data.fakes.usecases

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.proton.core.domain.entity.UserId
import proton.android.pass.common.api.Result
import proton.android.pass.data.api.usecases.ObserveAllShares
import proton.pass.domain.Share
import javax.inject.Inject

class TestObserveAllShares @Inject constructor() : ObserveAllShares {

    private val observeAllSharesFlow: MutableSharedFlow<Result<List<Share>>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun invoke(userId: UserId?): Flow<Result<List<Share>>> = observeAllSharesFlow

    fun sendResult(result: Result<List<Share>>) = observeAllSharesFlow.tryEmit(result)
}