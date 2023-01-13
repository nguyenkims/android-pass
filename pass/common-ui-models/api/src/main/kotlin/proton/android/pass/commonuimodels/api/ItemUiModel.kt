package proton.android.pass.commonuimodels.api

import proton.pass.domain.ItemId
import proton.pass.domain.ItemType
import proton.pass.domain.ShareId

data class ItemUiModel(
    val id: ItemId,
    val shareId: ShareId,
    val name: String,
    val note: String,
    val itemType: ItemType
)