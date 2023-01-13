package proton.android.pass.data.impl.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import proton.android.pass.data.impl.local.LocalEventDataSource
import proton.android.pass.data.impl.remote.RemoteEventDataSource
import proton.android.pass.data.impl.responses.EventList
import proton.android.pass.log.api.PassLogger
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import proton.pass.domain.ShareId
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val localEventDataSource: LocalEventDataSource,
    private val remoteEventDataSource: RemoteEventDataSource
) : EventRepository {
    private suspend fun getLatestEventId(
        userId: UserId,
        addressId: AddressId,
        shareId: ShareId
    ): String {
        val local = localEventDataSource.getLatestEventId(userId, addressId, shareId).first()
        if (local != null) {
            PassLogger.d(TAG, "Returning local latestEventId [share=${shareId.id}]")
            return local
        }
        PassLogger.d(TAG, "Retrieving remote latestEventId [share=${shareId.id}]")
        return remoteEventDataSource.getLatestEventId(userId, shareId).first()
    }

    override suspend fun getEvents(
        userId: UserId,
        addressId: AddressId,
        shareId: ShareId
    ): EventList =
        withContext(Dispatchers.IO) {
            val latestEventId = getLatestEventId(userId, addressId, shareId)
            PassLogger.d(TAG, "Fetching events [share=${shareId.id}][eventId=$latestEventId]")

            val events = remoteEventDataSource.getEvents(userId, shareId, latestEventId).first()
            localEventDataSource.storeLatestEventId(
                userId,
                addressId,
                shareId,
                events.latestEventId
            )
            return@withContext events
        }

    override suspend fun storeLatestEventId(
        userId: UserId,
        addressId: AddressId,
        shareId: ShareId,
        eventId: String
    ) {
        withContext(Dispatchers.IO) {
            localEventDataSource.storeLatestEventId(userId, addressId, shareId, eventId)
        }
    }

    companion object {
        private const val TAG = "EventRepositoryImpl"
    }
}