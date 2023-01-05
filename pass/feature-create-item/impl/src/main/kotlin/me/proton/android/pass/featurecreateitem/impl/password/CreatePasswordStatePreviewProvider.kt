package me.proton.android.pass.featurecreateitem.impl.password

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import me.proton.core.util.kotlin.times

class CreatePasswordStatePreviewProvider : PreviewParameterProvider<CreatePasswordUiState> {
    override val values: Sequence<CreatePasswordUiState>
        get() = sequenceOf(
            CreatePasswordUiState(
                password = "a1b!c_d3e#fg",
                length = 12,
                hasSpecialCharacters = true
            ),
            CreatePasswordUiState(
                password = "a1!2",
                length = 4,
                hasSpecialCharacters = false
            ),
            CreatePasswordUiState(
                password = "a".times(64),
                length = 64,
                hasSpecialCharacters = false
            )
        )
}