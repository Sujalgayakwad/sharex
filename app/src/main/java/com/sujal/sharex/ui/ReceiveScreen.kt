package com.sujal.sharex.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sujal.sharex.nearby.NearbyManager
import com.sujal.sharex.transfer.TransferManager
import com.sujal.sharex.utils.PermissionUtils

@Composable
fun ReceiveScreen(navController: NavController, transferManager: TransferManager, nearbyManager: NearbyManager) {
    val transferState by transferManager.transferState.collectAsState()
    val connectedEndpoint by nearbyManager.connectedEndpoint.collectAsState()
    
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        nearbyManager.startAdvertising("ReceiverDevice", transferManager.payloadCallback, transferManager.connectionLifecycleCallback)
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(PermissionUtils.getRequiredPermissions())
    }
    
    DisposableEffect(Unit) {
        onDispose { nearbyManager.stopAll() }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Receive Files", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Waiting for sender to connect...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Radar / Discovery Animation UI
        Box(modifier = Modifier.size(200.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(140.dp).background(MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(100.dp), strokeWidth = 2.dp)
                 Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (connectedEndpoint != null && transferState.pendingTransferInfo == null && !transferState.isTransferring && !transferState.isComplete) {
            Text("Sender Connected!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        
        if (transferState.pendingTransferInfo != null) {
            Card(
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), 
                shape = RoundedCornerShape(24.dp), 
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Incoming Transfer", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("${transferState.pendingTransferInfo!!.totalFiles} files", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(formatSize(transferState.pendingTransferInfo!!.totalSize), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { transferManager.rejectTransfer(connectedEndpoint!!) }, 
                            modifier = Modifier.height(50.dp).weight(1f), 
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                        ) { 
                            Text("Decline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) 
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { transferManager.acceptTransfer(connectedEndpoint!!) }, 
                            modifier = Modifier.height(50.dp).weight(1f), 
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { 
                            Text("Accept", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) 
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
                        if (transferState.isComplete) "Successfully saved to Downloads" else "Receiving... ${transferState.completedFiles}/${transferState.totalFiles} files", 
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
