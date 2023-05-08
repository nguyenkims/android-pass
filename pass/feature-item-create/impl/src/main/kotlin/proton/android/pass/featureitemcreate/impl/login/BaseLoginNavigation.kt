package proton.android.pass.featureitemcreate.impl.login

import proton.android.pass.common.api.Option
import proton.android.pass.commonuimodels.api.ItemUiModel
import proton.pass.domain.ItemId
import proton.pass.domain.ShareId

sealed interface BaseLoginNavigation {
    data class LoginCreated(val itemUiModel: ItemUiModel) : BaseLoginNavigation
    data class LoginUpdated(val shareId: ShareId, val itemId: ItemId) : BaseLoginNavigation
    data class CreateAlias(val shareId: ShareId, val title: Option<String>) : BaseLoginNavigation
    object GeneratePassword : BaseLoginNavigation
    object Upgrade : BaseLoginNavigation
    object ScanTotp : BaseLoginNavigation
    object Close : BaseLoginNavigation
}