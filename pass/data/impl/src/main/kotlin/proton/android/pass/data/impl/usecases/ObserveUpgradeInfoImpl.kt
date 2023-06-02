package proton.android.pass.data.impl.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import me.proton.core.payment.domain.PaymentManager
import proton.android.pass.data.api.usecases.ObserveCurrentUser
import proton.android.pass.data.api.usecases.ObserveItemCount
import proton.android.pass.data.api.usecases.ObserveMFACount
import proton.android.pass.data.api.usecases.ObserveUpgradeInfo
import proton.android.pass.data.api.usecases.ObserveVaultCount
import proton.android.pass.data.api.usecases.UpgradeInfo
import proton.android.pass.data.impl.repositories.PlanRepository
import proton.pass.domain.PlanType
import javax.inject.Inject

class ObserveUpgradeInfoImpl @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUser,
    private val observeMFACount: ObserveMFACount,
    private val observeItemCount: ObserveItemCount,
    private val paymentManager: PaymentManager,
    private val planRepository: PlanRepository,
    private val observeVaultCount: ObserveVaultCount
) : ObserveUpgradeInfo {
    override fun invoke(forceRefresh: Boolean): Flow<UpgradeInfo> = observeCurrentUser()
        .distinctUntilChanged()
        .flatMapLatest { user ->
            combine(
                planRepository.sendUserAccessAndObservePlan(
                    userId = user.userId,
                    forceRefresh = forceRefresh
                ),
                flowOf(paymentManager.isUpgradeAvailable()),
                observeMFACount(),
                observeItemCount(itemState = null),
                observeVaultCount(user.userId)
            ) { plan, isUpgradeAvailable, mfaCount, itemCount, vaultCount ->
                val isPaid = plan.planType is PlanType.Paid
                val displayUpgrade = when {
                    plan.hideUpgrade -> false
                    else -> isUpgradeAvailable && !isPaid
                }
                UpgradeInfo(
                    isUpgradeAvailable = displayUpgrade,
                    plan = plan.copy(
                        vaultLimit = plan.vaultLimit,
                        aliasLimit = plan.aliasLimit,
                        totpLimit = plan.totpLimit,
                    ),
                    totalVaults = vaultCount,
                    totalAlias = itemCount.alias.toInt(),
                    totalTotp = mfaCount
                )
            }
        }
        .distinctUntilChanged()
}
