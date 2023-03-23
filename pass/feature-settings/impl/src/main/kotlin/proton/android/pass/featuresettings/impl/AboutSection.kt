package proton.android.pass.featuresettings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak
import proton.android.pass.commonui.api.PassTheme
import proton.android.pass.commonui.api.ThemePreviewProvider
import proton.android.pass.composecomponents.impl.container.roundedContainer
import proton.android.pass.composecomponents.impl.setting.SettingOption

@Composable
fun AboutSection(
    modifier: Modifier = Modifier,
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.settings_about_section_title),
            style = ProtonTheme.typography.defaultSmallWeak,
        )
        Column(
            modifier = Modifier.roundedContainer(ProtonTheme.colors.separatorNorm)
        ) {
            SettingOption(
                text = stringResource(R.string.settings_option_privacy_policy),
                onClick = onPrivacyClick
            )
            Divider()
            SettingOption(
                text = stringResource(R.string.settings_option_terms_of_service),
                onClick = onTermsClick
            )
        }
    }
}

@Preview
@Composable
fun AboutSectionPreview(
    @PreviewParameter(ThemePreviewProvider::class) isDark: Boolean
) {
    PassTheme(isDark = isDark) {
        Surface {
            AboutSection(onPrivacyClick = {}, onTermsClick = {})
        }
    }
}