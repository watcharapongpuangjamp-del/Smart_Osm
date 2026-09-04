package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseNo: String,
    val nationalId: String,
    val fullName: String,
    val gender: String, // ชาย / หญิง
    val birthDate: LocalDate,
    val houseStatus: String, // เจ้าบ้าน / ผู้อาศัย
    val personStatus: String, // มีชีวิต / เสียชีวิต
    val dataStatus: String, // ยืนยันแล้ว / ต้องตรวจสอบ
    val latitude: Double? = null,
    val longitude: Double? = null
)
