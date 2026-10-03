package com.sujal.sharex.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Receive Files", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Waiting for sender to connect...", color = Color.Gray, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Box(modifier = Modifier.size(200.dp).background(Color(0xFFE8F5E9), CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(140.dp).background(Color(0xFF4CAF50), CircleShape), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Color.White, modifier = Modifier.size(80.dp), strokeWidth = 6.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (connectedEndpoint != null && transferState.pendingFileInfo == null && !transferState.isTransferring && !transferState.isComplete) {
            Text("Sender Connected!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        
        if (transferState.pendingFileInfo != null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Incoming File", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(transferState.pendingFileInfo!!.fileName, color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { transferManager.rejectTransfer(connectedEndpoint!!) }, modifier = Modifier.height(50.dp).weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { 
                            Text("Reject", fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { transferManager.acceptTransfer(connectedEndpoint!!) }, modifier = Modifier.height(50.dp).weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { 
                            Text("Accept", fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                        }
                    }
                }
            }
        }
        
        if (transferState.isTransferring || transferState.isComplete) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Receiving ${transferState.fileName}", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = transferState.progress, 
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (transferState.isComplete) "Successfully saved to Downloads/ShareX" else "${(transferState.progress * 100).toInt()}%", color = if (transferState.isComplete) Color(0xFF4CAF50) else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
