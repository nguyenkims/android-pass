/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Pass.
 *
 * Proton Pass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Pass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Pass.  If not, see <https://www.gnu.org/licenses/>.
 */

package proton.android.pass.preferences

import kotlinx.coroutines.flow.Flow
import proton.android.pass.common.api.Option

interface InternalSettingsRepository {

    suspend fun setLastUnlockedTime(time: Long): Result<Unit>
    fun getLastUnlockedTime(): Flow<Option<Long>>

    suspend fun setBootCount(count: Long): Result<Unit>
    fun getBootCount(): Flow<Option<Long>>

    suspend fun setDeclinedUpdateVersion(versionDeclined: String): Result<Unit>
    fun getDeclinedUpdateVersion(): Flow<String>

    suspend fun setHomeSortingOption(sortingOption: SortingOptionPreference): Result<Unit>
    fun getHomeSortingOption(): Flow<SortingOptionPreference>

    suspend fun setAutofillSortingOption(sortingOption: SortingOptionPreference): Result<Unit>
    fun getAutofillSortingOption(): Flow<SortingOptionPreference>

    suspend fun setSelectedVault(selectedVault: SelectedVaultPreference): Result<Unit>
    fun getSelectedVault(): Flow<SelectedVaultPreference>

    suspend fun setPinAttemptsCount(count: Int): Result<Unit>
    fun getPinAttemptsCount(): Flow<Int>

    suspend fun setMasterPasswordAttemptsCount(count: Int): Result<Unit>
    fun getMasterPasswordAttemptsCount(): Flow<Int>

    suspend fun clearSettings(): Result<Unit>
}
