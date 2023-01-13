package proton.android.pass.presentation.home

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import proton.android.pass.composecomponents.impl.topbar.SearchTopBar
import me.proton.core.compose.theme.ProtonTheme
import proton.android.pass.commonui.api.ThemePreviewProvider
import me.proton.pass.presentation.R

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
internal fun HomeTopBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    inSearchMode: Boolean,
    homeFilter: HomeFilterMode,
    onSearchQueryChange: (String) -> Unit,
    onEnterSearch: () -> Unit,
    onStopSearching: () -> Unit,
    onDrawerIconClick: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    if (inSearchMode) {
        SearchTopBar(
            modifier = modifier,
            placeholder = stringResource(id = R.string.placeholder_item_search),
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onStopSearch = {
                onStopSearching()
            }
        )
    } else {
        IdleHomeTopBar(
            modifier = modifier,
            homeFilter = homeFilter,
            startSearchMode = { onEnterSearch() },
            onDrawerIconClick = onDrawerIconClick,
            onMoreOptionsClick = onMoreOptionsClick
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Preview
@Composable
fun HomeTopBarIdlePreview(
    @PreviewParameter(ThemePreviewProvider::class) isDarkMode: Boolean
) {
    ProtonTheme(isDark = isDarkMode) {
        Surface {
            HomeTopBar(
                searchQuery = "",
                homeFilter = HomeFilterMode.AllItems,
                inSearchMode = false,
                onSearchQueryChange = {},
                onEnterSearch = {},
                onStopSearching = {},
                onDrawerIconClick = {},
                onMoreOptionsClick = {}
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Preview
@Composable
fun HomeTopBarSearchPreview(
    @PreviewParameter(ThemePreviewProvider::class) isDarkMode: Boolean
) {
    ProtonTheme(isDark = isDarkMode) {
        Surface {
            HomeTopBar(
                searchQuery = "some search",
                homeFilter = HomeFilterMode.AllItems,
                inSearchMode = true,
                onSearchQueryChange = {},
                onEnterSearch = {},
                onStopSearching = {},
                onDrawerIconClick = {},
                onMoreOptionsClick = {}
            )
        }
    }
}