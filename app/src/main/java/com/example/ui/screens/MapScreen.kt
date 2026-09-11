package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PersonViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: PersonViewModel,
    onHouseClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val houseSummary by viewModel.houseSummary.collectAsStateWithLifecycle()

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val firstLocation = houseSummary.firstOrNull { it.latitude != null && it.longitude != null }
    val initialLat = firstLocation?.latitude ?: 13.7563
    val initialLon = firstLocation?.longitude ?: 100.5018

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "แผนที่พิกัดครัวเรือน",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.example.ui.theme.EmeraldPrimary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (locationPermissionState.status.isGranted) {
                        coroutineScope.launch {
                            try {
                                Toast.makeText(context, "กำลังค้นหาตำแหน่งของคุณ...", Toast.LENGTH_SHORT).show()
                                @SuppressLint("MissingPermission")
                                val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                                    .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                                    .build()
                                val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()
                                if (location != null) {
                                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                                    mapViewRef?.controller?.animateTo(geoPoint)
                                    mapViewRef?.controller?.setZoom(17.0)
                                    Toast.makeText(context, "ย้ายไปยังตำแหน่งปัจจุบันแล้ว", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "ไม่พบตำแหน่งปัจจุบัน", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "เกิดข้อผิดพลาดในการดึงพิกัด: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        locationPermissionState.launchPermissionRequest()
                    }
                },
                containerColor = com.example.ui.theme.EmeraldPrimary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = CircleShape,
                modifier = Modifier.shadow(8.dp, CircleShape, spotColor = com.example.ui.theme.CardShadowTint)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "ตำแหน่งปัจจุบันของฉัน")
            }
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(initialLat, initialLon))
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                mapView.overlays.removeAll { it is Marker }
                houseSummary.filter { it.latitude != null && it.longitude != null }.forEach { house ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(house.latitude!!, house.longitude!!)
                        title = "บ้านเลขที่ ${house.houseNo}"
                        subDescription = "จำนวนสมาชิก: ${house.totalMembers} คน (แตะเพื่อดูรายละเอียด)"
                    }
                    marker.setOnMarkerClickListener { _, _ ->
                        onHouseClick(house.householdId)
                        true
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )
    }
}
