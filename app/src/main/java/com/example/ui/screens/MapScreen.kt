package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PersonViewModel
import com.example.data.HouseSummary
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.clustering.Clustering

class HouseholdClusterItem(
    val house: HouseSummary
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(house.latitude!!, house.longitude!!)
    override fun getTitle(): String = "บ้านเลขที่ ${house.houseNo}"
    override fun getSnippet(): String = "จำนวนสมาชิก: ${house.totalMembers} คน"
    override fun getZIndex(): Float? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: PersonViewModel,
    onHouseClick: (Long) -> Unit
) {
    val houseSummary by viewModel.houseSummary.collectAsStateWithLifecycle()

    val firstLocation = houseSummary.firstOrNull { it.latitude != null && it.longitude != null }
    val initialPosition = if (firstLocation != null) {
        LatLng(firstLocation.latitude!!, firstLocation.longitude!!)
    } else {
        LatLng(13.7563, 100.5018) // Bangkok
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

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
        val items = remember(houseSummary) {
            houseSummary
                .filter { it.latitude != null && it.longitude != null }
                .map { HouseholdClusterItem(it) }
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            cameraPositionState = cameraPositionState
        ) {
            Clustering(
                items = items,
                onClusterItemClick = { item ->
                    false // Return false to show info window
                },
                onClusterItemInfoWindowClick = { item ->
                    onHouseClick(item.house.householdId)
                }
            )
        }
    }
}
