package com.jehutyno.yomikata.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking


/**
 * Execute functions in batches of 500 to prevent issues with
 * the SQLite max of 999 placeholder ('?') operators
 */


/**
 * Add maps by grouping their values with the same keys.
 */
private fun <K, V> MutableMap<K, List<V>>.customPlusAssign(other: Map<K, List<V>>) {
    other.forEach { (t, u) ->
        if (t !in this) {
            this[t] = u
        } else {
            this[t] = this[t]!! + u
        }
    }
}

/**
 * Add maps by grouping their values with the same keys.
 */
private fun <K, V> Map<K, List<V>>.customPlus(other: Map<K, List<V>>): Map<K, List<V>> {
    val copy = this.toMutableMap()
    copy.customPlusAssign(other)
    return copy
}


/**
 * In batches general
 *
 * Generic function to perform [block] in batches, and concatenate the result.
 *
 * @param block Code that is called with the batches of the LongArray as input
 * @param accumulator Value to concatenate onto (usually empty, e.g. listOf())
 * @param customPlus Defines the concatenation ( x += y )
 * @return Concatenated result of the returned values from [block] calls
 */
private suspend fun <T> LongArray.inBatchesGeneral(
    block: suspend (LongArray) -> T, accumulator: T, customPlus: T.(T) -> T
): T {
    val batchSize = 500
    var index = 0
    val size = this.size

    var result = accumulator
    while (index < size) {
        result = result.customPlus(
            block(
                copyOfRange(index, (index + batchSize).coerceAtMost(size))
            )
        )
        index += batchSize
    }
    return result
}


/**
 * In batches with return
 *
 * Executes the block in batches of 500. Use for lists.
 *
 * @param block Function to call in batches
 * @receiver LongArray (typically ids)
 * @return Concatenated lists that were returned by block calls
 */
suspend fun <T> LongArray.inBatchesWithReturn(block: suspend (LongArray) -> List<T>): List<T> {
    return this.inBatchesGeneral(block, listOf(), List<T>::plus).toList()
}


/**
 * In batches
 *
 * Same as inBatchesWithReturn, but ignores return value.
 *
 */
suspend fun LongArray.inBatches(block: suspend (LongArray) -> Unit) {
    this.inBatchesWithReturn {
        block(it)
        mutableListOf<Int>()   // empty list which we will ignore
    }
}


/**
 * In batches with return map
 *
 * Executes the block in batches of 500. Use for maps.
 *
 * @param block Function to call in batches
 * @receiver LongArray (typically ids)
 * @return Concatenated maps that were returned by block calls.
 * The concatenation works by concatenating the lists for the required key.
 */
suspend fun <K, V> LongArray.inBatchesWithReturnMap(block: suspend (LongArray) -> Map<K, List<V>>): Map<K, List<V>> {
    return this.inBatchesGeneral(block, mapOf(), Map<K, List<V>>::customPlus)
}


/**
 * In batches general non suspend
 *
 * Generic function to perform [block] in batches, and concatenate the result.
 *
 * @param block Code that is called with the batches of the LongArray as input
 * @param empty Empty value to start the concatenation (e.g. listOf())
 * @param customPlus Defines the concatenation ( x + y )
 * @return Concatenated result of the returned values from [block] calls
 */
private fun <T> LongArray.inBatchesGeneralNonSuspend(block: (LongArray) -> T, empty: T, customPlus: T.(T) -> T): T {
    fun hi(x: LongArray): T {
        return block(x)
    }
    return runBlocking {
        return@runBlocking this@inBatchesGeneralNonSuspend.inBatchesGeneral(::hi, empty, customPlus)
    }
}


private fun <T> Flow<List<T>>.customPlusL(other: Flow<List<T>>): Flow<List<T>> {
    return this.combine(other) { one, two -> one + two }
}

/**
 * In batches with flow return L
 *
 * Returns a flow by performing the block in batches of 500, and concatenating the lists
 * in a new flow.
 *
 * @param block Function that returns a flow, which will be performed in batches
 * @return New flow containing the concatenated results
 */
fun <T> LongArray.inBatchesWithFlowReturnL(block: (LongArray) -> Flow<List<T>>): Flow<List<T>> {
    return this.inBatchesGeneralNonSuspend(block, flowOf(listOf()), Flow<List<T>>::customPlusL)
}


private fun Flow<Int>.customPlusI(other: Flow<Int>): Flow<Int> {
    return this.combine(other) { one, two -> one + two }
}

/**
 * In batches with flow return
 *
 * Returns a flow by performing the block in batches of 500, and concatenating the lists
 * in a new flow.
 *
 * @param block Function that returns a flow, which will be performed in batches
 * @return New flow containing the concatenated results
 */
fun LongArray.inBatchesWithFlowReturn(block: (LongArray) -> Flow<Int>): Flow<Int> {
    return this.inBatchesGeneralNonSuspend(block, flowOf(0), Flow<Int>::customPlusI)
}
