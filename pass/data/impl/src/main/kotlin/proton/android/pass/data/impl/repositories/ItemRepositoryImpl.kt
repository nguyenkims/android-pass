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

package proton.android.pass.data.impl.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.extension.primary
import me.proton.core.user.domain.repository.UserAddressRepository
import proton.android.pass.common.api.None
import proton.android.pass.common.api.Option
import proton.android.pass.common.api.Some
import proton.android.pass.common.api.transpose
import proton.android.pass.crypto.api.context.EncryptionContext
import proton.android.pass.crypto.api.context.EncryptionContextProvider
import proton.android.pass.crypto.api.error.CryptoException
import proton.android.pass.crypto.api.usecases.CreateItem
import proton.android.pass.crypto.api.usecases.MigrateItem
import proton.android.pass.crypto.api.usecases.OpenItem
import proton.android.pass.crypto.api.usecases.UpdateItem
import proton.android.pass.data.api.ItemCountSummary
import proton.android.pass.data.api.PendingEventList
import proton.android.pass.data.api.errors.CannotRemoveNotTrashedItemError
import proton.android.pass.data.api.repositories.ItemRepository
import proton.android.pass.data.api.repositories.ShareItemCount
import proton.android.pass.data.api.repositories.ShareRepository
import proton.android.pass.data.api.usecases.ItemTypeFilter
import proton.android.pass.data.impl.db.PassDatabase
import proton.android.pass.data.impl.db.entities.ItemEntity
import proton.android.pass.data.impl.extensions.hasPackageName
import proton.android.pass.data.impl.extensions.hasTotp
import proton.android.pass.data.impl.extensions.hasWebsite
import proton.android.pass.data.impl.extensions.toCrypto
import proton.android.pass.data.impl.extensions.toDomain
import proton.android.pass.data.impl.extensions.toItemRevision
import proton.android.pass.data.impl.extensions.toRequest
import proton.android.pass.data.impl.extensions.with
import proton.android.pass.data.impl.extensions.withUrl
import proton.android.pass.data.impl.local.LocalItemDataSource
import proton.android.pass.data.impl.remote.RemoteItemDataSource
import proton.android.pass.data.impl.requests.CreateAliasRequest
import proton.android.pass.data.impl.requests.CreateItemAliasRequest
import proton.android.pass.data.impl.requests.MigrateItemRequest
import proton.android.pass.data.impl.requests.MigrateItemsBody
import proton.android.pass.data.impl.requests.MigrateItemsRequest
import proton.android.pass.data.impl.requests.TrashItemRevision
import proton.android.pass.data.impl.requests.TrashItemsRequest
import proton.android.pass.data.impl.responses.ItemRevision
import proton.android.pass.data.impl.util.TimeUtil
import proton.android.pass.datamodels.api.serializeToProto
import proton.android.pass.log.api.PassLogger
import proton.pass.domain.Item
import proton.pass.domain.ItemContents
import proton.pass.domain.ItemId
import proton.pass.domain.ItemState
import proton.pass.domain.ItemType
import proton.pass.domain.Share
import proton.pass.domain.ShareId
import proton.pass.domain.ShareSelection
import proton.pass.domain.entity.NewAlias
import proton.pass.domain.entity.PackageInfo
import proton.pass.domain.key.ShareKey
import proton_pass_item_v1.ItemV1
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class ItemRepositoryImpl @Inject constructor(
    private val database: PassDatabase,
    private val accountManager: AccountManager,
    override val userAddressRepository: UserAddressRepository,
    private val shareRepository: ShareRepository,
    private val createItem: CreateItem,
    private val updateItem: UpdateItem,
    private val localItemDataSource: LocalItemDataSource,
    private val remoteItemDataSource: RemoteItemDataSource,
    private val shareKeyRepository: ShareKeyRepository,
    private val openItem: OpenItem,
    private val migrateItem: MigrateItem,
    private val itemKeyRepository: ItemKeyRepository,
    private val encryptionContextProvider: EncryptionContextProvider
) : BaseRepository(userAddressRepository), ItemRepository {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createItem(
        userId: UserId,
        share: Share,
        contents: ItemContents
    ): Item = withContext(Dispatchers.IO) {
        withUserAddress(userId) { userAddress ->
            val shareKey = shareKeyRepository.getLatestKeyForShare(share.id).first()

            val body = try {
                createItem.create(shareKey, contents)
            } catch (e: RuntimeException) {
                PassLogger.w(TAG, e, "Error creating item")
                throw e
            }

            val itemResponse =
                remoteItemDataSource.createItem(userId, share.id, body.request.toRequest())
            val entity = itemResponseToEntity(
                userAddress,
                itemResponse,
                share,
                listOf(shareKey)
            )
            localItemDataSource.upsertItem(entity)

            encryptionContextProvider.withEncryptionContext {
                entity.toDomain(this@withEncryptionContext)
            }
        }
    }

    override suspend fun createAlias(
        userId: UserId,
        share: Share,
        newAlias: NewAlias
    ): Item = withContext(Dispatchers.IO) {
        withUserAddress(userId) { userAddress ->
            val shareKey = shareKeyRepository.getLatestKeyForShare(share.id).first()
            val itemContents = ItemContents.Alias(
                title = newAlias.title,
                note = newAlias.note,
                aliasEmail = "" // Not used when creating the payload
            )
            val body = createItem.create(shareKey, itemContents)

            val mailboxIds = newAlias.mailboxes.map { it.id }
            val requestBody = CreateAliasRequest(
                prefix = newAlias.prefix,
                signedSuffix = newAlias.suffix.signedSuffix,
                mailboxes = mailboxIds,
                item = body.request.toRequest()
            )

            val itemResponse = remoteItemDataSource.createAlias(userId, share.id, requestBody)
            val entity = itemResponseToEntity(
                userAddress,
                itemResponse,
                share,
                listOf(shareKey)
            )
            localItemDataSource.upsertItem(entity)
            encryptionContextProvider.withEncryptionContext {
                entity.toDomain(this@withEncryptionContext)
            }
        }
    }

    override suspend fun createItemAndAlias(
        userId: UserId,
        shareId: ShareId,
        contents: ItemContents,
        newAlias: NewAlias
    ): Item = withContext(Dispatchers.IO) {
        withUserAddress(userId) { userAddress ->
            val share = shareRepository.getById(userId, shareId)
            val shareKey = shareKeyRepository.getLatestKeyForShare(shareId).first()
            val request = runCatching {
                val itemBody = createItem.create(shareKey, contents)
                val aliasContents = ItemContents.Alias(
                    title = newAlias.title,
                    note = newAlias.note,
                    aliasEmail = "" // Not used when creating the payload
                )
                val aliasBody = createItem.create(shareKey, aliasContents)

                CreateItemAliasRequest(
                    alias = CreateAliasRequest(
                        prefix = newAlias.prefix,
                        signedSuffix = newAlias.suffix.signedSuffix,
                        mailboxes = newAlias.mailboxes.map { it.id },
                        item = aliasBody.request.toRequest()
                    ),
                    item = itemBody.request.toRequest()
                )
            }.fold(
                onSuccess = { it },
                onFailure = {
                    PassLogger.e(TAG, it, "Error creating item")
                    throw it
                }
            )

            val itemResponse = remoteItemDataSource.createItemAndAlias(userId, shareId, request)
            val itemEntity =
                itemResponseToEntity(userAddress, itemResponse.item, share, listOf(shareKey))
            val aliasEntity =
                itemResponseToEntity(userAddress, itemResponse.alias, share, listOf(shareKey))
            database.inTransaction {
                localItemDataSource.upsertItem(itemEntity)
                localItemDataSource.upsertItem(aliasEntity)
            }

            encryptionContextProvider.withEncryptionContext {
                itemEntity.toDomain(this@withEncryptionContext)
            }
        }
    }

    override suspend fun updateItem(
        userId: UserId,
        share: Share,
        item: Item,
        contents: ItemContents
    ): Item = withContext(Dispatchers.IO) {
        val itemContents = encryptionContextProvider.withEncryptionContext {
            contents.serializeToProto(itemUuid = item.itemUuid, this)
        }
        performUpdate(
            userId,
            share,
            item,
            itemContents
        )
    }

    override fun observeItems(
        userId: UserId,
        shareSelection: ShareSelection,
        itemState: ItemState?,
        itemTypeFilter: ItemTypeFilter
    ): Flow<List<Item>> =
        when (shareSelection) {
            is ShareSelection.Share -> localItemDataSource.observeItemsForShare(
                userId = userId,
                shareId = shareSelection.shareId,
                itemState = itemState,
                filter = itemTypeFilter
            )

            is ShareSelection.AllShares -> localItemDataSource.observeItems(
                userId = userId,
                itemState = itemState,
                filter = itemTypeFilter
            )
        }
            .map { items ->
                // Detect if we have received the update from a logout
                val isAccountStillAvailable = accountManager.getAccount(userId).first() != null
                if (!isAccountStillAvailable) return@map emptyList()
                encryptionContextProvider.withEncryptionContext {
                    items.map { it.toDomain(this@withEncryptionContext) }
                }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun getById(
        userId: UserId,
        shareId: ShareId,
        itemId: ItemId
    ): Item =
        withContext(Dispatchers.IO) {
            val item = localItemDataSource.getById(shareId, itemId)
            requireNotNull(item)
            encryptionContextProvider.withEncryptionContext {
                item.toDomain(this@withEncryptionContext)
            }
        }

    override suspend fun trashItem(
        userId: UserId,
        shareId: ShareId,
        itemId: ItemId
    ) {
        withContext(Dispatchers.IO) {
            val item = requireNotNull(localItemDataSource.getById(shareId, itemId))
            if (item.state == ItemState.Trashed.value) {
                throw CannotRemoveNotTrashedItemError()
            }

            val body = TrashItemsRequest(
                listOf(TrashItemRevision(itemId = item.id, revision = item.revision))
            )

            val response = remoteItemDataSource.sendToTrash(userId, shareId, body)
            return@withContext database.inTransaction {
                response.items.find { it.itemId == item.id }
                    ?.let {
                        val updatedItem = item.copy(
                            revision = it.revision,
                            state = ItemState.Trashed.value
                        )
                        localItemDataSource.upsertItem(updatedItem)
                    }
            }
        }
    }


    override suspend fun untrashItem(
        userId: UserId,
        shareId: ShareId,
        itemId: ItemId
    ) {
        withContext(Dispatchers.IO) {
            // Optimistically update the local database
            val originalItem: ItemEntity = database.inTransaction {
                val item = requireNotNull(localItemDataSource.getById(shareId, itemId))
                if (item.state == ItemState.Active.value) return@inTransaction null
                val updatedItem = item.copy(state = ItemState.Active.value)
                localItemDataSource.upsertItem(updatedItem)
                item
            } ?: return@withContext

            // Perform the network request
            val body = TrashItemsRequest(
                listOf(TrashItemRevision(originalItem.id, originalItem.revision))
            )

            runCatching { remoteItemDataSource.untrash(userId, shareId, body) }
                .onFailure {
                    PassLogger.w(TAG, "Error untrashing item. Restoring the original one")
                    localItemDataSource.upsertItem(originalItem)
                    throw it
                }
        }
    }

    override suspend fun clearTrash(userId: UserId) {
        withContext(Dispatchers.IO) {
            val trashedItems = localItemDataSource.getTrashedItems(userId)
            val trashedPerShare = trashedItems.groupBy { it.shareId }
            val results = trashedPerShare
                .map { entry ->
                    async {
                        clearItemsForShare(
                            shareId = ShareId(entry.key),
                            shareItems = entry.value,
                            userId = userId
                        )
                    }
                }
                .awaitAll()
                .transpose()

            results.onFailure {
                throw it
            }
        }
    }

    override suspend fun restoreItems(userId: UserId) {
        withContext(Dispatchers.IO) {
            val trashedItems = localItemDataSource.getTrashedItems(userId)
            val trashedPerShare = trashedItems.groupBy { it.shareId }
            val results = trashedPerShare
                .map { entry ->
                    async {
                        restoreItemsForShare(
                            userId = userId,
                            shareId = ShareId(entry.key),
                            shareItems = entry.value
                        )
                    }
                }
                .awaitAll()
                .transpose()

            results.onFailure {
                throw it
            }
        }
    }

    override suspend fun deleteItem(
        userId: UserId,
        shareId: ShareId,
        itemId: ItemId
    ) {
        withContext(Dispatchers.IO) {
            val item = requireNotNull(localItemDataSource.getById(shareId, itemId))
            if (item.state != ItemState.Trashed.value) return@withContext

            val body =
                TrashItemsRequest(
                    listOf(
                        TrashItemRevision(
                            itemId = item.id,
                            revision = item.revision
                        )
                    )
                )

            runCatching { remoteItemDataSource.delete(userId, shareId, body) }
                .onSuccess {
                    localItemDataSource.delete(shareId, itemId)
                }
                .onFailure {
                    PassLogger.w(TAG, it, "Error deleting item")
                    throw it
                }
        }
    }

    @Suppress("ReturnCount")
    override suspend fun addPackageAndUrlToItem(
        shareId: ShareId,
        itemId: ItemId,
        packageInfo: Option<PackageInfo>,
        url: Option<String>
    ): Item = withContext(Dispatchers.IO) {
        val itemEntity = requireNotNull(localItemDataSource.getById(shareId, itemId))

        val (item, itemProto) = encryptionContextProvider.withEncryptionContext {
            val item = itemEntity.toDomain(this@withEncryptionContext)
            val itemContents = decrypt(item.content)
            item to ItemV1.Item.parseFrom(itemContents)
        }

        val (needsToUpdate, updatedContents) = updateItemContents(
            item,
            itemProto,
            packageInfo,
            url
        )

        if (!needsToUpdate) {
            PassLogger.i(TAG, "Did not need to perform any update")
            return@withContext item
        }

        val userId = accountManager.getPrimaryUserId().first()
            ?: throw CryptoException("UserId cannot be null")
        val share = shareRepository.getById(userId, shareId)
        return@withContext performUpdate(
            userId,
            share,
            item,
            updatedContents
        )
    }

    override suspend fun refreshItems(userId: UserId, share: Share): List<Item> =
        withContext(Dispatchers.IO) {
            val address = requireNotNull(userAddressRepository.getAddresses(userId).primary())
            val items = remoteItemDataSource.getItems(address.userId, share.id)
            decryptItems(address, share, items)
        }

    override suspend fun refreshItems(userId: UserId, shareId: ShareId): List<Item> =
        withContext(Dispatchers.IO) {
            val share = shareRepository.getById(userId, shareId)
            refreshItems(userId, share)
        }

    override suspend fun applyEvents(
        userId: UserId,
        addressId: AddressId,
        shareId: ShareId,
        events: PendingEventList
    ) {
        withContext(Dispatchers.IO) {
            PassLogger.d(
                TAG,
                "Applying events: [updates=${events.updatedItems.size}] [deletes=${events.deletedItemIds.size}]"
            )

            val userAddress = requireNotNull(userAddressRepository.getAddress(userId, addressId))
            val share = shareRepository.getById(userId, shareId)
            val shareKeys = shareKeyRepository.getShareKeys(userId, addressId, shareId).first()

            val updateAsEntities = events.updatedItems.map {
                itemResponseToEntity(
                    userAddress,
                    it.toItemRevision(),
                    share,
                    shareKeys
                )
            }

            database.inTransaction {
                localItemDataSource.upsertItems(updateAsEntities)
                events.deletedItemIds.forEach { itemId ->
                    localItemDataSource.delete(shareId, ItemId(itemId))
                }
            }
            PassLogger.d(TAG, "Finishing applying events")
        }
    }

    override fun observeItemCountSummary(
        userId: UserId,
        shareIds: List<ShareId>,
        itemState: ItemState?,
    ): Flow<ItemCountSummary> =
        localItemDataSource.observeItemCountSummary(userId, shareIds, itemState)
            .flowOn(Dispatchers.IO)

    override suspend fun updateItemLastUsed(shareId: ShareId, itemId: ItemId) {
        withContext(Dispatchers.IO) {
            val userId = accountManager.getPrimaryUserId().first()
                ?: throw CryptoException("UserId cannot be null")

            PassLogger.i(TAG, "Updating last used time [shareId=$shareId][itemId=$itemId]")

            val now = TimeUtil.getNowUtc()
            localItemDataSource.updateLastUsedTime(shareId, itemId, now)
            remoteItemDataSource.updateLastUsedTime(userId, shareId, itemId, now)

            PassLogger.i(TAG, "Updated last used time [shareId=$shareId][itemId=$itemId]")
        }
    }

    override fun observeItemCount(shareIds: List<ShareId>): Flow<Map<ShareId, ShareItemCount>> =
        localItemDataSource.observeItemCount(shareIds)

    override suspend fun migrateItem(
        userId: UserId,
        source: Share,
        destination: Share,
        itemId: ItemId
    ): Item {
        val item = requireNotNull(localItemDataSource.getById(source.id, itemId))
        val destinationKey = shareKeyRepository.getLatestKeyForShare(destination.id).first()

        val body =
            migrateItem.migrate(destinationKey, item.encryptedContent, item.contentFormatVersion)
        val request = MigrateItemRequest(
            shareId = destination.id.id,
            item = body.toRequest()
        )

        val res = remoteItemDataSource.migrateItem(userId, source.id, ItemId(item.id), request)

        val userAddress = requireNotNull(userAddressRepository.getAddresses(userId).primary())
        val resAsEntity =
            itemResponseToEntity(userAddress, res, destination, listOf(destinationKey))
        database.inTransaction {
            localItemDataSource.upsertItem(resAsEntity)
            localItemDataSource.delete(source.id, ItemId(item.id))
        }

        return encryptionContextProvider.withEncryptionContext {
            resAsEntity.toDomain(this@withEncryptionContext)
        }
    }

    override suspend fun migrateItems(userId: UserId, source: ShareId, destination: ShareId) {
        val items = localItemDataSource.observeItemsForShare(
            userId = userId,
            shareId = source,
            itemState = ItemState.Active,
            filter = ItemTypeFilter.All
        ).first()
        val destinationKey = shareKeyRepository.getLatestKeyForShare(destination).first()

        withContext(Dispatchers.Default) {
            items.chunked(MAX_BATCH_ITEMS_PER_REQUEST).map { chunk ->
                async {
                    migrateChunk(userId, source, destination, destinationKey, chunk)
                }
            }.awaitAll()
        }
    }

    override suspend fun getItemByAliasEmail(userId: UserId, aliasEmail: String): Item? {
        val item = localItemDataSource.getItemByAliasEmail(userId, aliasEmail) ?: return null

        return encryptionContextProvider.withEncryptionContext {
            item.toDomain(this@withEncryptionContext)
        }
    }

    private suspend fun migrateChunk(
        userId: UserId,
        source: ShareId,
        destination: ShareId,
        destinationKey: ShareKey,
        chunk: List<ItemEntity>
    ) {
        val migrations = chunk.map { item ->
            val req = migrateItem.migrate(
                destinationKey = destinationKey,
                encryptedItemContents = item.encryptedContent,
                contentFormatVersion = item.contentFormatVersion
            )
            MigrateItemsBody(
                itemId = item.id,
                item = req.toRequest()
            )
        }

        val body = MigrateItemsRequest(
            shareId = destination.id,
            items = migrations
        )

        val destinationShare = shareRepository.getById(userId, destination)
        val userAddress = requireNotNull(userAddressRepository.getAddresses(userId).primary())

        val res = remoteItemDataSource.migrateItems(userId, source, body)

        val resAsEntities = res.map {
            itemResponseToEntity(userAddress, it, destinationShare, listOf(destinationKey))
        }

        database.inTransaction {
            localItemDataSource.upsertItems(resAsEntities)
            chunk.forEach {
                localItemDataSource.delete(source, ItemId(it.id))
            }
        }
    }

    private suspend fun restoreItemsForShare(
        userId: UserId,
        shareId: ShareId,
        shareItems: List<ItemEntity>
    ): Result<Unit> = shareItems.chunked(MAX_BATCH_ITEMS_PER_REQUEST).map { items ->
        val body = TrashItemsRequest(
            items.map {
                TrashItemRevision(
                    it.id,
                    it.revision
                )
            }
        )
        runCatching { remoteItemDataSource.untrash(userId, shareId, body) }
            .onSuccess {
                database.inTransaction {
                    items.forEach { item ->
                        localItemDataSource.setItemState(
                            shareId,
                            ItemId(item.id),
                            ItemState.Active
                        )
                    }
                }
            }
    }.transpose().map { }

    private suspend fun clearItemsForShare(
        userId: UserId,
        shareId: ShareId,
        shareItems: List<ItemEntity>
    ): Result<Unit> = shareItems.chunked(MAX_BATCH_ITEMS_PER_REQUEST).map { items ->
        val body =
            TrashItemsRequest(
                items.map {
                    TrashItemRevision(
                        it.id,
                        it.revision
                    )
                }
            )

        runCatching { remoteItemDataSource.delete(userId, shareId, body) }
            .onSuccess {
                database.inTransaction {
                    items.forEach { item ->
                        localItemDataSource.delete(
                            shareId,
                            ItemId(item.id)
                        )
                    }
                }
            }
            .onFailure {
                PassLogger.w(TAG, it, "Error clearing items for share")
            }
    }.transpose().map { }

    private fun updateItemContents(
        item: Item,
        itemProto: ItemV1.Item,
        packageInfoOption: Option<PackageInfo>,
        url: Option<String>
    ): Pair<Boolean, ItemV1.Item> {
        var needsToUpdate = false

        val itemContentsWithPackageName = when (packageInfoOption) {
            None -> itemProto
            is Some -> {
                if (itemProto.hasPackageName(packageInfoOption.value.packageName)) {
                    PassLogger.i(
                        TAG,
                        "Item already has this package name " +
                            "[shareId=${item.shareId}] [itemId=${item.id}] [packageName=$packageInfoOption]"
                    )
                    itemProto
                } else {
                    needsToUpdate = true
                    itemProto.with(packageInfoOption.value)
                }
            }
        }

        val updatedContents = when (url) {
            None -> itemContentsWithPackageName
            is Some -> when (val loginItem = item.itemType) {
                is ItemType.Login -> {
                    if (loginItem.hasWebsite(url.value)) {
                        // Item already has the URL, not doing anything
                        PassLogger.i(
                            TAG,
                            "Item already has the URL in the websites list, not performing any update"
                        )
                        itemContentsWithPackageName
                    } else {
                        // Item does not have the URL, adding it
                        needsToUpdate = true
                        itemContentsWithPackageName.withUrl(url.value)
                    }
                }

                else -> {
                    PassLogger.i(
                        TAG,
                        "Not performing any update, as we can only add urls to ItemType.Login"
                    )
                    itemContentsWithPackageName
                }
            }
        }

        return needsToUpdate to updatedContents
    }

    private suspend fun performUpdate(
        userId: UserId,
        share: Share,
        item: Item,
        itemContents: ItemV1.Item
    ): Item {
        return withUserAddress(userId) { userAddress ->
            val (shareKey, itemKey) = itemKeyRepository
                .getLatestItemKey(userId, userAddress.addressId, share.id, item.id)
                .first()
            val body = updateItem.createRequest(
                itemKey,
                itemContents,
                item.revision
            )
            val itemResponse = remoteItemDataSource.updateItem(
                userId = userId,
                shareId = share.id,
                itemId = item.id,
                body = body.toRequest()
            )
            val entity = itemResponseToEntity(
                userAddress,
                itemResponse,
                share,
                listOf(shareKey)
            )
            localItemDataSource.upsertItem(entity)
            encryptionContextProvider.withEncryptionContext {
                entity.toDomain(this@withEncryptionContext)
            }
        }
    }

    private suspend fun decryptItems(
        userAddress: UserAddress,
        share: Share,
        items: List<ItemRevision>
    ): List<Item> {
        val shareKeys = shareKeyRepository
            .getShareKeys(userAddress.userId, userAddress.addressId, share.id)
            .first()
        val itemsEntities = encryptionContextProvider.withEncryptionContextSuspendable {
            val encryptionContext = this@withEncryptionContextSuspendable
            withContext(Dispatchers.Default) {
                items.map { item ->
                    async {
                        decryptItem(
                            encryptionContext = encryptionContext,
                            userAddress = userAddress,
                            share = share,
                            item = item,
                            shareKeys = shareKeys
                        )
                    }
                }.awaitAll()
            }
        }

        val entities = itemsEntities.map { it.second }
        localItemDataSource.upsertItems(entities)

        return itemsEntities.map { it.first }
    }

    private fun decryptItem(
        encryptionContext: EncryptionContext,
        userAddress: UserAddress,
        share: Share,
        item: ItemRevision,
        shareKeys: List<ShareKey>
    ): Pair<Item, ItemEntity> {
        val entity = itemResponseToEntity(
            userAddress = userAddress,
            itemRevision = item,
            share = share,
            shareKeys = shareKeys
        )
        return entity.toDomain(encryptionContext) to entity
    }

    private fun itemResponseToEntity(
        userAddress: UserAddress,
        itemRevision: ItemRevision,
        share: Share,
        shareKeys: List<ShareKey>
    ): ItemEntity {
        val output = openItem.open(itemRevision.toCrypto(), share, shareKeys)
        val hasTotp = encryptionContextProvider.withEncryptionContext {
            output.item.hasTotp(this@withEncryptionContext)
        }
        return ItemEntity(
            id = itemRevision.itemId,
            userId = userAddress.userId.id,
            addressId = userAddress.addressId.id,
            shareId = share.id.id,
            revision = itemRevision.revision,
            contentFormatVersion = itemRevision.contentFormatVersion,
            content = itemRevision.content,
            state = itemRevision.state,
            itemType = output.item.itemType.toWeightedInt(),
            createTime = itemRevision.createTime,
            modifyTime = itemRevision.modifyTime,
            lastUsedTime = itemRevision.lastUseTime,
            encryptedContent = output.item.content,
            encryptedTitle = output.item.title,
            encryptedNote = output.item.note,
            aliasEmail = itemRevision.aliasEmail,
            keyRotation = itemRevision.keyRotation,
            key = itemRevision.itemKey,
            encryptedKey = output.itemKey,
            hasTotp = hasTotp,
        )
    }

    companion object {
        const val MAX_BATCH_ITEMS_PER_REQUEST = 50
        const val TAG = "ItemRepositoryImpl"
    }
}
