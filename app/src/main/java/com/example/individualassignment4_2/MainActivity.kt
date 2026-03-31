package com.example.individualassignment4_2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.individualassignment4_2.ui.theme.IndividualAssignment42Theme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.location.LocationRequest

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var currentLocation by mutableStateOf<Location?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply{
            setMinUpdateIntervalMillis(5000)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    currentLocation = it
                }
            }
        }
        enableEdgeToEdge()

        setContent {
            var permission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }
                IndividualAssignment42Theme {
                    if (!permission) {
                        RequestPermissions(fusedLocationClient, locationRequest, locationCallback)
                    }
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            currentLocation?.let {
                                MainScreen(
                                    location = it,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } ?: Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Getting location...")
                            }
                        }
                    }
        }

    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Composable
fun MainScreen(modifier: Modifier, location: Location) {

    val context = LocalContext.current
    var addressText by remember { mutableStateOf("Loading address...") }

    LaunchedEffect(location) {
        val geocoder = android.location.Geocoder(context)
        val addresses = geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1
        )

        if (!addresses.isNullOrEmpty()) {
            addressText = addresses[0].getAddressLine(0)
        }
    }

    val userLatLng = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
    }

    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.SATELLITE))
    }

    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = false))
    }

    Box {
//        GoogleMap(
//            modifier = Modifier.fillMaxSize(),
//            cameraPositionState = cameraPositionState,
//            properties = properties,
//            uiSettings = uiSettings
//        ) {
//            Marker(
//                state = MarkerState(position = userLatLng),
//                title = "My Location",
////                snippet = "Bald Head Island Marina"
//            )
//        }

        var markers by remember { mutableStateOf(listOf(userLatLng)) }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                markers = markers + latLng
            }
        ){
            Marker(
                state = MarkerState(position = userLatLng),
                title = "Current Location",
                snippet = addressText
            )
            markers.forEach {
                Marker(state = MarkerState(position = it))
            }
        }

        Text(
            text = addressText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp),
            textAlign = TextAlign.Center,
            color = Color.White
        )

//        var checkedState by remember { mutableStateOf(true) }

//        Switch(
//            modifier = Modifier
//                .align(Alignment.TopCenter),
//            checked = checkedState,
//            onCheckedChange = {
//                checkedState = it
//                if (it) {
//                    cameraPositionState.move(CameraUpdateFactory.zoomIn())
//                } else {
//                    cameraPositionState.move(CameraUpdateFactory.zoomOut())
//                }
//            }
//        )

        /*
        Switch(
            modifier = Modifier
                .align(Alignment.TopCenter),
            checked = uiSettings.compassEnabled,
            onCheckedChange = {
                uiSettings = uiSettings.copy(compassEnabled = it)
                properties = if (it) {
                    properties.copy(mapType = MapType.TERRAIN)
                } else {
                    properties.copy(mapType = MapType.HYBRID)
                }
            }
        )
    */
    }

}

@Composable
fun RequestPermissions(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: com.google.android.gms.location.LocationRequest,
    locationCallback: LocationCallback
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun MapPreview() {
//    IndividualAssignment42Theme {
////        Greeting("Android")
//        MainScreen(modifier = Modifier, location)
//    }
//}