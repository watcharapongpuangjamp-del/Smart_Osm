package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ThemeQuickToggleButton
import com.example.ui.theme.*
import com.example.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PersonViewModel
) {
    val ageGroupSummary by viewModel.ageGroupSummary.collectAsStateWithLifecycle()
    val totalPersonsCount by viewModel.totalPersonsCount.collectAsStateWithLifecycle()
    val totalHouseholdsCount by viewModel.totalHouseholdsCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.HealthAndSafety,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "SMART OSM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                            Text(
                                "ระบบสารสนเทศสุขภาพชุมชน",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    ThemeQuickToggleButton(iconTint = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Atmospheric Hero Card with Subsurface Gradient & Live Health-Tech Vibe
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = CardShadowTint,
                            ambientColor = CardShadowTint
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(HeroGradientBrush)
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MintAccent)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "พื้นที่รับผิดชอบ อสม.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "ภาพรวมประชากรในเขต",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Analytics,
                                    contentDescription = null,
                                    tint = MintAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "ข้อมูลสำรวจเชิงรุกพร้อมการวิเคราะห์กลุ่มอายุเพื่อการดูแลสาธารณสุขมูลฐานอย่างทั่วถึง",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 2. Bento Grid Stat Cards with Rich Depth
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        title = "ประชากรทั้งหมด",
                        value = "$totalPersonsCount",
                        unit = "คน",
                        icon = Icons.Filled.Groups,
                        gradientBrush = StatCardGradient1,
                        accentDotColor = MintAccent
                    )
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        title = "ครัวเรือนทั้งหมด",
                        value = "$totalHouseholdsCount",
                        unit = "หลังคาเรือน",
                        icon = Icons.Filled.Home,
                        gradientBrush = StatCardGradient2,
                        accentDotColor = Color(0xFFA5F3FC)
                    )
                }
            }

            // 3. Demographic Age Breakdown (Layered Card with Progress Meters)
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "โครงสร้างประชากรตามช่วงวัย",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ageGroupSummary.values.sum()} รายการ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = CardShadowTint
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (ageGroupSummary.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "ยังไม่มีข้อมูลประชากร",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "เพิ่มบ้านและสมาชิกเพื่อดูสถิติเชิงลึก",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                val total = if (ageGroupSummary.values.sum() > 0) ageGroupSummary.values.sum() else 1
                                ageGroupSummary.forEach { (group, count) ->
                                    val progress = (count.toFloat() / total).coerceIn(0f, 1f)
                                    val percent = (progress * 100).toInt()

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (group.contains("สูงอายุ")) GoldenAmber
                                                            else if (group.contains("เด็ก")) TealLight
                                                            else MaterialTheme.colorScheme.primary
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = group,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "$count คน",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "($percent%)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Subtle progress meter
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progress)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(
                                                        if (group.contains("สูงอายุ")) GoldenAmber
                                                        else if (group.contains("เด็ก")) TealLight
                                                        else MaterialTheme.colorScheme.primary
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }
}

@Composable
fun ModernStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    gradientBrush: Brush,
    accentDotColor: Color
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = CardShadowTint
            )
            .clip(RoundedCornerShape(20.dp))
            .background(gradientBrush)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentDotColor)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

