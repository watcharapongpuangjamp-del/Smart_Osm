package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "persons",
    foreignKeys = [
        ForeignKey(
            entity = Household::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("householdId"),
        Index("nationalId", unique = true)
    ]
)
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val householdId: Long,
    val nationalId: String,
    val fullName: String,
    val gender: Gender,
    val birthDate: LocalDate?,
    val houseStatus: HouseholdRole,
    val personStatus: PersonStatus,
    val dataStatus: DataStatus = DataStatus.NEEDS_REVIEW
)
