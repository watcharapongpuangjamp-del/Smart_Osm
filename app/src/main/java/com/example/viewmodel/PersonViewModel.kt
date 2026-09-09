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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import android.content.Context
import android.net.Uri
import android.widget.Toast
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Cell

import com.example.data.Household
import com.example.data.HouseholdWithPersons
import java.time.Period
import com.example.utils.ValidationUtils

import com.example.data.HouseSummary

class PersonViewModel(
    private val repository: PersonRepository,
    private val excelImportUseCase: com.example.domain.ExcelImportUseCase
) : ViewModel() {
    
    private val _importResult = kotlinx.coroutines.flow.MutableStateFlow<com.example.domain.ExcelImportResult?>(null)
    val importResult: StateFlow<com.example.domain.ExcelImportResult?> = _importResult
    
    private val _isImporting = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    fun clearImportResult() {
        _importResult.value = null
    }
    
    val allPersons: StateFlow<List<Person>> = repository.allPersons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val allHouseholdsWithPersons: StateFlow<List<HouseholdWithPersons>> = repository.allHouseholdsWithPersons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalPersonsCount: StateFlow<Int> = repository.totalPersonsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalHouseholdsCount: StateFlow<Int> = repository.totalHouseholdsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun insertHousehold(household: Household, onComplete: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertHousehold(household)
            withContext(Dispatchers.Main) {
                onComplete(id)
            }
        }
    }
    fun updateHousehold(household: Household) = viewModelScope.launch { repository.updateHousehold(household) }
    fun deleteHousehold(household: Household) = viewModelScope.launch { repository.deleteHousehold(household) }
    
    suspend fun getHouseholdById(id: Long): Household? = repository.getHouseholdById(id)
    fun getHouseholdWithPersonsById(id: Long) = repository.getHouseholdWithPersonsById(id)

    fun insert(person: Person) = viewModelScope.launch { repository.insert(person) }
    fun update(person: Person) = viewModelScope.launch { repository.update(person) }
    fun delete(person: Person) = viewModelScope.launch { repository.delete(person) }
    
    suspend fun getPersonById(id: Long): Person? = repository.getPersonById(id)
    suspend fun getPersonByNationalId(nationalId: String): Person? = repository.getPersonByNationalId(nationalId)
    
    fun validateThaiNationalId(id: String): Boolean = ValidationUtils.isValidThaiNationalId(id)

    fun calculateAge(birthDate: LocalDate?, personStatus: com.example.data.PersonStatus): Int? {
        if (personStatus == com.example.data.PersonStatus.DEAD || birthDate == null) return null
        return Period.between(birthDate, LocalDate.now()).years
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

    val houseSummary: StateFlow<List<HouseSummary>> = repository.houseSummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getHistoryForPerson(personId: Long) = repository.getHistoryForPerson(personId)

    fun importExcelData(context: Context, uri: Uri) {
        if (_isImporting.value) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            var inputStream: java.io.InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val result = excelImportUseCase(inputStream)
                    _importResult.value = result
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "ไม่สามารถเปิดไฟล์ได้", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                inputStream?.close()
                _isImporting.value = false
            }
        }
    }
}
