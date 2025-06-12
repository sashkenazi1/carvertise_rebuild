package com.example.carvertiseai
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.widget.ProgressBar
import android.view.View
import android.widget.ImageView
import android.app.Activity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL
import com.bumptech.glide.Glide
import android.os.Looper
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult





class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationText: TextView
    private lateinit var openMapButton: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var adImage: ImageView


    ///////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // אתחול רכיבי UI
        locationText = findViewById(R.id.locationText)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        openMapButton = findViewById(R.id.openMapButton)
        adImage = findViewById(R.id.adImage)
        val refreshButton = findViewById<Button>(R.id.refreshButton)

        // הצגת placeholder ברירת מחדל
        Glide.with(this)
            .load(R.drawable.placeholder_1)
            .into(adImage)
        adImage.visibility = View.VISIBLE

        // לחצן "הצג במפה"
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

        // אתחול fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // לחצן רענון מיקום
        refreshButton.setOnClickListener {
            getLastLocation()
        }

        // בדיקת הרשאות מיקום - אם יש, מתחילים מיד
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            getLastLocation()  // ✅ קריאה אוטומטית למיקום בעת פתיחה
        }
    }

    ///////////////////////////////////////////////
    // תוצאה של בקשת הרשאה
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            locationText.text = "לא התקבלה הרשאה למיקום"
        }
    }
    ///////////////////////////////////////////////////////
    private fun getLastLocation() {
        loadingSpinner.visibility = View.VISIBLE

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 0
        ).setMaxUpdates(1).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                loadingSpinner.visibility = View.GONE
                val location = locationResult.lastLocation

                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    locationText.text = "Latitude: $lat\nLongitude: $lon"
                    val locationBubble = findViewById<TextView>(R.id.locationBubble)
                    locationBubble.text = "Lat: %.5f\nLon: %.5f".format(lat, lon)

                    Log.d("LOCATION_DEBUG", "Got location: $lat, $lon")

                    fetchAdZonesFromServer(this@MainActivity) { zones ->
                        Log.d("ADZONE_DEBUG", "📦 קיבלנו ${zones.size} אזורים מהשרת")
                        val matchedZone = findMatchingZone(lat, lon, zones)

                        if (matchedZone != null) {
                            Log.d("ADZONE_DEBUG", "✅ נמצא אזור תואם: ${matchedZone.name}, imageUrl = ${matchedZone.imageUrl}")

                            adImage.visibility = View.VISIBLE

                            // ✅ אם יש imageUrl, טוענים את התמונה עם אנימציה חלקה
                            if (matchedZone.imageUrl.isNotEmpty()) {
                                Glide.with(this@MainActivity)
                                    .load(matchedZone.imageUrl)
                                    .placeholder(R.drawable.placeholder_1)
                                    .error(R.drawable.uploadfail)
                                    .into(adImage)

                                adImage.alpha = 0f
                                adImage.animate().alpha(1f).setDuration(1000).start()  // ✅ אנימציה
                            } else {
                                Glide.with(this@MainActivity)
                                    .load(R.drawable.placeholder_1)
                                    .into(adImage)
                                adImage.alpha = 1f
                            }

                            Toast.makeText(this@MainActivity, "📍 באזור: ${matchedZone.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d("ADZONE_DEBUG", "❌ לא נמצא אזור תואם למיקום")
                            adImage.visibility = View.GONE
                        }

                    }

                } else {
                    locationText.text = "⚠️ לא ניתן לקבל מיקום כרגע"
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }





    ///////////////////////////////////////////////
    private fun fetchAdZonesFromServer(context: Context, onResult: (List<AdZone>) -> Unit) {
        Thread {
            try {
                val url = URL("http://shayga.mtacloud.co.il/ad_zones.json") // כתובת ה־JSON שלך
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // הדפסת קוד התגובה של השרת
                Log.d("JSON_FETCH", "🔌 קוד תגובה מהשרת: ${connection.responseCode}")

                val inputStream = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("JSON_FETCH", "📥 JSON שהתקבל מהשרת: $inputStream")

                val gson = Gson()
                val adZoneListType = object : TypeToken<List<AdZone>>() {}.type
                val adZones: List<AdZone> = gson.fromJson(inputStream, adZoneListType)

                (context as Activity).runOnUiThread {
                    onResult(adZones)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("JSON_FETCH", "❌ שגיאה בטעינת JSON: ${e.message}")
            }
        }.start()
    }





}
///////////////////////////////////////
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
            latitude = zone.latitude
            longitude = zone.longitude
        }
        val distance = userLocation.distanceTo(zoneLocation)
        if (distance <= zone.radius) {
            return zone
        }
    }

    return null
}









