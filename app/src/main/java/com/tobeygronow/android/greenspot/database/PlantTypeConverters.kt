package com.tobeygronow.android.greenspot.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Contains the methods for converting data to and from SQLite data types, and Kotlin / Java data types
 */
class PlantTypeConverters {
    /**
     * Converts an SQLite Date to a Kotlin / Java Date
     */
    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    /**
     * Converts a Kotlin / Java Date to a SQLite Date
     */
    @TypeConverter
    fun toDate(millisSinceEpoch: Long): Date {
        return Date(millisSinceEpoch)
    }
}