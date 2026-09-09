package com.example.domain

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.Household
import com.example.data.Person
import com.example.data.PersonHistory
import com.example.data.Gender
import com.example.data.HouseholdRole
import com.example.data.PersonStatus
import com.example.data.DataStatus
import com.example.utils.ValidationUtils
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ExcelImportUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(inputStream: InputStream): ExcelImportResult {
        var result = ExcelImportResult()
        
        try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            
            val errors = mutableListOf<ImportError>()
            var currentHouseNo = ""
            var currentHouseholdId = 0L
            
            // Collect data first to process in a transaction
            val rowsToProcess = mutableListOf<PersonDataToProcess>()
            
            val totalRows = maxOf(0, sheet.lastRowNum - 3) // Assuming header is up to row 3 (0-indexed 3)
            
            for (i in 4..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                
                var rowHouseNo = getCellValueAsString(row.getCell(0))
                if (rowHouseNo.isNotBlank()) {
                    currentHouseNo = rowHouseNo
                } else {
                    rowHouseNo = currentHouseNo
                }
                
                if (rowHouseNo.isBlank()) {
                    errors.add(ImportError(i + 1, "ไม่มีข้อมูลบ้านเลขที่"))
                    continue
                }
                
                val nationalId = getCellValueAsString(row.getCell(2)).replace("-", "").replace(" ", "")
                val fullName = getCellValueAsString(row.getCell(3))
                
                if (nationalId.isBlank() && fullName.isBlank()) continue // Skip completely empty trailing rows
                
                var isNationalIdValid = ValidationUtils.isValidThaiNationalId(nationalId)
                
                val gender = Gender.fromString(getCellValueAsString(row.getCell(4)))
                val rawBirthDate = row.getCell(5)
                val parsedDateResult = parseDateCellWithYearFlag(rawBirthDate)
                val birthDate = parsedDateResult.first
                val isBirthYearOnly = parsedDateResult.second
                val houseStatusStr = getCellValueAsString(row.getCell(7))
                val houseStatus = if (houseStatusStr.isBlank()) HouseholdRole.RESIDENT else HouseholdRole.fromString(houseStatusStr)
                val personStatusStr = getCellValueAsString(row.getCell(8))
                val personStatus = if (personStatusStr.isBlank()) PersonStatus.ALIVE else PersonStatus.fromString(personStatusStr)
                
                var dataStatusStr = getCellValueAsString(row.getCell(10))
                var dataStatus = if (dataStatusStr.isBlank()) DataStatus.NEEDS_REVIEW else DataStatus.fromString(dataStatusStr)
                
                var needsReview = false
                if (!isNationalIdValid) {
                    needsReview = true
                    errors.add(ImportError(i + 1, "เลขบัตรประชาชนไม่ถูกต้อง ($nationalId)"))
                }
                if (birthDate == null) {
                    needsReview = true
                    errors.add(ImportError(i + 1, "วันเกิดไม่ถูกต้อง"))
                }
                
                if (needsReview) {
                    dataStatus = DataStatus.NEEDS_REVIEW
                }
                
                rowsToProcess.add(
                    PersonDataToProcess(
                        rowNum = i + 1,
                        houseNo = rowHouseNo,
                        nationalId = nationalId,
                        fullName = fullName,
                        gender = gender,
                        birthDate = birthDate,
                        isBirthYearOnly = isBirthYearOnly,
                        houseStatus = houseStatus,
                        personStatus = personStatus,
                        dataStatus = dataStatus,
                        isNationalIdValid = isNationalIdValid,
                        hasValidBirthDate = birthDate != null
                    )
                )
            }
            
            workbook.close()
            
            // Execute DB operations inside a transaction
            database.withTransaction {
                val householdDao = database.householdDao()
                val personDao = database.personDao()
                val historyDao = database.personHistoryDao()
                
                // Cache households by houseNo to reduce DB lookups
                val householdCache = mutableMapOf<String, Long>()
                
                var successCount = 0
                var duplicateCount = 0
                var invalidNationalIdCount = 0
                var invalidBirthDateCount = 0
                var needsReviewCount = 0
                
                for (data in rowsToProcess) {
                    if (!data.isNationalIdValid) invalidNationalIdCount++
                    if (!data.hasValidBirthDate) invalidBirthDateCount++
                    if (data.dataStatus == DataStatus.NEEDS_REVIEW) needsReviewCount++
                    
                    // Check duplicate national ID
                    val existingPerson = personDao.getPersonByNationalId(data.nationalId)
                    if (existingPerson != null) {
                        duplicateCount++
                        errors.add(ImportError(data.rowNum, "เลขบัตรประชาชนซ้ำในระบบ (${data.nationalId})"))
                        continue
                    }
                    
                    // Resolve household
                    var householdId = householdCache[data.houseNo]
                    if (householdId == null) {
                        val existingHousehold = householdDao.getHouseholdByNo(data.houseNo)
                        if (existingHousehold != null) {
                            householdId = existingHousehold.id
                        } else {
                            // Insert new household
                            householdId = householdDao.insert(Household(houseNo = data.houseNo))
                        }
                        householdCache[data.houseNo] = householdId
                    }
                    
                    val newPerson = Person(
                        householdId = householdId,
                        nationalId = data.nationalId,
                        fullName = data.fullName,
                        gender = data.gender,
                        birthDate = data.birthDate,
                        isBirthYearOnly = data.isBirthYearOnly,
                        houseStatus = data.houseStatus,
                        personStatus = data.personStatus,
                        dataStatus = data.dataStatus
                    )
                    
                    val insertedId = personDao.insertPerson(newPerson)
                    
                    // Add audit log
                    historyDao.insert(
                        PersonHistory(
                            personId = insertedId,
                            action = "IMPORT_EXCEL",
                            oldValue = null,
                            newValue = "Imported from Excel Row ${data.rowNum}"
                        )
                    )
                    
                    successCount++
                }
                
                result = ExcelImportResult(
                    totalRows = totalRows,
                    successCount = successCount,
                    failedCount = rowsToProcess.size - successCount,
                    duplicateCount = duplicateCount,
                    invalidNationalIdCount = invalidNationalIdCount,
                    invalidBirthDateCount = invalidBirthDateCount,
                    invalidHouseNoCount = errors.count { it.reason.contains("บ้านเลขที่") },
                    needsReviewCount = needsReviewCount,
                    errors = errors
                )
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            result = ExcelImportResult(errors = listOf(ImportError(0, "System Error: ${e.message}")))
        } finally {
            inputStream.close()
        }
        
        return result
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

    private fun parseDateCellWithYearFlag(cell: Cell?): Pair<LocalDate?, Boolean> {
        if (cell == null) return Pair(null, false)
        return try {
            if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                val date = cell.dateCellValue
                Pair(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), false)
            } else {
                val str = getCellValueAsString(cell)
                ValidationUtils.parseThaiDate(str)
            }
        } catch (e: Exception) {
            Pair(null, false)
        }
    }
}

data class PersonDataToProcess(
    val rowNum: Int,
    val houseNo: String,
    val nationalId: String,
    val fullName: String,
    val gender: Gender,
    val birthDate: LocalDate?,
    val isBirthYearOnly: Boolean,
    val houseStatus: HouseholdRole,
    val personStatus: PersonStatus,
    val dataStatus: DataStatus,
    val isNationalIdValid: Boolean,
    val hasValidBirthDate: Boolean
)
