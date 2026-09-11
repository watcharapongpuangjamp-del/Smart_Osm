package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfoScreen() {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ข้อมูลผู้พัฒนา (อสม.)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Crest / Official Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(12.dp, CircleShape, spotColor = CardShadowTint)
                    .clip(CircleShape)
                    .background(HeroGradientBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.HealthAndSafety,
                    contentDescription = null,
                    tint = MintAccent,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "วัชรพงษ์ พวงแจ่ม",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurfacePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = "ได้รับการรับรอง",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = StatusVerifiedBg
            ) {
                Text(
                    text = "อาสาสมัครสาธารณสุขประจำหมู่บ้าน (อสม.)",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusVerifiedFg,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Contact & Organization Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    InfoRow(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            }
                        },
                        title = "พื้นที่รับผิดชอบ",
                        text = "หมู่ 8 ตำบลป่าขะ อำเภอบ้านนา จังหวัดนครนายก"
                    )

                    Divider(color = HairlineBorder)

                    InfoRow(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = TealSecondary, modifier = Modifier.size(20.dp))
                            }
                        },
                        title = "หน่วยบริการปฐมภูมิ",
                        text = "รพ.สต.บ้านกร่างประดู่วัง"
                    )

                    Divider(color = HairlineBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0991546800"))
                                context.startActivity(dialIntent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusVerifiedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PhoneInTalk, contentDescription = null, tint = StatusVerifiedFg, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("เบอร์โทรศัพท์ติดต่อ (แตะเพื่อโทร)", style = MaterialTheme.typography.labelSmall, color = OnSurfaceTertiary)
                            Text(
                                text = "099-154-6800",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceVariantLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "สังกัด กระทรวงสาธารณสุข ประเทศไทย",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
            ) {
                Text(
                    text = "“ทุกปัญหาสุขภาพ เราพร้อมรับฟังและช่วยเหลือเคียงข้างชุมชน”",
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: @Composable () -> Unit,
    title: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = OnSurfaceTertiary)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfacePrimary
            )
        }
    }
}

