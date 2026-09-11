package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Villa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast

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
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusVerifiedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("ผลการนำเข้าข้อมูล", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ทั้งหมด: ${importResult!!.totalRows} รายการ", fontWeight = FontWeight.Bold)
                    Row {
                        Text("• สำเร็จ: ", color = OnSurfaceSecondary)
                        Text("${importResult!!.successCount} รายการ", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        Text("• ผิดพลาด: ", color = OnSurfaceSecondary)
                        Text("${importResult!!.failedCount} รายการ", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = HairlineBorder)
                    Text("รายละเอียดจำแนกข้อผิดพลาด:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("- ข้อมูลซ้ำ: ${importResult!!.duplicateCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- เลขบัตร ปชช. ไม่ถูกต้อง: ${importResult!!.invalidNationalIdCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- รูปแบบวันเกิดผิด: ${importResult!!.invalidBirthDateCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- ไม่มีเลขที่บ้าน: ${importResult!!.invalidHouseNoCount}", style = MaterialTheme.typography.bodySmall)
                    Text("- ข้อมูลที่ต้องตรวจสอบ: ${importResult!!.needsReviewCount}", style = MaterialTheme.typography.bodySmall, color = StatusNeedsReviewFg)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearImportResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("รับทราบ")
                }
            }
        )
    }

    if (isImporting) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("กำลังนำเข้าข้อมูล...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("กำลังประมวลผลไฟล์ Excel กรุณารอสักครู่")
                }
            },
            confirmButton = { }
        )
    }

    val filteredList = remember(houseSummary, searchQuery) {
        if (searchQuery.isBlank()) houseSummary
        else houseSummary.filter { it.houseNo.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ทะเบียนครัวเรือน",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "ทั้งหมด ${houseSummary.size} ครัวเรือน",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { exportLauncher.launch("smart_osm_households.xlsx") }
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = "ส่งออก Excel")
                    }
                    IconButton(
                        onClick = { importLauncher.launch("*/*") }
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = "นำเข้า Excel")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHouseClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("เพิ่มครัวเรือน", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Search Bar header span
            item(span = { GridItemSpan(2) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceLight,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ค้นหาด้วยบ้านเลขที่...", color = OnSurfaceTertiary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "ล้างการค้นหา", tint = OnSurfaceTertiary)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Data Sync Banner
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceVariantLight)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MintAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "ผลการค้นหา: ${filteredList.size} หลังคาเรือน" else "ข้อมูลสำรวจล่าสุด",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "แตะเพื่อดูสมาชิก",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariantLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Villa,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = OnSurfaceTertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                if (searchQuery.isBlank()) "ยังไม่มีข้อมูลครัวเรือน" else "ไม่พบข้อมูลบ้านเลขที่ \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfacePrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "กดปุ่ม + ด้านล่างเพื่อเพิ่มข้อมูลใหม่",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.householdId }) { summary ->
                    HouseholdCard(
                        summary = summary,
                        onClick = { onHouseClick(summary.householdId) }
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun HouseholdCard(
    summary: com.example.data.HouseSummary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = CardShadowTint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = EmeraldPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (summary.totalMembers > 0) StatusVerifiedBg else SurfaceVariantLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (summary.totalMembers > 0) StatusVerifiedFg else OnSurfaceTertiary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${summary.totalMembers}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.totalMembers > 0) StatusVerifiedFg else OnSurfaceTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column {
                Text(
                    text = "บ้านเลขที่",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceTertiary
                )
                Text(
                    text = summary.houseNo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurfacePrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${summary.totalMembers} คนในทะเบียน",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceSecondary
                )
            }
        }
    }
}

