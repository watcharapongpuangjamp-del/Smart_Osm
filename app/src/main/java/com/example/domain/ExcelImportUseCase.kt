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

class ExcelImportUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(inputStream: InputStream): ExcelImportResult {
        val errors = mutableListOf<ImportError>()
        var totalRows = 0
        var successCount = 0
        var duplicateCount = 0
        var invalidNationalIdCount = 0
        var invalidBirthDateCount = 0
        var needsReviewCount = 0

        try {
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            
            val rowsToProcess = mutableListOf<ImportRowData>()
            
            var startRow = 1
            val headerRow = sheet.getRow(0)
            if (headerRow != null && getCellValueAsString(headerRow.getCell(0)).contains("schemaVersion", ignoreCase = true)) {
                startRow = 1
            } else if (sheet.lastRowNum > 0 && sheet.getRow(1)?.getCell(0)?.toString()?.contains("schemaVersion", ignoreCase = true) == true) {
                startRow = 2
            } else {
                startRow = 1
            }

            for (i in startRow..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                totalRows++

                val colCount = row.lastCellNum.toInt()
                val isV1 = colCount >= 12 && (getCellValueAsString(row.getCell(0)).equals("SMART_OSM_EXCEL_V1", ignoreCase = true) || colCount >= 15)

                val householdUuid = if (isV1 && colCount > 1) getCellValueAsString(row.getCell(1)) else ""
                val personUuid = if (isV1 && colCount > 2) getCellValueAsString(row.getCell(2)) else ""
                
                val houseNoIdx = if (isV1) 3 else 0
                val houseNo = getCellValueAsString(row.getCell(houseNoIdx))
                
                if (houseNo.isBlank()) {
                    errors.add(ImportError(i + 1, "ไม่มีข้อมูลบ้านเลขที่"))
                    continue
                }

                val villageNo = if (isV1) getCellValueAsString(row.getCell(4)) else ""
                val subdistrict = if (isV1) getCellValueAsString(row.getCell(5)) else ""
                val district = if (isV1) getCellValueAsString(row.getCell(6)) else ""
                val province = if (isV1) getCellValueAsString(row.getCell(7)) else ""

                val natIdIdx = if (isV1) 8 else 2
                val rawNatId = getCellValueAsString(row.getCell(natIdIdx))
                val nationalId = ValidationUtils.normalizeNationalId(rawNatId).ifBlank { null }

                val nameIdx = if (isV1) 9 else 3
                val fullName = getCellValueAsString(row.getCell(nameIdx))

                if (houseNo.isBlank() && nationalId.isNullOrBlank() && fullName.isBlank()) {
                    totalRows--
                    continue
                }

                val genderIdx = if (isV1) 10 else 4
                val gender = Gender.fromString(getCellValueAsString(row.getCell(genderIdx)))

                val dobIdx = if (isV1) 11 else 5
                val rawDobCell = row.getCell(dobIdx)
                val parsedDateResult = parseDateCell(rawDobCell)
                val birthDate = parsedDateResult.first
                val isBirthYearOnly = parsedDateResult.second

                val hStatusIdx = if (isV1) 13 else 7
                val houseStatus = HouseholdRole.fromString(getCellValueAsString(row.getCell(hStatusIdx)))

                val pStatusIdx = if (isV1) 14 else 8
                val personStatus = PersonStatus.fromString(getCellValueAsString(row.getCell(pStatusIdx)))

                var isNationalIdValid = true
                if (nationalId != null) {
                    if (!ValidationUtils.isValidThaiNationalId(nationalId)) {
                        isNationalIdValid = false
                        invalidNationalIdCount++
                        errors.add(ImportError(i + 1, "เลขบัตรประชาชนไม่ถูกต้อง ($nationalId)"))
                    }
                }

                var hasValidBirthDate = birthDate != null
                if (!hasValidBirthDate) {
                    invalidBirthDateCount++
                    errors.add(ImportError(i + 1, "วันเกิดไม่ถูกต้องหรือว่างเปล่า"))
                }

                var dataStatus = DataStatus.VERIFIED
                if (!isNationalIdValid || !hasValidBirthDate) {
                    dataStatus = DataStatus.NEEDS_REVIEW
                    needsReviewCount++
                }

                rowsToProcess.add(
                    ImportRowData(
                        rowNum = i + 1,
                        householdUuid = householdUuid.ifBlank { java.util.UUID.randomUUID().toString() },
                        personUuid = personUuid.ifBlank { java.util.UUID.randomUUID().toString() },
                        houseNo = houseNo,
                        villageNo = villageNo,
                        subdistrict = subdistrict,
                        district = district,
                        province = province,
                        nationalId = nationalId,
                        fullName = fullName,
                        gender = gender,
                        birthDate = birthDate,
                        isBirthYearOnly = isBirthYearOnly,
                        houseStatus = houseStatus,
                        personStatus = personStatus,
                        dataStatus = dataStatus,
                        isNationalIdValid = isNationalIdValid
                    )
                )
            }
            workbook.close()

            database.withTransaction {
                val householdDao = database.householdDao()
                val personDao = database.personDao()
                val historyDao = database.personHistoryDao()

                val existingHouseholds = householdDao.getAllHouseholds()
                val householdsByUuid = existingHouseholds.associateBy { it.householdUuid }
                val householdsByAddress = existingHouseholds.associateBy { h ->
                    "${h.houseNo.trim()}|${h.villageNo.trim()}|${h.subdistrict.trim()}|${h.district.trim()}|${h.province.trim()}".lowercase()
                }

                val existingPersons = personDao.getAllPersonsList()
                val personsByUuid = existingPersons.associateBy { it.personUuid }.toMutableMap()
                val personsByNationalId = existingPersons.filter { !it.nationalId.isNullOrBlank() }.associateBy { it.nationalId!! }.toMutableMap()

                val seenNationalIdsInFile = mutableSetOf<String>()
                val seenPersonUuidsInFile = mutableSetOf<String>()

                val mutableHouseholdsByUuid = householdsByUuid.toMutableMap()
                val mutableHouseholdsByAddress = householdsByAddress.toMutableMap()

                for (data in rowsToProcess) {
                    if (data.nationalId != null) {
                        if (!seenNationalIdsInFile.add(data.nationalId)) {
                            duplicateCount++
                            errors.add(ImportError(data.rowNum, "เลขบัตรประชาชนซ้ำภายในไฟล์ Excel (${data.nationalId})"))
                            continue
                        }
                    }
                    if (!seenPersonUuidsInFile.add(data.personUuid)) {
                        data.personUuid = java.util.UUID.randomUUID().toString()
                    }

                    val addressKey = "${data.houseNo.trim()}|${data.villageNo.trim()}|${data.subdistrict.trim()}|${data.district.trim()}|${data.province.trim()}".lowercase()
                    
                    var household = mutableHouseholdsByUuid[data.householdUuid]
                        ?: mutableHouseholdsByAddress[addressKey]

                    val householdId: Long = if (household != null) {
                        household.id
                    } else {
                        val newHousehold = Household(
                            householdUuid = data.householdUuid,
                            houseNo = data.houseNo,
                            villageNo = data.villageNo,
                            subdistrict = data.subdistrict,
                            district = data.district,
                            province = data.province,
                            dataStatus = data.dataStatus
                        )
                        val newId = householdDao.insert(newHousehold)
                        val inserted = newHousehold.copy(id = newId)
                        mutableHouseholdsByUuid[inserted.householdUuid] = inserted
                        mutableHouseholdsByAddress[addressKey] = inserted
                        newId
                    }

                    val existingPerson = personsByUuid[data.personUuid] 
                        ?: (if (data.nationalId != null) personsByNationalId[data.nationalId] else null)

                    if (existingPerson != null) {
                        val updatedPerson = existingPerson.copy(
                            householdId = householdId,
                            nationalId = data.nationalId ?: existingPerson.nationalId,
                            fullName = data.fullName.ifBlank { existingPerson.fullName },
                            gender = data.gender,
                            birthDate = data.birthDate ?: existingPerson.birthDate,
                            isBirthYearOnly = data.isBirthYearOnly,
                            houseStatus = data.houseStatus,
                            personStatus = data.personStatus,
                            dataStatus = data.dataStatus
                        )
                        personDao.updatePerson(updatedPerson)
                        historyDao.insert(
                            PersonHistory(
                                personId = existingPerson.id,
                                action = "UPDATE_EXCEL",
                                oldValue = existingPerson.toString(),
                                newValue = updatedPerson.toString(),
                                operatorId = "IMPORT_USER",
                                operatorName = "Excel Importer",
                                role = "ADMIN",
                                deviceId = "local",
                                source = "EXCEL_IMPORT"
                            )
                        )
                        successCount++
                    } else {
                        val newPerson = Person(
                            personUuid = data.personUuid,
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
                        personsByUuid[newPerson.personUuid] = newPerson.copy(id = insertedId)
                        if (newPerson.nationalId != null) {
                            personsByNationalId[newPerson.nationalId] = newPerson.copy(id = insertedId)
                        }

                        historyDao.insert(
                            PersonHistory(
                                personId = insertedId,
                                action = "CREATE_EXCEL",
                                oldValue = null,
                                newValue = newPerson.toString(),
                                operatorId = "IMPORT_USER",
                                operatorName = "Excel Importer",
                                role = "ADMIN",
                                deviceId = "local",
                                source = "EXCEL_IMPORT"
                            )
                        )
                        successCount++
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            errors.add(ImportError(0, "เกิดข้อผิดพลาดในการอ่านไฟล์: ${e.message}"))
        }

        return ExcelImportResult(
            totalRows = totalRows,
            successCount = successCount,
            failedCount = errors.size,
            duplicateCount = duplicateCount,
            invalidNationalIdCount = invalidNationalIdCount,
            invalidBirthDateCount = invalidBirthDateCount,
            invalidHouseNoCount = errors.count { it.reason.contains("บ้านเลขที่") },
            needsReviewCount = needsReviewCount,
            errors = errors
        )
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        cell.dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
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

    private fun parseDateCell(cell: Cell?): Pair<LocalDate?, Boolean> {
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

data class ImportRowData(
    val rowNum: Int,
    var householdUuid: String,
    var personUuid: String,
    val houseNo: String,
    val villageNo: String,
    val subdistrict: String,
    val district: String,
    val province: String,
    val nationalId: String?,
    val fullName: String,
    val gender: Gender,
    val birthDate: LocalDate?,
    val isBirthYearOnly: Boolean,
    val houseStatus: HouseholdRole,
    val personStatus: PersonStatus,
    val dataStatus: DataStatus,
    val isNationalIdValid: Boolean
)
