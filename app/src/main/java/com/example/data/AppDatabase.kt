package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class, Household::class, PersonHistory::class], version = 5, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun householdDao(): HouseholdDao
    abstract fun personHistoryDao(): PersonHistoryDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Household: Add columns
                db.execSQL("ALTER TABLE households ADD COLUMN villageNo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE households ADD COLUMN subdistrict TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE households ADD COLUMN district TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE households ADD COLUMN province TEXT NOT NULL DEFAULT ''")
                
                // Recreate Person table to enforce new schema
                db.execSQL("CREATE TABLE IF NOT EXISTS `persons_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `householdId` INTEGER NOT NULL, `nationalId` TEXT NOT NULL, `fullName` TEXT NOT NULL, `gender` TEXT NOT NULL, `birthDate` TEXT, `houseStatus` TEXT NOT NULL, `personStatus` TEXT NOT NULL, `dataStatus` TEXT NOT NULL, FOREIGN KEY(`householdId`) REFERENCES `households`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO `persons_new` (`id`, `householdId`, `nationalId`, `fullName`, `gender`, `birthDate`, `houseStatus`, `personStatus`, `dataStatus`) SELECT `id`, `householdId`, `nationalId`, `fullName`, `gender`, `birthDate`, `houseStatus`, `personStatus`, `dataStatus` FROM `persons`")
                db.execSQL("DROP TABLE `persons`")
                db.execSQL("ALTER TABLE `persons_new` RENAME TO `persons`")
                
                // Create unique indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_households_houseNo_villageNo_subdistrict_district_province` ON `households` (`houseNo`, `villageNo`, `subdistrict`, `district`, `province`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_persons_householdId` ON `persons` (`householdId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_persons_nationalId` ON `persons` (`nationalId`)")
            }
        }
    }
}
