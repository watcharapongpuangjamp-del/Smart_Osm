package com.example.ui.screens

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PersonViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: PersonViewModel,
    onHouseClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val houseSummary by viewModel.houseSummary.collectAsStateWithLifecycle()

    val firstLocation = houseSummary.firstOrNull { it.latitude != null && it.longitude != null }
    val initialLat = firstLocation?.latitude ?: 13.7563
    val initialLon = firstLocation?.longitude ?: 100.5018

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("แผนที่ครัวเรือน") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
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
