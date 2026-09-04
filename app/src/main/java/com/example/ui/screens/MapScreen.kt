package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PersonViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: PersonViewModel,
    onHouseClick: (String) -> Unit
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
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            cameraPositionState = cameraPositionState
        ) {
            houseSummary.filter { it.latitude != null && it.longitude != null }.forEach { house ->
                MarkerInfoWindowContent(
                    state = MarkerState(position = LatLng(house.latitude!!, house.longitude!!)),
                    title = "บ้านเลขที่ ${house.houseNo}",
                    onInfoWindowClick = {
                        onHouseClick(house.houseNo)
                    }
                ) { marker ->
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("บ้านเลขที่: ${house.houseNo}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("จำนวนสมาชิก: ${house.totalMembers} คน", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "แตะเพื่อดูรายละเอียด >",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
