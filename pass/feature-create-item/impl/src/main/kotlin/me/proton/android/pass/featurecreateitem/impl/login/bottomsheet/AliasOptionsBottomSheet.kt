package me.proton.android.pass.featurecreateitem.impl.login.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.collections.immutable.persistentListOf
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItem
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItemIcon
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItemList
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetItemTitle
import me.proton.android.pass.composecomponents.impl.bottomsheet.BottomSheetTitle
import me.proton.android.pass.featurecreateitem.impl.R
import me.proton.core.compose.theme.ProtonTheme
import me.proton.pass.commonui.api.ThemePreviewProvider

@Composable
fun AliasOptionsBottomSheet(
    modifier: Modifier = Modifier,
    onRemoveAliasClick: () -> Unit
) {
    Column(modifier = modifier) {
        BottomSheetTitle(
            title = stringResource(id = R.string.alias_bottomsheet_alias_title),
            showDivider = false
        )
        BottomSheetItemList(
            items = persistentListOf(
                createRemoveAlias(onRemoveAliasClick)
            )
        )
    }
}

private fun createRemoveAlias(onRemoveAlias: () -> Unit): BottomSheetItem =
    object : BottomSheetItem {
        override val title: @Composable () -> Unit
            get() = {
                BottomSheetItemTitle(
                    text = stringResource(id = R.string.action_remove),
                    textcolor = ProtonTheme.colors.notificationError
                )
            }
        override val subtitle: (@Composable () -> Unit)?
            get() = null
        override val icon: (@Composable () -> Unit)
            get() = {
                BottomSheetItemIcon(
                    iconId = me.proton.core.presentation.R.drawable.ic_proton_trash,
                    tint = ProtonTheme.colors.notificationError
                )
            }
        override val onClick: () -> Unit
            get() = onRemoveAlias
    }

@Preview
@Composable
fun AliasOptionsBottomSheetPreview(
    @PreviewParameter(ThemePreviewProvider::class) isDark: Boolean
) {
    ProtonTheme(isDark = isDark) {
        Surface {
            AliasOptionsBottomSheet(
                onRemoveAliasClick = {}
            )
        }
    }
}