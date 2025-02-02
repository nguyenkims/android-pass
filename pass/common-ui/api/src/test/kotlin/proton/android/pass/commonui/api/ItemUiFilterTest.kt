package proton.android.pass.commonui.api
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

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import proton.android.pass.commonuimodels.fakes.TestItemUiModel
import proton.pass.domain.CreditCardType
import proton.pass.domain.HiddenState
import proton.pass.domain.ItemContents

class ItemUiFilterTest {

    @Test
    fun `filterByQuery should return the same list when query is empty`() {
        val list = listOf(
            TestItemUiModel.create(title = "item1", note = "note1"),
            TestItemUiModel.create(title = "item2", note = "note2")
        )
        val query = ""

        val result = ItemUiFilter.filterByQuery(list, query)

        assertEquals(list, result)
    }

    @Test
    fun `filterByQuery should return an empty list when query is blank`() {
        val list = listOf(
            TestItemUiModel.create(title = "item1", note = "note1"),
            TestItemUiModel.create(title = "item2", note = "note2")
        )
        val query = "   "

        val result = ItemUiFilter.filterByQuery(list, query)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterByQuery should filter items based on the query`() {
        val list = listOf(
            TestItemUiModel.create(title = "item1", note = "note1"),
            TestItemUiModel.create(title = "item2", note = "note2")
        )
        val query = "item1"

        val result = ItemUiFilter.filterByQuery(list, query)

        assertEquals(1, result.size)
        assertEquals("item1", result[0].contents.title)
        assertEquals("note1", result[0].contents.note)
    }

    @Test
    fun `filterByQuery should match alias title`() {
        val itemList = createAliasList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "title")

        assertEquals(2, filteredList.size)
    }

    @Test
    fun `filterByQuery should match alias email`() {
        val itemList = createAliasList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "test@example.com")

        assertEquals(1, filteredList.size)
    }

    @Test
    fun `filterByQuery should match login username `() {
        val itemList = createLoginList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "user")

        assertEquals(2, filteredList.size)
    }

    @Test
    fun `filterByQuery should match URL`() {
        val itemList = createLoginList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "test")

        assertEquals(1, filteredList.size)
    }

    @Test
    fun `filterByQuery should match cc title`() {
        val itemList = createCreditCardList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "cc")

        assertEquals(1, filteredList.size)
    }

    @Test
    fun `filterByQuery should match cc cardholder`() {
        val itemList = createCreditCardList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "maecenas")

        assertEquals(1, filteredList.size)
    }

    @Test
    fun `filterByQuery should match cc note`() {
        val itemList = createCreditCardList()
        val filteredList = ItemUiFilter.filterByQuery(itemList, "pertinacia")

        assertEquals(1, filteredList.size)
    }

    private fun createAliasList() = listOf(
        TestItemUiModel.create(
            itemContents = ItemContents.Alias(
                title = "Title",
                note = "Note",
                aliasEmail = "alias@example.com"
            )
        ),
        TestItemUiModel.create(
            itemContents = ItemContents.Alias(
                title = "Another Title",
                note = "Another Note",
                aliasEmail = "test@example.com"
            )
        )
    )

    private fun createCreditCardList() = listOf(
        TestItemUiModel.create(
            itemContents = ItemContents.CreditCard(
                title = "CC Item",
                note = "Note",
                cardHolder = "maecenas",
                type = CreditCardType.MasterCard,
                number = "ancillae",
                cvv = HiddenState.Empty(""),
                pin = HiddenState.Empty(""),
                expirationDate = "pro"
            )
        ),
        TestItemUiModel.create(
            itemContents = ItemContents.CreditCard(
                title = "dis",
                note = "pertinacia",
                cardHolder = "ei",
                type = CreditCardType.MasterCard,
                number = "viris",
                cvv = HiddenState.Empty(""),
                pin = HiddenState.Empty(""),
                expirationDate = "pertinacia"
            )
        )
    )

    private fun createLoginList() = listOf(
        TestItemUiModel.create(
            itemContents = ItemContents.Login(
                title = "Login Item",
                note = "Note",
                username = "username",
                urls = listOf("example.com", "test.com"),
                password = HiddenState.Empty(""),
                packageInfoSet = setOf(),
                primaryTotp = HiddenState.Empty(""),
                customFields = listOf()
            )
        ),
        TestItemUiModel.create(
            itemContents = ItemContents.Login(
                title = "Another Login",
                note = "Note",
                username = "user",
                urls = listOf("google.com"),
                password = HiddenState.Empty(""),
                packageInfoSet = setOf(),
                primaryTotp = HiddenState.Empty(""),
                customFields = listOf()
            )
        )
    )
}
