package proton.android.pass.featurecreateitem.impl.alias

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import proton.android.pass.composecomponents.impl.form.ProtonFormInput
import proton.android.pass.featurecreateitem.impl.R
import me.proton.core.compose.theme.ProtonTheme
import proton.android.pass.commonui.api.ThemePairPreviewProvider

@Composable
internal fun DisplayAliasSection(
    modifier: Modifier = Modifier,
    state: AliasItem
) {
    ProtonFormInput(
        title = stringResource(id = R.string.field_alias_title),
        value = state.aliasToBeCreated ?: "",
        onChange = {},
        editable = false,
        modifier = modifier.padding(top = 8.dp)
    )
}

class ThemedDisplayAliasPreviewProvider :
    ThemePairPreviewProvider<AliasItemParameter>(AliasItemPreviewProvider())

@Preview
@Composable
fun DisplayAliasSectionPreview(
    @PreviewParameter(ThemedDisplayAliasPreviewProvider::class) input: Pair<Boolean, AliasItemParameter>
) {
    ProtonTheme(isDark = input.first) {
        Surface {
            DisplayAliasSection(state = input.second.item)
        }
    }
}