package com.tobeygronow.android.greenspot

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.Date

@Entity
data class Plant(
    @PrimaryKey val id: UUID,
    val title: String,
    val place: String,
    val date: Date,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoFileName: String? = null
)
