package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.Household
import com.example.viewmodel.PersonViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HouseholdFormScreen(
    viewModel: PersonViewModel,
    householdId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var houseNo by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(householdId) {
        if (householdId != -1L) {
            val household = viewModel.getHouseholdById(householdId)
            household?.let {
                houseNo = it.houseNo
                latitude = it.latitude
                longitude = it.longitude
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (householdId == -1L) "เพิ่มบ้านใหม่" else "แก้ไขบ้าน") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (houseNo.isNotBlank()) {
                        val household = Household(
                            id = if (householdId == -1L) 0 else householdId,
                            houseNo = houseNo,
                            latitude = latitude,
                            longitude = longitude
                        )
                        if (householdId == -1L) {
                            viewModel.insertHousehold(household) { newId ->
                                onNavigateToDetail(newId)
                            }
                        } else {
                            viewModel.updateHousehold(household)
                            onNavigateBack()
                        }
                    } else {
                        Toast.makeText(context, "กรุณากรอกบ้านเลขที่", Toast.LENGTH_SHORT).show()
                    }
                },
                icon = { Icon(Icons.Filled.Save, contentDescription = "บันทึก") },
                text = { Text("บันทึก") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = houseNo,
                onValueChange = { houseNo = it },
                label = { Text("บ้านเลขที่ *") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text("พิกัด GPS", style = MaterialTheme.typography.titleMedium)
            
            if (latitude != null && longitude != null) {
                Text("Lat: $latitude\nLon: $longitude", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("ยังไม่มีพิกัด", style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    if (locationPermissionState.status.isGranted) {
                        coroutineScope.launch {
                            try {
                                @SuppressLint("MissingPermission")
                                val location = fusedLocationClient.lastLocation.await()
                                if (location != null) {
                                    latitude = location.latitude
                                    longitude = location.longitude
                                    Toast.makeText(context, "ดึงพิกัดสำเร็จ", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "ไม่สามารถหาตำแหน่งได้", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        locationPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("อัปเดตพิกัด GPS")
            }
        }
    }
}
