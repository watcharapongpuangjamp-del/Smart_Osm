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

data class HouseSummary(
    val householdId: Long,
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
    
    val allHouseholdsWithPersons: StateFlow<List<HouseholdWithPersons>> = repository.allHouseholdsWithPersons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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

    val houseSummary: StateFlow<List<HouseSummary>> = allHouseholdsWithPersons.map { list ->
        list.map { item ->
            val persons = item.persons
            HouseSummary(
                householdId = item.household.id,
                houseNo = item.household.houseNo,
                totalMembers = persons.size,
                males = persons.count { it.gender == com.example.data.Gender.MALE },
                females = persons.count { it.gender == com.example.data.Gender.FEMALE },
                owners = persons.count { it.houseStatus == com.example.data.HouseholdRole.HEAD },
                residents = persons.count { it.houseStatus == com.example.data.HouseholdRole.RESIDENT },
                latitude = item.household.latitude,
                longitude = item.household.longitude
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getHistoryForPerson(personId: Long) = repository.getHistoryForPerson(personId)

    fun importExcelData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            var inputStream: java.io.InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                
                var currentHouseNo = ""
                var currentHouseholdId = 0L
                var importedCount = 0
                
                for (i in 4..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    
                    val tempHouseNo = getCellValueAsString(row.getCell(0))
                    if (tempHouseNo.isNotBlank()) {
                        currentHouseNo = tempHouseNo
                        
                        // Check if household exists or create one
                        var household = repository.getHouseholdByNo(currentHouseNo)
                        if (household == null) {
                            household = Household(houseNo = currentHouseNo)
                            currentHouseholdId = repository.insertHousehold(household)
                        } else {
                            currentHouseholdId = household.id
                        }
                    }
                    
                    val nationalId = getCellValueAsString(row.getCell(2))
                    val fullName = getCellValueAsString(row.getCell(3))
                    
                    if (nationalId.isBlank() && fullName.isBlank()) continue
                    
                    val gender = com.example.data.Gender.fromString(getCellValueAsString(row.getCell(4)))
                    val birthDate = parseDateCell(row.getCell(5))
                    val houseStatusStr = getCellValueAsString(row.getCell(7))
                    val houseStatus = if (houseStatusStr.isBlank()) com.example.data.HouseholdRole.RESIDENT else com.example.data.HouseholdRole.fromString(houseStatusStr)
                    val personStatusStr = getCellValueAsString(row.getCell(8))
                    val personStatus = if (personStatusStr.isBlank()) com.example.data.PersonStatus.ALIVE else com.example.data.PersonStatus.fromString(personStatusStr)
                    val dataStatusStr = getCellValueAsString(row.getCell(10))
                    val dataStatus = if (birthDate == null) com.example.data.DataStatus.NEEDS_REVIEW else if (dataStatusStr.isBlank()) com.example.data.DataStatus.NEEDS_REVIEW else com.example.data.DataStatus.fromString(dataStatusStr)
                    
                    val person = Person(
                        householdId = currentHouseholdId,
                        nationalId = nationalId,
                        fullName = fullName,
                        gender = gender,
                        birthDate = birthDate,
                        houseStatus = houseStatus,
                        personStatus = personStatus,
                        dataStatus = dataStatus
                    )
                    
                    // Basic duplicate check for Excel imports
                    if (repository.getPersonByNationalId(nationalId) == null) {
                        repository.insert(person)
                        importedCount++
                    }
                }
                
                workbook.close()
                inputStream?.close()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "นำเข้าข้อมูลสำเร็จ $importedCount รายการ", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                inputStream?.close()
            }
        }
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val date = cell.dateCellValue
                        date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    } else {
                        BigDecimal(cell.numericCellValue).toPlainString()
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    when (cell.cachedFormulaResultType) {
                        CellType.STRING -> cell.richStringCellValue.string.trim()
                        CellType.NUMERIC -> BigDecimal(cell.numericCellValue).toPlainString()
                        else -> ""
                    }
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseDateCell(cell: Cell?): LocalDate? {
        if (cell == null) return null
        return try {
            if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                val date = cell.dateCellValue
                date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                val str = getCellValueAsString(cell)
                if (str.isNotBlank()) {
                    try {
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        LocalDate.parse(str, formatter)
                    } catch (e: Exception) {
                        try {
                            LocalDate.parse(str) // fallback yyyy-MM-dd
                        } catch (e2: Exception) {
                            null
                        }
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
