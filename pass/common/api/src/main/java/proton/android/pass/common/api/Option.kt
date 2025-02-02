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

package proton.android.pass.common.api

sealed interface Option<out A> {

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    fun <R> map(block: (A) -> R): Option<R>

    fun <B> flatMap(f: (A) -> Option<B>): Option<B> =
        when (this) {
            is None -> this
            is Some -> f(value)
        }

    fun value(): A? = when (this) {
        None -> null
        is Some -> this.value
    }

    companion object {

        @JvmStatic
        fun <A> fromNullable(a: A?): Option<A> = if (a != null) Some(a) else None

        @JvmStatic
        operator fun <A> invoke(a: A): Option<A> = Some(a)
    }
}

object None : Option<Nothing> {
    override fun isEmpty(): Boolean = true

    override fun toString(): String = "Option.None"

    override fun <R> map(block: (Nothing) -> R): Option<R> = None
}

data class Some<out T>(val value: T) : Option<T> {

    override fun isEmpty(): Boolean = false

    override fun toString(): String = "Option.Some($value)"

    override fun <R> map(block: (T) -> R): Option<R> = Some(block(value))

    companion object {
        @PublishedApi
        internal val unit: Option<Unit> = Some(Unit)
    }
}

fun <A> A.some(): Option<A> = Some(this)

fun <A> none(): Option<A> = None

fun <T> T?.toOption(): Option<T> = this?.let { Some(it) } ?: None
