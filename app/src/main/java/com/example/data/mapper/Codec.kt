package com.example.data.mapper

import com.example.domain.model.GeoPoint
import com.example.domain.model.TrailWaypoint

/**
 * Encoding for list- and map-shaped fields stored in single Room columns.
 *
 * Room type converters were avoided deliberately here: keeping the encoding explicit and
 * local means the same helpers serialise for Firestore-free local storage and are easy to
 * reason about when reading raw rows during debugging. Every decoder is total — malformed
 * input yields an empty collection rather than an exception, because a corrupt cached row
 * must not crash the app on launch.
 */
object Codec {

    private const val ITEM = ";"
    private const val FIELD = "|"
    private const val PAIR = "="

    fun encodeList(values: Collection<String>): String =
        values.filter { it.isNotBlank() }.joinToString(ITEM) { it.replace(ITEM, ",") }

    fun decodeList(raw: String?): List<String> =
        raw?.split(ITEM)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun <T : Enum<T>> encodeEnums(values: Collection<T>): String = values.joinToString(ITEM) { it.name }

    fun <T : Enum<T>> decodeEnums(raw: String?, parse: (String) -> T?): Set<T> =
        decodeList(raw).mapNotNull { runCatching { parse(it) }.getOrNull() }.toSet()

    fun encodePoints(points: List<GeoPoint>): String =
        points.joinToString(ITEM) { "${it.latitude},${it.longitude}" }

    fun decodePoints(raw: String?): List<GeoPoint> =
        decodeList(raw).mapNotNull { chunk ->
            val parts = chunk.split(",")
            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull()
            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull()
            if (lat != null && lng != null) GeoPoint(lat, lng) else null
        }

    fun encodeWaypoints(waypoints: List<TrailWaypoint>): String =
        waypoints.joinToString(ITEM) { w ->
            listOf(w.name.replace(FIELD, "/"), w.kmMarker.toString(), w.elevationM.toString(), w.type)
                .joinToString(FIELD)
        }

    fun decodeWaypoints(raw: String?): List<TrailWaypoint> =
        decodeList(raw).mapNotNull { chunk ->
            val parts = chunk.split(FIELD)
            if (parts.size < 4) return@mapNotNull null
            TrailWaypoint(
                name = parts[0],
                kmMarker = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                elevationM = parts[2].toIntOrNull() ?: 0,
                type = parts[3]
            )
        }

    fun encodeLongMap(values: Map<String, Long>): String =
        values.entries.joinToString(ITEM) { "${it.key}$PAIR${it.value}" }

    fun decodeLongMap(raw: String?): Map<String, Long> =
        decodeList(raw).mapNotNull { chunk ->
            val parts = chunk.split(PAIR)
            val key = parts.getOrNull(0)?.trim().orEmpty()
            val value = parts.getOrNull(1)?.trim()?.toLongOrNull()
            if (key.isNotEmpty() && value != null) key to value else null
        }.toMap()
}
