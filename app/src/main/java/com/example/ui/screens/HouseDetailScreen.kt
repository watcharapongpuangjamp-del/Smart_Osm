package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Person
import com.example.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailScreen(
    viewModel: PersonViewModel,
    householdId: Long,
    onNavigateBack: () -> Unit,
    onAddMemberClick: () -> Unit,
    onEditMemberClick: (Long) -> Unit,
    onHistoryClick: (Long) -> Unit
) {
    val householdWithPersons by viewModel.getHouseholdWithPersonsById(householdId).collectAsStateWithLifecycle(initialValue = null)
    
    var personToDelete by remember { mutableStateOf<Person?>(null) }

    if (personToDelete != null) {
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            title = { Text("ยืนยันการลบ") },
            text = { Text("คุณต้องการลบข้อมูลของ ${personToDelete?.fullName} หรือไม่?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        personToDelete?.let { viewModel.delete(it) }
                        personToDelete = null
                    }
                ) { Text("ลบ", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("ยกเลิก") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("รายละเอียดบ้านเลขที่ ${householdWithPersons?.household?.houseNo ?: ""}") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMemberClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "เพิ่มสมาชิก")
            }
        }
    ) { padding ->
        if (householdWithPersons == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        
        val houseMembers = householdWithPersons!!.persons
        val household = householdWithPersons!!.household

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("สมาชิกทั้งหมด", style = MaterialTheme.typography.labelLarge)
                            Text("${houseMembers.size} คน", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("เจ้าบ้าน", style = MaterialTheme.typography.labelLarge)
                            Text("${houseMembers.count { it.houseStatus == com.example.data.HouseholdRole.HEAD }} คน", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                if (household.latitude != null && household.longitude != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("พิกัด GPS: ${household.latitude}, ${household.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("รายชื่อสมาชิกครัวเรือน", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(houseMembers, key = { it.id }) { person ->
                val age = viewModel.calculateAge(person.birthDate, person.personStatus)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (person.gender == com.example.data.Gender.MALE) Color(0xFFBBDEFB) else Color(0xFFF8BBD0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = if (person.gender == com.example.data.Gender.MALE) Color(0xFF1976D2) else Color(0xFFC2185B))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = person.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "อายุ: ${age ?: "-"} ปี | บัตร: ${person.nationalId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(
                                    text = person.houseStatus.value, 
                                    backgroundColor = if (person.houseStatus == com.example.data.HouseholdRole.HEAD) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = if (person.houseStatus == com.example.data.HouseholdRole.HEAD) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (person.personStatus == com.example.data.PersonStatus.DEAD) {
                                    StatusChip(
                                        text = "เสียชีวิต", 
                                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                                        textColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        
                        // Actions
                        Column {
                            IconButton(onClick = { onHistoryClick(person.id) }) {
                                Icon(Icons.Filled.Info, contentDescription = "ประวัติ", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { onEditMemberClick(person.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "แก้ไข", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { personToDelete = person }) {
                                Icon(Icons.Filled.Delete, contentDescription = "ลบ", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(72.dp)) } // Space for FAB
        }
    }
}

@Composable
fun StatusChip(text: String, backgroundColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
