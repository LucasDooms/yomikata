package com.jehutyno.yomikata.presenters

interface LevelInterface {
    /**
     * Level up
     *
     * Updates the level and points in the database when a user manually levels up a word.
     *
     * @param ids Words to level up
     * @param points Current value of points (new points and level are calculated internally)
     */
    suspend fun levelUp(ids: LongArray, points: IntArray)
    /**
     * Level down
     *
     * Updates the level and points in the database when a user manually levels down a word.
     *
     * @param ids Words to level down
     * @param points Current value of points (new points and level are calculated internally)
     */
    suspend fun levelDown(ids: LongArray, points: IntArray)
}
