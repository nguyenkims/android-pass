package proton.android.pass.data.fakes.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import proton.android.pass.data.api.usecases.ObserveUpgradeInfo
import proton.android.pass.data.api.usecases.UpgradeInfo
import proton.pass.domain.Plan
import proton.pass.domain.PlanType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestObserveUpgradeInfo @Inject constructor() : ObserveUpgradeInfo {

    private var upgradeInfo: MutableSharedFlow<UpgradeInfo> = MutableStateFlow(DEFAULT)

    fun setResult(value: UpgradeInfo) {
        upgradeInfo.tryEmit(value)
    }

    override fun invoke(forceRefresh: Boolean): Flow<UpgradeInfo> = upgradeInfo

    companion object {
        val DEFAULT = UpgradeInfo(
            isUpgradeAvailable = false,
            plan = Plan(
                planType = PlanType.Free,
                vaultLimit = 0,
                aliasLimit = 0,
                totpLimit = 0,
                updatedAt = 0,
                hideUpgrade = false,
            ),
            totalVaults = 0,
            totalAlias = 0,
            totalTotp = 0
        )
    }
}
