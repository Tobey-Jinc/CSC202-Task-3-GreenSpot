package com.tobeygronow.android.greenspot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdate

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.tobeygronow.android.greenspot.databinding.ActivityMapsBinding

/**
 * Displays Google Maps
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }


    /**
     * Update the map when possible
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateUI()
    }

    /**
     * Set the maps location to the Plants latitude and longitude
     */
    private fun updateUI() {
        mMap.clear()

        val title = intent.getStringExtra("title") // Title the location with the Plants name
        val longitude = intent.getDoubleExtra("longitude", 0.0) // Set the latitude
        val latitude = intent.getDoubleExtra("latitude", 0.0) // Set the longitude
        val point = LatLng(latitude, longitude) // Define the exact point
        mMap.addMarker(MarkerOptions().position(point).title(title)) // Create the marker using the point and title

        val zoomLevel: Float = 10f
        val update: CameraUpdate = CameraUpdateFactory.newLatLngZoom(point, zoomLevel) // Create the update

        mMap.animateCamera(update) // Apply the update
    }
}