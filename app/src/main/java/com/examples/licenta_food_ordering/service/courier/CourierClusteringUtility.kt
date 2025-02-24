package com.examples.licenta_food_ordering.service.courier

import com.google.android.gms.maps.model.LatLng
import kotlin.math.pow
import kotlin.math.sqrt

class CourierClusteringUtility {
    fun generateHotspots(locations: List<LatLng>, k: Int): List<LatLng> {
        if (locations.isEmpty() || k <= 0) return emptyList()

        val randomCenters = locations.shuffled().take(k).toMutableList()
        var clusters: MutableMap<LatLng, MutableList<LatLng>>
        var newCenters: List<LatLng>

        do {
            clusters = mutableMapOf()
            randomCenters.forEach { clusters[it] = mutableListOf() }

            locations.forEach { loc ->
                val closestCenter = randomCenters.minByOrNull { center -> distance(loc, center) }!!
                clusters[closestCenter]?.add(loc)
            }

            newCenters = clusters.map { (_, points) ->
                val avgLat = points.map { it.latitude }.average()
                val avgLng = points.map { it.longitude }.average()
                LatLng(avgLat, avgLng)
            }
        } while (newCenters != randomCenters.also { randomCenters.clear(); randomCenters.addAll(newCenters) })

        return newCenters
    }

    private fun distance(a: LatLng, b: LatLng): Double {
        return sqrt((a.latitude - b.latitude).pow(2) + (a.longitude - b.longitude).pow(2))
    }
}