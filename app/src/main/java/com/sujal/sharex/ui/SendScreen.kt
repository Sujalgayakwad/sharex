package com.sujal.sharex.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sujal.sharex.nearby.NearbyManager
import com.sujal.sharex.transfer.TransferManager
import com.sujal.sharex.utils.PermissionUtils

@Composable
fun SendScreen(navController: NavController, transferManager: TransferManager, nearbyManager: NearbyManager) {
    val context = LocalContext.current
    val discoveredDevices by nearbyManager.discoveredDevices.collectAsState()
    val transferState by transferManager.transferState.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    selectedFileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        nearbyManager.startDiscovery()
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(PermissionUtils.getRequiredPermissions())
    }
    
    DisposableEffect(Unit) {
        onDispose { nearbyManager.stopAll() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA)).padding(24.dp)) {
        Text("Send Files", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0072FF)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Select File", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        if (selectedFileName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Ready: $selectedFileName", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Nearby Receivers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Text("Ensure receiver is waiting on the Receive screen.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (discoveredDevices.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0072FF))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(discoveredDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                            if (selectedUri != null) {
                                transferManager.prepareFileToSend(device.endpointId, selectedUri!!, selectedFileName)
                                nearbyManager.requestConnection(device.endpointId, "SenderDevice", transferManager.payloadCallback, transferManager.connectionLifecycleCallback)
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(56.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(device.endpointName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF333333))
                        }
                    }
                }
            }
        }
        
        if (transferState.isTransferring || transferState.isComplete) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Sending ${transferState.fileName}", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = transferState.progress, 
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(0xFF0072FF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (transferState.isComplete) "Complete!" else "${(transferState.progress * 100).toInt()}%", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
