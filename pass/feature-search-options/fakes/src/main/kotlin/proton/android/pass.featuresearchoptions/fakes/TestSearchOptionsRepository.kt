package proton.android.pass.featuresearchoptions.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import proton.android.pass.featuresearchoptions.api.SearchOptions
import proton.android.pass.featuresearchoptions.api.SearchOptionsRepository
import proton.android.pass.featuresearchoptions.api.SortingOption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestSearchOptionsRepository @Inject constructor() : SearchOptionsRepository {

    private val searchOptionsFlow = MutableStateFlow(SearchOptions.Initial)

    private val sortingOptionFlow = MutableStateFlow(SearchOptions.Initial.sortingOption)

    override fun observeSearchOptions(): Flow<SearchOptions> = searchOptionsFlow

    override fun observeSortingOption(): Flow<SortingOption> = sortingOptionFlow

    override fun setSortingOption(sortingOption: SortingOption) {
        sortingOptionFlow.update { sortingOption }
    }

    override fun clearSearchOptions() {
    }
}