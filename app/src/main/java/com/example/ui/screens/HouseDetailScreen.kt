package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Person
import com.example.ui.theme.*
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
            title = { Text("ยืนยันการลบข้อมูล") },
            text = { Text("คุณต้องการลบข้อมูลของ \"${personToDelete?.fullName}\" ออกจากทะเบียนครัวเรือนหรือไม่?") },
            confirmButton = {
                Button(
                    onClick = {
                        personToDelete?.let { viewModel.delete(it) }
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("ลบข้อมูล") }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) { Text("ยกเลิก") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "บ้านเลขที่ ${householdWithPersons?.household?.houseNo ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMemberClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("เพิ่มสมาชิก", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint)
            )
        }
    ) { padding ->
        if (householdWithPersons == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
            return@Scaffold
        }
        
        val houseMembers = householdWithPersons!!.persons
        val household = householdWithPersons!!.household

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Household Summary Hero Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint)
                        .clip(RoundedCornerShape(22.dp))
                        .background(HeroGradientBrush)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "ครัวเรือนในเขตความรับผิดชอบ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    "บ้านเลขที่ ${household.houseNo}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Home, contentDescription = null, tint = MintAccent, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("สมาชิกทั้งหมด", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${houseMembers.size} คน", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("เจ้าบ้าน", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.height(2.dp))
                                val headCount = houseMembers.count { it.houseStatus == com.example.data.HouseholdRole.HEAD }
                                Text("$headCount คน", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MintAccent)
                            }
                        }

                        if (household.latitude != null && household.longitude != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MintAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "พิกัด GPS: ${household.latitude}, ${household.longitude}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "รายชื่อสมาชิกในบ้าน",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfacePrimary
                    )
                    Text(
                        "${houseMembers.size} ท่าน",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceSecondary
                    )
                }
            }

            if (houseMembers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = OnSurfaceTertiary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("ยังไม่มีข้อมูลสมาชิกในบ้านหลังนี้", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("แตะปุ่ม \"เพิ่มสมาชิก\" ด้านล่างเพื่อเริ่มบันทึก", style = MaterialTheme.typography.labelSmall, color = OnSurfaceTertiary)
                        }
                    }
                }
            } else {
                items(houseMembers, key = { it.id }) { person ->
                    val age = viewModel.calculateAge(person.birthDate, person.personStatus)
                    val isHead = person.houseStatus == com.example.data.HouseholdRole.HEAD
                    val isDead = person.personStatus == com.example.data.PersonStatus.DEAD

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isHead) 1.5.dp else 1.dp,
                            color = if (isHead) EmeraldPrimary.copy(alpha = 0.4f) else HairlineBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Badge
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDead) StatusDeadBg
                                        else if (person.gender == com.example.data.Gender.MALE) Color(0xFFE0F2FE)
                                        else Color(0xFFFCE7F3)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (isDead) StatusDeadFg
                                    else if (person.gender == com.example.data.Gender.MALE) Color(0xFF0284C7)
                                    else Color(0xFFDB2777),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            // Details Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = person.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfacePrimary
                                    )
                                    if (isHead) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Filled.Star, contentDescription = "เจ้าบ้าน", tint = GoldenAmber, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "อายุ: ${age ?: "-"} ปี | ปชช: ${person.nationalId ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceSecondary
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModernChip(
                                        text = person.houseStatus.value,
                                        bg = if (isHead) StatusVerifiedBg else SurfaceVariantLight,
                                        fg = if (isHead) StatusVerifiedFg else OnSurfaceSecondary
                                    )
                                    if (isDead) {
                                        ModernChip(text = "เสียชีวิต", bg = StatusDeadBg, fg = StatusDeadFg)
                                    }
                                    if (person.dataStatus == com.example.data.DataStatus.NEEDS_REVIEW) {
                                        ModernChip(text = "รอตรวจสอบ", bg = StatusNeedsReviewBg, fg = StatusNeedsReviewFg)
                                    }
                                }
                            }
                            
                            // Actions
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                IconButton(
                                    onClick = { onHistoryClick(person.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.History, contentDescription = "ประวัติแก้ไข", tint = TealSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onEditMemberClick(person.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "แก้ไข", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { personToDelete = person },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "ลบ", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ModernChip(text: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bg,
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

