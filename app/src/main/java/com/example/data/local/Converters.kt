package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.TrailPoint
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    @TypeConverter
    fun fromTrailPoints(value: List<TrailPoint>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach {
            val obj = JSONObject().apply {
                put("name", it.name)
                put("kmMarker", it.kmMarker)
                put("elevationM", it.elevationM)
                put("type", it.type)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toTrailPoints(value: String?): List<TrailPoint> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<TrailPoint>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TrailPoint(
                        name = obj.optString("name", ""),
                        kmMarker = obj.optDouble("kmMarker", 0.0),
                        elevationM = obj.optInt("elevationM", 0),
                        type = obj.optString("type", "POINT")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
