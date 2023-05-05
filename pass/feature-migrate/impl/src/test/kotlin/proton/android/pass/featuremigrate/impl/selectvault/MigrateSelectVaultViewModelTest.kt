package proton.android.pass.featuremigrate.impl.selectvault

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import proton.android.pass.data.fakes.usecases.TestObserveVaultsWithItemCount
import proton.android.pass.featuremigrate.impl.MigrateModeArg
import proton.android.pass.featuremigrate.impl.MigrateModeValue
import proton.android.pass.navigation.api.CommonNavArgId
import proton.android.pass.navigation.api.CommonOptionalNavArgId
import proton.android.pass.test.MainDispatcherRule
import proton.android.pass.test.TestSavedStateHandle
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId
import proton.pass.domain.Vault
import proton.pass.domain.VaultWithItemCount

class MigrateSelectVaultViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    private lateinit var instance: MigrateSelectVaultViewModel
    private lateinit var observeVaults: TestObserveVaultsWithItemCount

    @Before
    fun setup() {
        observeVaults = TestObserveVaultsWithItemCount()
        instance = MigrateSelectVaultViewModel(
            observeVaults = observeVaults,
            savedStateHandle = TestSavedStateHandle.create().apply {
                set(CommonNavArgId.ShareId.key, SHARE_ID.id)
                set(MigrateModeArg.key, MigrateModeValue.SingleItem.name)
                set(CommonOptionalNavArgId.ItemId.key, ITEM_ID.id)
            }
        )
    }

    @Test
    fun `marks the current vault as not enabled`() = runTest {
        val (currentVault, otherVault) = initialVaults()
        observeVaults.sendResult(Result.success(listOf(currentVault, otherVault)))

        val expected = listOf(
            VaultEnabledPair(
                currentVault,
                false
            ),
            VaultEnabledPair(otherVault, true)
        )
        instance.state.test {
            val item = awaitItem()
            assertThat(item.vaultList).isEqualTo(expected)
            assertThat(item.event.value()).isNull()
        }
    }

    private fun initialVaults(): Pair<VaultWithItemCount, VaultWithItemCount> =
        Pair(
            VaultWithItemCount(
                vault = Vault(
                    shareId = SHARE_ID,
                    name = "vault1",
                    isPrimary = false
                ),
                activeItemCount = 1,
                trashedItemCount = 0
            ),
            VaultWithItemCount(
                vault = Vault(
                    shareId = ShareId("OTHER_SHARE_ID"),
                    name = "vault2",
                    isPrimary = false
                ),
                activeItemCount = 1,
                trashedItemCount = 0
            )
        )

    companion object {
        private val SHARE_ID = ShareId("123")
        private val ITEM_ID = ItemId("456")
    }

}