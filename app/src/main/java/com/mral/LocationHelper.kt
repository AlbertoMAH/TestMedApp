package com.mral.LocationHelper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        onLocationResult: (Location?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            Toast.makeText(context, "Permission de localisation manquante", Toast.LENGTH_SHORT).show()
            onLocationResult(null)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationResult(location)
            } else {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    0L
                )
                    .setMaxUpdates(1)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        onLocationResult(result.lastLocation)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Erreur lors de la récupération de la position", Toast.LENGTH_SHORT).show()
            onLocationResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        locationUpdateIntervalMs: Long = 1000L, // Plus rapide : 1 seconde
        onLocationUpdate: (Location) -> Unit
    ): LocationCallback? {
        if (!hasLocationPermission(context)) {
            Toast.makeText(context, "Permission de localisation manquante", Toast.LENGTH_SHORT).show()
            return null
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            locationUpdateIntervalMs
        )
            .setMinUpdateIntervalMillis(500L) // Autorise des updates plus fréquents si dispo
            .setMinUpdateDistanceMeters(1f)   // Update dès 1 mètre de déplacement
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocationUpdate(it) }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return locationCallback
    }

    fun stopLocationUpdates(
        fusedLocationClient: FusedLocationProviderClient,
        locationCallback: LocationCallback?
    ) {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}