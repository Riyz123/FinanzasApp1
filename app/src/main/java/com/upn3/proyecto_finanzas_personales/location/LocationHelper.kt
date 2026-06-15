package com.upn3.proyecto_finanzas_personales.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationHelper(context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun isLocationEnabled(): Boolean =
        LocationManagerCompat.isLocationEnabled(locationManager)

    /**
     * Returns (latitude, longitude) or null if unavailable.
     * Caller must hold ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!isLocationEnabled()) return@withContext null

        // 1. Try last known location (acceptable if < 60 s old)
        val lastLoc = runCatching { fusedClient.lastLocation.await() }.getOrNull()
        if (lastLoc != null && System.currentTimeMillis() - lastLoc.time <= 60_000L) {
            return@withContext Pair(lastLoc.latitude, lastLoc.longitude)
        }

        // 2. Request a fresh fix with 9 s hard timeout
        val cts = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setDurationMillis(8_000L)
            .setMaxUpdateAgeMillis(20_000L)
            .build()

        withTimeoutOrNull(9_000L) {
            runCatching {
                fusedClient.getCurrentLocation(request, cts.token).await()
            }.getOrNull()?.let { Pair(it.latitude, it.longitude) }
        }
    }
}
