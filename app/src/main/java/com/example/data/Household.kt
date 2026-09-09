package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class Household(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseNo: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dataStatus: String = "ต้องตรวจสอบ"
)
