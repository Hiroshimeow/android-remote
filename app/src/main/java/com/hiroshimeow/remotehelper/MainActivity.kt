package com.hiroshimeow.remotehelper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiroshimeow.remotehelper.input.RemoteAccessibilityService
import com.hiroshimeow.remotehelper.session.SessionManager
import com.hiroshimeow.remotehelper.session.SessionState
import com.hiroshimeow.remotehelper.ui.theme.*

class MainActivity : ComponentActivity() {
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            SessionManager.onScreenConsentGranted()
            val serviceIntent = Intent(this, RemoteSessionService::class.java).apply {
                action = RemoteSessionService.ACTION_START
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("RESULT_DATA", result.data)
            }
            startForegroundService(serviceIntent)
        } else {
            SessionManager.stopSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartSession = {
                            SessionManager.startSession()
                            requestScreenCapture()
                        },
                        onStopSession = { 
                            val serviceIntent = Intent(this, RemoteSessionService::class.java).apply {
                                action = RemoteSessionService.ACTION_STOP
                            }
                            startService(serviceIntent)
                        },
                        ipAddress = getDeviceIpAddress()
                    )
                }
            }
        }
    }

    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun getDeviceIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            var fallbackIp = "0.0.0.0"
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (intf.name.startsWith("tailscale")) {
                            return hostAddress
                        }
                        if (intf.name.startsWith("wlan")) {
                            fallbackIp = hostAddress
                        } else if (fallbackIp == "0.0.0.0") {
                            fallbackIp = hostAddress
                        }
                    }
                }
            }
            return fallbackIp
        } catch (e: Exception) {
            e.printStackTrace()
            return "0.0.0.0"
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    ipAddress: String
) {
    val sessionState by SessionManager.sessionState.collectAsState()
    val isAccessibilityEnabled = RemoteAccessibilityService.instance != null
    val isSessionActive = sessionState.state != SessionState.Idle && sessionState.state != SessionState.Stopped

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .background(Primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                    )
                }
                Column {
                    Text(
                        text = "RemoteHelper",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "FAMILY SUPPORT MVP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isSessionActive) {
                // Active Session Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(ActiveSessionBg)
                        .border(1.dp, ActiveSessionBorder, RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(ActiveSessionTag, CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Session Active", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(DangerColor, CircleShape))
                                Text("LIVE", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatsBox(modifier = Modifier.weight(1f), label = "Stream", value = sessionState.streamProfile.id)
                            StatsBox(modifier = Modifier.weight(1f), label = "FPS", value = "~30")
                            StatsBox(modifier = Modifier.weight(1f), label = "State", value = sessionState.state.name)
                        }
                    }
                }
            }

            // Connection Details Card
            CardBox(title = "Connection Details") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetailRow(
                        icon = {
                            Box(modifier = Modifier.size(16.dp).border(2.dp, TextSecondary, RoundedCornerShape(2.dp)))
                        },
                        title = "Browser URL (Tailscale / LAN)",
                        content = {
                            Text("http://$ipAddress:8080", fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    )
                    
                    if (isSessionActive) {
                        DetailRow(
                            icon = {
                                Box(modifier = Modifier.size(16.dp).border(2.dp, TextSecondary, RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(4.dp).background(TextSecondary, CircleShape))
                                }
                            },
                            title = "Secure Access PIN",
                            content = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    sessionState.pin.forEach { char ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = 32.dp, height = 40.dp)
                                                .background(BgColor, RoundedCornerShape(8.dp))
                                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(char.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // System Status Card
            CardBox(title = "System Status") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusRow(
                        label = "Accessibility Service",
                        status = if (isAccessibilityEnabled) "ENABLED" else "DISABLED",
                        isSuccess = isAccessibilityEnabled
                    )
                    StatusRow(
                        label = "Media Projection",
                        status = if (isSessionActive) "CAPTURING" else "INACTIVE",
                        isSuccess = isSessionActive
                    )
                }
                if (!isAccessibilityEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please enable 'Remote Helper' in Settings > Accessibility.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSessionActive) {
                Button(
                    onClick = onStopSession,
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
                    shape = CircleShape
                ) {
                    Text("STOP SESSION", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Sharing your screen allows your family to control this device. Stop session to end access instantly.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = CircleShape
                ) {
                    Text("START SESSION", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StatsBox(modifier: Modifier = Modifier, label: String, value: String) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), fontSize = 10.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CardBox(title: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun DetailRow(icon: @Composable () -> Unit, title: String, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(InputBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Column {
            Text(title, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            content()
        }
    }
}

@Composable
fun StatusRow(label: String, status: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isSuccess) SuccessColor else DangerColor, CircleShape)
            )
            Text(label, fontSize = 14.sp, color = TextPrimary)
        }
        Text(status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSuccess) SuccessColor else DangerColor)
    }
}
