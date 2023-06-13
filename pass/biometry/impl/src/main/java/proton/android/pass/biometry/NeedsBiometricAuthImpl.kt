package proton.android.pass.biometry

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import proton.android.pass.preferences.UserPreferencesRepository
import javax.inject.Inject

class NeedsBiometricAuthImpl @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val authTimeHolder: BiometryAuthTimeHolder,
    private val clock: Clock
) : NeedsBiometricAuth {

    override fun invoke(): Flow<Boolean> = combine(
        preferencesRepository.getBiometricLockState().distinctUntilChanged(),
        preferencesRepository.getHasAuthenticated().distinctUntilChanged(),
        preferencesRepository.getAppLockPreference().distinctUntilChanged(),
        authTimeHolder.getBiometryAuthTime().distinctUntilChanged()
    ) { biometricLock, hasAuthenticated, appLockPreference, biometryAuthTime ->
        NeedsAuthChecker.needsAuth(
            biometricLock = biometricLock,
            hasAuthenticated = hasAuthenticated,
            appLockPreference = appLockPreference,
            lastUnlockTime = biometryAuthTime,
            now = clock.now()
        )
    }
}