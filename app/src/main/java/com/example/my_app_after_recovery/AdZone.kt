package com.example.carvertiseai

data class AdZone(
    val companyName: String = "",
    val location: LocationData = LocationData(),
    val radius: Int = 100,
    val mediaURL: String = ""
)

data class LocationData(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
