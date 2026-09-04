package com.example.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.example.data.Person
import com.example.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PersonFormScreen(
    viewModel: PersonViewModel,
    personId: Long,
    initialHouseNo: String,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(personId != -1L) }

    var houseNo by remember { mutableStateOf(initialHouseNo) }
    var nationalId by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("ชาย") }
    var birthDate by remember { mutableStateOf(LocalDate.now()) }
    var houseStatus by remember { mutableStateOf("เจ้าบ้าน") }
    var personStatus by remember { mutableStateOf("มีชีวิต") }
    var dataStatus by remember { mutableStateOf("ยืนยันแล้ว") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    LaunchedEffect(personId) {
        if (personId != -1L) {
            val person = viewModel.getPersonById(personId)
            person?.let {
                houseNo = it.houseNo
                nationalId = it.nationalId
                fullName = it.fullName
                gender = it.gender
                birthDate = it.birthDate
                houseStatus = it.houseStatus
                personStatus = it.personStatus
                dataStatus = it.dataStatus
                latitude = it.latitude
                longitude = it.longitude
            }
            isLoading = false
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        birthDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("ตกลง") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("ยกเลิก") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (personId == -1L) "เพิ่มข้อมูลประชากร" else "แก้ไขข้อมูลประชากร") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: General Info
                FormSectionCard(title = "ข้อมูลพื้นฐาน") {
                    OutlinedTextField(
                        value = houseNo,
                        onValueChange = { houseNo = it },
                        label = { Text("บ้านเลขที่") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    var nationalIdError by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = {
                            if (it.length <= 13 && it.all { char -> char.isDigit() }) {
                                nationalId = it
                                nationalIdError = it.length != 13
                            }
                        },
                        label = { Text("เลขบัตรประชาชน (13 หลัก)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nationalIdError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    if (nationalIdError) {
                        Text("กรุณากรอกเลขบัตรประชาชนให้ครบ 13 หลัก", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("ชื่อ-นามสกุล") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownMenuField(
                                label = "เพศ",
                                options = listOf("ชาย", "หญิง"),
                                selectedOption = gender,
                                onOptionSelected = { gender = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = birthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                onValueChange = {},
                                label = { Text("วันเกิด") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Filled.DateRange, contentDescription = "เลือกวันเกิด")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Section 2: Status
                FormSectionCard(title = "รายละเอียดและสถานะ") {
                    DropdownMenuField(
                        label = "สถานะในบ้าน",
                        options = listOf("เจ้าบ้าน", "ผู้อาศัย"),
                        selectedOption = houseStatus,
                        onOptionSelected = { houseStatus = it }
                    )

                    DropdownMenuField(
                        label = "สถานะบุคคล",
                        options = listOf("มีชีวิต", "เสียชีวิต"),
                        selectedOption = personStatus,
                        onOptionSelected = { personStatus = it }
                    )

                    DropdownMenuField(
                        label = "สถานะข้อมูล",
                        options = listOf("ยืนยันแล้ว", "ต้องตรวจสอบ"),
                        selectedOption = dataStatus,
                        onOptionSelected = { dataStatus = it }
                    )
                }

                // Section 3: Location
                FormSectionCard(title = "พิกัดสถานที่ (GPS)") {
                    val context = LocalContext.current
                    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
                    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

                    Text(
                        text = "ละติจูด: ${latitude ?: "-"} \nลองจิจูด: ${longitude ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                        loc?.let {
                                            latitude = it.latitude
                                            longitude = it.longitude
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    // Handle exception
                                }
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ดึงพิกัดตำแหน่งปัจจุบัน")
                    }
                }

                Button(
                    onClick = {
                        if (nationalId.length == 13 && houseNo.isNotBlank() && fullName.isNotBlank()) {
                            val person = Person(
                                id = if (personId == -1L) 0 else personId,
                                houseNo = houseNo,
                                nationalId = nationalId,
                                fullName = fullName,
                                gender = gender,
                                birthDate = birthDate,
                                houseStatus = houseStatus,
                                personStatus = personStatus,
                                dataStatus = dataStatus,
                                latitude = latitude,
                                longitude = longitude
                            )
                            coroutineScope.launch {
                                if (personId == -1L) {
                                    viewModel.insert(person)
                                } else {
                                    viewModel.update(person)
                                }
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = nationalId.length == 13 && houseNo.isNotBlank() && fullName.isNotBlank()
                ) {
                    Text("บันทึกข้อมูล", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FormSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
