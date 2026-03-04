package com.axon.data.local.entity

import androidx.room.TypeConverter
import com.axon.domain.model.SessionRepResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<SessionRepResult>? {
        if (value == null) return null
        val listType = object : TypeToken<List<SessionRepResult>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<SessionRepResult>?): String? {
        if (list == null) return null
        val gson = Gson()
        return gson.toJson(list)
    }
}
