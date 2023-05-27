package com.jehutyno.yomikata.util


/** execute in batches of 500 to prevent issues with SQLite max of 999 '?' operators */
suspend fun LongArray.inBatches(block: suspend (LongArray) -> Unit) {
    this.inBatchesWithReturn {
        block(it)
        listOf<Int>()   // empty list which we will ignore
    }
}


/**
 * In batches with return
 *
 * Executes the block in batches of 500.
 *
 * @param T Type in returned list
 * @param block Function to call in batches
 * @receiver LongArray (typically ids)
 * @return Concatenated lists that were returned by block calls
 */
suspend fun <T> LongArray.inBatchesWithReturn(block: suspend (LongArray) -> Iterable<T>): List<T> {
    val batchSize = 500
    var index = 0
    val size = this.size
    val result = mutableListOf<T>()
    while (index < size) {
        result += block (
            this.sliceArray(index until (index + batchSize).coerceAtMost(size))
        )
        index += batchSize
    }
    return result
}


suspend fun <K, V> LongArray.inBatchesWithReturnMap(block: suspend (LongArray) -> Map<K, List<V>>): Map<K, List<V>> {
    val batchSize = 500
    var index = 0
    val size = this.size
    val result = mutableMapOf<K, List<V>>()
    while (index < size) {
        result.customPlusAssign(
            block (
                this.sliceArray(index until (index + batchSize).coerceAtMost(size))
            )
        )
        index += batchSize
    }
    return result
}


/**
 * Add maps by grouping their values with the same keys.
 */
fun <K, V> MutableMap<K, List<V>>.customPlusAssign(other: Map<K, List<V>>) {
    other.forEach { (t, u) ->
        if (t !in this) {
            this[t] = u
        } else {
            this[t] = this[t]!! + u
        }
    }
}
