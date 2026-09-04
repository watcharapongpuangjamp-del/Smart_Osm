package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Person
import com.example.data.PersonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HouseSummary(
    val houseNo: String,
    val totalMembers: Int,
    val males: Int,
    val females: Int,
    val owners: Int,
    val residents: Int,
    val latitude: Double?,
    val longitude: Double?
)

class PersonViewModel(private val repository: PersonRepository) : ViewModel() {
    
    val allPersons: StateFlow<List<Person>> = repository.allPersons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insert(person: Person) = viewModelScope.launch { repository.insert(person) }
    fun update(person: Person) = viewModelScope.launch { repository.update(person) }
    fun delete(person: Person) = viewModelScope.launch { repository.delete(person) }
    
    suspend fun getPersonById(id: Long): Person? = repository.getPersonById(id)

    fun calculateAge(birthDate: LocalDate, personStatus: String): Int? {
        if (personStatus == "เสียชีวิต") return null
        val currentYear = LocalDate.now().year
        val birthYear = birthDate.year
        return currentYear - birthYear
    }

    fun getAgeGroup(age: Int?): String {
        if (age == null) return "ไม่ระบุ"
        return when {
            age <= 5 -> "เด็กเล็ก (0-5)"
            age <= 12 -> "เด็กวัยเรียน (6-12)"
            age <= 17 -> "วัยรุ่น (13-17)"
            age <= 24 -> "วัยหนุ่มสาว (18-24)"
            age <= 39 -> "วัยทำงานตอนต้น (25-39)"
            age <= 59 -> "วัยทำงานตอนกลาง (40-59)"
            else -> "ผู้สูงอายุ (60+)"
        }
    }

    val ageGroupSummary: StateFlow<Map<String, Int>> = allPersons.map { persons ->
        val summary = mutableMapOf<String, Int>().withDefault { 0 }
        persons.forEach { person ->
            val age = calculateAge(person.birthDate, person.personStatus)
            val group = getAgeGroup(age)
            summary[group] = summary.getValue(group) + 1
        }
        summary
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val houseSummary: StateFlow<List<HouseSummary>> = allPersons.map { persons ->
        persons.groupBy { it.houseNo }.map { (houseNo, list) ->
            HouseSummary(
                houseNo = houseNo,
                totalMembers = list.size,
                males = list.count { it.gender == "ชาย" },
                females = list.count { it.gender == "หญิง" },
                owners = list.count { it.houseStatus == "เจ้าบ้าน" },
                residents = list.count { it.houseStatus == "ผู้อาศัย" },
                latitude = list.firstOrNull { it.latitude != null }?.latitude,
                longitude = list.firstOrNull { it.longitude != null }?.longitude
            )
        }.sortedBy { it.houseNo }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
