package proton.android.pass.presentation.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import me.proton.pass.presentation.R

@Stable
enum class SortingType(@StringRes val titleId: Int) {
    ByName(R.string.sort_by_name),
    ByItemType(R.string.sort_by_type)
}