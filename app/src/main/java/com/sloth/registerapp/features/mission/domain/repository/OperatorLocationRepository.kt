package com.sloth.registerapp.features.mission.domain.repository

import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.Flow

interface OperatorLocationRepository {
    fun locationUpdates(): Flow<Point>
    suspend fun getLastKnownLocation(): Point?
}
