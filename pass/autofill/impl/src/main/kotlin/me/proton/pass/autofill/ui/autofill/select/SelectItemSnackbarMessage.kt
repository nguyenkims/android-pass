package me.proton.pass.autofill.ui.autofill.select

import androidx.annotation.StringRes
import me.proton.android.pass.notifications.api.SnackbarMessage
import me.proton.android.pass.notifications.api.SnackbarType
import me.proton.pass.autofill.service.R

enum class SelectItemSnackbarMessage(
    @StringRes override val id: Int,
    override val type: SnackbarType,
    override val isClipboard: Boolean = false
) : SnackbarMessage {
    LoadItemsError(R.string.error_loading_items, SnackbarType.ERROR),
}
