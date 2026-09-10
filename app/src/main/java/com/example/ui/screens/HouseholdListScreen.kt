package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PersonViewModel

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdListScreen(
    viewModel: PersonViewModel,
    onHouseClick: (Long) -> Unit,
    onAddHouseClick: () -> Unit
) {
    val houseSummary by viewModel.houseSummary.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcelData(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportExcelData(context, it) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (importResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = { Text("รายงานการนำเข้าข้อมูล") },
            text = {
                Column {
                    Text("ทั้งหมด: ${importResult!!.totalRows} รายการ")
                    Text("สำเร็จ: ${importResult!!.successCount} รายการ", color = MaterialTheme.colorScheme.primary)
                    Text("ผิดพลาด: ${importResult!!.failedCount} รายการ", color = MaterialTheme.colorScheme.error)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("รายละเอียดข้อผิดพลาด:", fontWeight = FontWeight.Bold)
                    Text("- ข้อมูลซ้ำ: ${importResult!!.duplicateCount}")
                    Text("- เลขบัตร ปชช. ผิด: ${importResult!!.invalidNationalIdCount}")
                    Text("- วันเกิดผิดรูปแบบ: ${importResult!!.invalidBirthDateCount}")
                    Text("- ไม่มีบ้านเลขที่: ${importResult!!.invalidHouseNoCount}")
                    Text("- ข้อมูลต้องตรวจสอบ: ${importResult!!.needsReviewCount}", color = MaterialTheme.colorScheme.secondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportResult() }) {
                    Text("ตกลง")
                }
            }
        )
    }

    if (isImporting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("กำลังนำเข้าข้อมูล") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("กรุณารอสักครู่...")
                }
            },
            confirmButton = { }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ทะเบียนครัวเรือน") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { exportLauncher.launch("population_report.xlsx") }) {
                        Icon(Icons.Filled.Download, contentDescription = "ส่งออกข้อมูล Excel")
                    }
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "นำเข้าข้อมูลจาก Excel")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHouseClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = "เพิ่มข้อมูลใหม่") },
                text = { Text("เพิ่มข้อมูลบ้าน/บุคคล") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { padding ->
        if (houseSummary.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ยังไม่มีข้อมูลครัวเรือน", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                items(houseSummary, key = { it.householdId }) { summary ->
                    Card(
                        onClick = { onHouseClick(summary.householdId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "บ้านเลขที่ ${summary.houseNo}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${summary.totalMembers} คน",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
