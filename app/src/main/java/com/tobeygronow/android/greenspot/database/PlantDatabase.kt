package com.tobeygronow.android.greenspot.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tobeygronow.android.greenspot.Plant

/**
 * Define the Plant database
 */
@Database(entities = [Plant::class], version = 1, exportSchema = false)
@TypeConverters(PlantTypeConverters::class)
abstract class PlantDatabase : RoomDatabase() {
    /**
     * The DAO given to the database
     */
    abstract fun plantDao(): PlantDao
}