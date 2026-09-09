package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(household: Household): Long

    @Update
    suspend fun update(household: Household)

    @Delete
    suspend fun delete(household: Household)

    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    suspend fun getHouseholdById(id: Long): Household?

    @Query("SELECT * FROM households WHERE houseNo = :houseNo LIMIT 1")
    suspend fun getHouseholdByNo(houseNo: String): Household?

    @Transaction
    @Query("SELECT * FROM households ORDER BY houseNo ASC")
    fun getHouseholdsWithPersons(): Flow<List<HouseholdWithPersons>>

    @Transaction
    @Query("SELECT * FROM households WHERE id = :householdId LIMIT 1")
    fun getHouseholdWithPersonsById(householdId: Long): Flow<HouseholdWithPersons?>
}
