package com.example.carvertiseai

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import com.google.firebase.FirebaseApp
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationText: TextView
    private lateinit var locationBubble: TextView
    private lateinit var openMapButton: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var adImage: ImageView
    private val database: FirebaseDatabase by lazy { Firebase.database }

    private var lastLat = 0.0
    private var lastLon = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // אתחול רכיבים
        locationText = findViewById(R.id.locationText)
        locationBubble = findViewById(R.id.locationBubble)
        openMapButton = findViewById(R.id.openMapButton)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        adImage = findViewById(R.id.adImage)

        Glide.with(this).load(R.drawable.placeholder_1).into(adImage)

        openMapButton.setOnClickListener {
            val latLng = locationText.text.toString()
            val latLon = latLng.split("\n")
            if (latLon.size == 2) {
                val lat = latLon[0].substringAfter(": ").toDoubleOrNull()
                val lon = latLon[1].substringAfter(": ").toDoubleOrNull()
                if (lat != null && lon != null) {
                    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            startLiveLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLiveLocationUpdates()
        } else {
            locationText.text = "❌ לא התקבלה הרשאה למיקום"
        }
    }

    private fun startLiveLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000)
            .setMinUpdateIntervalMillis(8000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                val lat = location.latitude
                val lon = location.longitude

                locationText.text = "Latitude: $lat\nLongitude: $lon"
                locationBubble.text = "Lat: %.5f\nLon: %.5f".format(lat, lon)

                val previousLocation = Location("").apply {
                    latitude = lastLat
                    longitude = lastLon
                }
                val currentLocation = Location("").apply {
                    latitude = lat
                    longitude = lon
                }
                val distance = previousLocation.distanceTo(currentLocation)

                if (distance > 30) {
                    lastLat = lat
                    lastLon = lon
                    fetchAdZonesFromFirebase(lat, lon)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun fetchAdZonesFromFirebase(currentLat: Double, currentLon: Double) {
        val ref = database.getReference("contacts")
        ref.get().addOnSuccessListener { snapshot ->
            val zones = mutableListOf<AdZone>()
            snapshot.children.forEach { child ->
                val zone = child.getValue(AdZone::class.java)
                if (zone != null) {
                    zones.add(zone)
                }
            }

            val matchedZone = findMatchingZone(currentLat, currentLon, zones)
            if (matchedZone != null) {
                Glide.with(this)
                    .load(matchedZone.mediaURL)
                    .placeholder(R.drawable.placeholder_1)
                    .error(R.drawable.uploadfail)
                    .into(adImage)

                adImage.alpha = 0f
                adImage.visibility = View.VISIBLE
                adImage.animate().alpha(1f).setDuration(1000).start()

                Toast.makeText(this, "📍 באזור: ${matchedZone.companyName}", Toast.LENGTH_SHORT).show()
            } else {
                Glide.with(this)
                    .load(R.drawable.default_ad)
                    .into(adImage)

                adImage.alpha = 0f
                adImage.visibility = View.VISIBLE
                adImage.animate().alpha(1f).setDuration(1000).start()

                Toast.makeText(this, "📭 אין פרסומת באזור – מציג ברירת מחדל", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "❌ שגיאה בטעינת המידע מהשרת", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findMatchingZone(
        currentLat: Double,
        currentLon: Double,
        zones: List<AdZone>
    ): AdZone? {
        val userLocation = Location("").apply {
            latitude = currentLat
            longitude = currentLon
        }
        for (zone in zones) {
            val zoneLocation = Location("").apply {
                latitude = zone.location.lat
                longitude = zone.location.lng
            }
            val distance = userLocation.distanceTo(zoneLocation)
            if (distance <= zone.radius) {
                return zone
            }
        }
        return null
    }
}
