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

// 🔽 Pour l'envoi HTTP + JSON
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.io.BufferedReader
import java.io.InputStreamReader

// 🔽 Pour les logs
import android.util.Log

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
                ).setMaxUpdates(1).build()

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
        locationUpdateIntervalMs: Long = 1000L,
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
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1f)
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

    fun sendPositionToServer(busNumber: String, latitude: Double, longitude: Double) {
    val json = JSONObject().apply {
        put("busNumber", busNumber)
        put("latitude", latitude)
        put("longitude", longitude)
    }

    Thread {
        try {
            val url = URL("https://geektest.onrender.com/api/position")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.doInput = true // ← important !

            val output = BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8"))
            output.write(json.toString())
            output.flush()
            output.close()

            // Lis la réponse (très important pour que la requête soit proprement terminée)
            val input = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?

            while (input.readLine().also { line = it } != null) {
                response.append(line)
            }
            input.close()

            Log.d("API", "Réponse: ${connection.responseCode} ${response}")
        } catch (e: Exception) {
            Log.e("API", "Erreur d'envoi : ${e.message}")
        }
    }.start()
}
}