package proton.android.pass.autofill.ui.autosave

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import proton.android.pass.autofill.entities.SaveInformation
import proton.android.pass.autofill.entities.SaveItemType
import proton.android.pass.commonuimodels.api.PackageInfoUi
import proton.android.pass.featureauth.impl.AUTH_SCREEN_ROUTE
import proton.android.pass.featureauth.impl.AuthScreen
import proton.android.pass.featureitemcreate.impl.login.CreateLogin
import proton.android.pass.featureitemcreate.impl.login.InitialCreateLoginUiState
import proton.android.pass.featureitemcreate.impl.login.createLoginGraph
import proton.android.pass.featureitemcreate.impl.totp.CameraTotp
import proton.android.pass.featureitemcreate.impl.totp.PhotoPickerTotp
import proton.android.pass.featureitemcreate.impl.totp.TOTP_NAV_PARAMETER_KEY
import proton.android.pass.featureitemcreate.impl.totp.createTotpGraph
import proton.android.pass.navigation.api.AppNavigator

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.autosaveActivityGraph(
    appNavigator: AppNavigator,
    info: SaveInformation,
    onAutoSaveCancel: () -> Unit,
    onAutoSaveSuccess: () -> Unit
) {
    composable(AUTH_SCREEN_ROUTE) {
        AuthScreen(
            onAuthSuccessful = { appNavigator.navigate(CreateLogin) },
            onAuthFailed = { onAutoSaveCancel() },
            onAuthDismissed = { onAutoSaveCancel() }
        )
    }
    val (username, password) = when (info.itemType) {
        is SaveItemType.Login -> Pair(info.itemType.identity, info.itemType.password)
        is SaveItemType.SingleValue -> Pair(info.itemType.contents, info.itemType.contents)
    }
    createLoginGraph(
        initialCreateLoginUiState = InitialCreateLoginUiState(
            title = info.appName,
            username = username,
            password = password,
            packageInfoUi = PackageInfoUi(info.packageName, info.appName)
        ),
        showCreateAliasButton = false,
        getPrimaryTotp = { appNavigator.navState<String>(TOTP_NAV_PARAMETER_KEY, null) },
        onSuccess = { onAutoSaveSuccess() },
        onClose = onAutoSaveCancel,
        onScanTotp = { appNavigator.navigate(CameraTotp) },
        onCreateAlias = { _, _ -> }
    )
    createTotpGraph(
        onUriReceived = { totp -> appNavigator.navigateUpWithResult(TOTP_NAV_PARAMETER_KEY, totp) },
        onCloseTotp = { appNavigator.onBackClick() },
        onOpenImagePicker = {
            appNavigator.navigate(
                destination = PhotoPickerTotp,
                backDestination = CreateLogin
            )
        }
    )
}