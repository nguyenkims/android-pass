package me.proton.pass.presentation.components.navigation.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun InternalDrawerItem(
    modifier: Modifier = Modifier,
    closeDrawerAction: () -> Unit,
    onClick: () -> Unit
) {
    NavigationDrawerListItem(
        title = "(dev) Internal developer options",
        icon = me.proton.core.presentation.R.drawable.ic_proton_cog_wheel,
        isSelected = false,
        closeDrawerAction = closeDrawerAction,
        modifier = modifier,
        onClick = onClick
    )
}