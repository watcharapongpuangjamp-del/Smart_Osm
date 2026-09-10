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

    fun exportExcelData(context: Context, uri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
                
                // Sheet 1: Persons
                val sheetPerson = workbook.createSheet("รายชื่อประชากร")
                val pHeader = sheetPerson.createRow(0)
                pHeader.createCell(0).setCellValue("รหัส")
                pHeader.createCell(1).setCellValue("รหัสครัวเรือน")
                pHeader.createCell(2).setCellValue("เลขบัตรประชาชน")
                pHeader.createCell(3).setCellValue("ชื่อ-นามสกุล")
                pHeader.createCell(4).setCellValue("เพศ")
                pHeader.createCell(5).setCellValue("วันเกิด")
                pHeader.createCell(6).setCellValue("สถานะครัวเรือน")
                pHeader.createCell(7).setCellValue("สถานะบุคคล")
                pHeader.createCell(8).setCellValue("สถานะข้อมูล")

                val persons = repository.getAllPersonsList()
                persons.forEachIndexed { index, p ->
                    val row = sheetPerson.createRow(index + 1)
                    row.createCell(0).setCellValue(p.id.toDouble())
                    row.createCell(1).setCellValue(p.householdId.toDouble())
                    row.createCell(2).setCellValue(p.nationalId)
                    row.createCell(3).setCellValue(p.fullName)
                    row.createCell(4).setCellValue(p.gender.name)
                    row.createCell(5).setCellValue(p.birthDate?.toString() ?: "")
                    row.createCell(6).setCellValue(p.houseStatus.name)
                    row.createCell(7).setCellValue(p.personStatus.name)
                    row.createCell(8).setCellValue(p.dataStatus.name)
                }

                // Sheet 2: Households
                val sheetHouse = workbook.createSheet("ข้อมูลครัวเรือน")
                val hHeader = sheetHouse.createRow(0)
                hHeader.createCell(0).setCellValue("รหัสครัวเรือน")
                hHeader.createCell(1).setCellValue("บ้านเลขที่")
                hHeader.createCell(2).setCellValue("หมู่ที่")
                hHeader.createCell(3).setCellValue("ตำบล")
                hHeader.createCell(4).setCellValue("อำเภอ")
                hHeader.createCell(5).setCellValue("จังหวัด")
                hHeader.createCell(6).setCellValue("ละติจูด")
                hHeader.createCell(7).setCellValue("ลองจิจูด")
                hHeader.createCell(8).setCellValue("สถานะข้อมูล")

                val households = repository.getAllHouseholds()
                households.forEachIndexed { index, h ->
                    val row = sheetHouse.createRow(index + 1)
                    row.createCell(0).setCellValue(h.id.toDouble())
                    row.createCell(1).setCellValue(h.houseNo)
                    row.createCell(2).setCellValue(h.villageNo)
                    row.createCell(3).setCellValue(h.subdistrict)
                    row.createCell(4).setCellValue(h.district)
                    row.createCell(5).setCellValue(h.province)
                    row.createCell(6).setCellValue(h.latitude ?: 0.0)
                    row.createCell(7).setCellValue(h.longitude ?: 0.0)
                    row.createCell(8).setCellValue(h.dataStatus.name)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                withContext(Dispatchers.Main) {
                    onComplete(true, "ส่งออกข้อมูลสำเร็จ")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false, "เกิดข้อผิดพลาด: ${e.message}")
                }
            }
        }
    }
}
