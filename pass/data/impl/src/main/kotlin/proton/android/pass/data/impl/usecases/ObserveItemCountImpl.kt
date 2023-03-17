package proton.android.pass.data.impl.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import proton.android.pass.common.api.LoadingResult
import proton.android.pass.data.api.ItemCountSummary
import proton.android.pass.data.api.repositories.ItemRepository
import proton.android.pass.data.api.usecases.ObserveAllShares
import proton.android.pass.data.api.usecases.ObserveCurrentUser
import proton.android.pass.data.api.usecases.ObserveItemCount
import javax.inject.Inject

class ObserveItemCountImpl @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUser,
    private val observeAllShares: ObserveAllShares,
    private val itemRepository: ItemRepository
) : ObserveItemCount {

    override fun invoke(): Flow<LoadingResult<ItemCountSummary>> = observeAllShares()
        .flatMapLatest { result ->
            flow {
                when (result) {
                    is LoadingResult.Error -> emit(LoadingResult.Error(result.exception))
                    LoadingResult.Loading -> emit(LoadingResult.Loading)
                    is LoadingResult.Success -> {
                        observeCurrentUser()
                            .flatMapLatest { user ->
                                itemRepository.observeItemCountSummary(
                                    user.userId,
                                    result.data.map { it.id }
                                )
                            }
                            .collect { emit(LoadingResult.Success(it)) }
                    }
                }
            }
        }
}