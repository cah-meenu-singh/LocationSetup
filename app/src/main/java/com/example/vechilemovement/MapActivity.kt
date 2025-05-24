package com.example.vechilemovement

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var carMarker: Marker
    private lateinit var searchMarker: Marker
    private lateinit var database: DatabaseReference
    private lateinit var speedText: TextView
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private var lastLocation: GeoPoint? = null
    private var isFirstUpdate = true
    private val localLatitude = 28.6139  // Example: New Delhi
    private val localLongitude = 77.2090

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        setContentView(R.layout.activity_map)

        initViews()
        setupMap()
       // setupMarkers()
        pinLocalLocation()
        setupFirebase()
        setupSearchFunction()
    }

    private fun initViews() {
        speedText = findViewById(R.id.speedText)
        map = findViewById(R.id.map)
        searchInput = findViewById(R.id.searchInput)
        searchButton = findViewById(R.id.searchButton)
    }

    private fun setupMap() {
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
    }

    private fun pinLocalLocation() {
        val localPoint = GeoPoint(localLatitude, localLongitude)
        val localMarker = Marker(map).apply {
            position = localPoint
            title = "Local Location"
            snippet = "Pinned from local lat/lng"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = BitmapDrawable(
                resources,
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(resources, R.drawable.ic_location_pin),
                    80, 80, true
                )
            )
        }

        map.overlays.add(localMarker)
        map.controller.animateTo(localPoint)
        map.invalidate()
    }


    private fun setupMarkers() {
        // Car Marker
        carMarker = Marker(map).apply {
            title = "Vehicle"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = BitmapDrawable(
                resources,
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(resources, R.drawable.car_icon),
                    80, 80, true
                )
            )
        }
        map.overlays.add(carMarker)

        // Search Marker
        searchMarker = Marker(map).apply {
            title = "Searched Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = BitmapDrawable(
                resources,
                Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(resources, R.drawable.ic_location_pin),
                    80, 80, true
                )
            )
        }
    }

    private fun setupFirebase() {
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.w("Firebase", "Persistence already enabled")
        }

        database = FirebaseDatabase.getInstance().getReference("vehicle/location")
        listenToLocationUpdates()
    }

    private fun listenToLocationUpdates() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("lat").getValue(Double::class.java)
                val lng = snapshot.child("lng").getValue(Double::class.java)

                if (lat != null && lng != null) {
                    val newLocation = GeoPoint(lat, lng)
                    animateMarkerTo(newLocation)
                    calculateAndDisplaySpeed(newLocation)
                } else {
                    Log.w("Firebase", "Invalid location data")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Database error: ${error.message}")
            }
        })
    }

    private fun animateMarkerTo(toPosition: GeoPoint) {
        val fromPosition = carMarker.position ?: toPosition

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val v = animation.animatedFraction
                val lat = v * toPosition.latitude + (1 - v) * fromPosition.latitude
                val lon = v * toPosition.longitude + (1 - v) * fromPosition.longitude
                carMarker.position = GeoPoint(lat, lon)
                map.invalidate()
            }
            start()
        }

        if (isFirstUpdate) {
            map.controller.animateTo(toPosition)
            isFirstUpdate = false
        }
    }

    private fun calculateAndDisplaySpeed(newLocation: GeoPoint) {
        lastLocation?.let { previous ->
            val results = FloatArray(1)
            Location.distanceBetween(
                previous.latitude, previous.longitude,
                newLocation.latitude, newLocation.longitude,
                results
            )

            val distanceMeters = results[0]
            val speedKmh = (distanceMeters / 1.0) * 3.6 // km/h
            speedText.text = "Speed: ${"%.1f".format(speedKmh)} km/h"
        }
        lastLocation = newLocation
    }

    private fun setupSearchFunction() {
        searchButton.setOnClickListener {
            val locationName = searchInput.text.toString().trim()
            if (locationName.isNotEmpty()) {
                searchAndPinLocation(locationName)
            } else {
                Toast.makeText(this, "Enter a location to search", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchAndPinLocation(locationName: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(locationName, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val geoPoint = GeoPoint(address.latitude, address.longitude)

                searchMarker.position = geoPoint
                searchMarker.snippet = address.getAddressLine(0)
                map.overlays.remove(searchMarker)
                map.overlays.add(searchMarker)

                map.controller.animateTo(geoPoint)
                map.invalidate()
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
