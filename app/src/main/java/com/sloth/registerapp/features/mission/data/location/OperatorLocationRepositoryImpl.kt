package com.sloth.registerapp.features.mission.data.location

import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.sloth.registerapp.features.mission.domain.repository.OperatorLocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OperatorLocationRepositoryImpl(
    context: Context
) : OperatorLocationRepository {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    override fun locationUpdates(): Flow<Point> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(Point.fromLngLat(location.longitude, location.latitude))
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (security: SecurityException) {
            close(security)
        }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }.distinctUntilChanged()

    override suspend fun getLastKnownLocation(): Point? =
        suspendCancellableCoroutine { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        continuation.resume(null)
                    } else {
                        continuation.resume(Point.fromLngLat(location.longitude, location.latitude))
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
}
