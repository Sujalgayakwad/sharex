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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
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
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var totalSelectedSize by remember { mutableStateOf(0L) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        selectedUris = uris
        var size = 0L
        uris.forEach { uri ->
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1) {
                        size += it.getLong(sizeIdx)
                    }
                }
            }
        }
        totalSelectedSize = size
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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Text("Send Files", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(if (selectedUris.isEmpty()) "Select Files" else "Add More Files", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        if (selectedUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
                shape = RoundedCornerShape(16.dp), 
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("${selectedUris.size} files selected", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(formatSize(totalSelectedSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Nearby Receivers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Ensure receiver is waiting on the Receive screen.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (discoveredDevices.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(discoveredDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                            if (selectedUris.isNotEmpty()) {
                                transferManager.prepareFilesToSend(device.endpointId, selectedUris)
                                nearbyManager.requestConnection(device.endpointId, "SenderDevice", transferManager.payloadCallback, transferManager.connectionLifecycleCallback)
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(device.endpointName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
        }
        
        if (transferState.isTransferring || transferState.isComplete) {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        if (transferState.isComplete) "Transfer Complete!" else "Sending... ${transferState.completedFiles}/${transferState.totalFiles} files", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val overallProgress = if (transferState.totalSize > 0) {
                        transferState.totalTransferredBytes.toFloat() / transferState.totalSize.toFloat()
                    } else 0f
                    
                    LinearProgressIndicator(
                        progress = overallProgress, 
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatSize(transferState.totalTransferredBytes) + " / " + formatSize(transferState.totalSize), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(overallProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
    return String.format("%.1f %sB", bytes.toDouble() / (1L shl (z * 10)), " KMGTPE"[z])
}
