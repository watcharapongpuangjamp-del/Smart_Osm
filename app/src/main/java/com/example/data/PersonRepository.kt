package com.example.data

import kotlinx.coroutines.flow.Flow

class PersonRepository(
    private val personDao: PersonDao,
    private val householdDao: HouseholdDao
) {
    val allPersons: Flow<List<Person>> = personDao.getAllPersons()
    val allHouseholdsWithPersons: Flow<List<HouseholdWithPersons>> = householdDao.getHouseholdsWithPersons()

    // Household Operations
    suspend fun insertHousehold(household: Household): Long {
        return householdDao.insert(household)
    }

    suspend fun updateHousehold(household: Household) {
        householdDao.update(household)
    }

    suspend fun deleteHousehold(household: Household) {
        householdDao.delete(household)
    }

    suspend fun getHouseholdById(id: Long): Household? {
        return householdDao.getHouseholdById(id)
    }
    
    suspend fun getHouseholdByNo(houseNo: String): Household? {
        return householdDao.getHouseholdByNo(houseNo)
    }
    
    fun getHouseholdWithPersonsById(id: Long): Flow<HouseholdWithPersons?> {
        return householdDao.getHouseholdWithPersonsById(id)
    }

    // Person Operations
    suspend fun insert(person: Person) {
        personDao.insertPerson(person)
    }

    suspend fun update(person: Person) {
        personDao.updatePerson(person)
    }

    suspend fun delete(person: Person) {
        personDao.deletePerson(person)
    }
    
    suspend fun getPersonById(id: Long): Person? {
        return personDao.getPersonById(id)
    }
    
    suspend fun getPersonByNationalId(nationalId: String): Person? {
        return personDao.getPersonByNationalId(nationalId)
    }
}
