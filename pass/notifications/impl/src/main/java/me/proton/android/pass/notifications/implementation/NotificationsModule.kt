package me.proton.android.pass.notifications.implementation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.pass.notifications.api.SnackbarMessageRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    abstract fun bindSnackbarMessageRepository(
        snackbarMessageRepositoryImpl: SnackbarMessageRepositoryImpl
    ): SnackbarMessageRepository
}