package com.sujal.sharex.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Receive File", style = MaterialTheme.typography.headlineMedium)
        Text("Visible to nearby devices as ReceiverDevice")
        Spacer(modifier = Modifier.height(16.dp))
        
        if (connectedEndpoint != null) {
            Text("Connected!")
        }
        
        if (transferState.pendingFileInfo != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Incoming File: ${transferState.pendingFileInfo!!.fileName}")
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { transferManager.rejectTransfer(connectedEndpoint!!) }) { Text("Reject") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { transferManager.acceptTransfer(connectedEndpoint!!) }) { Text("Accept") }
                    }
                }
            }
        }
        
        if (transferState.isTransferring || transferState.isComplete) {
            Text("Receiving: ${transferState.fileName}")
            LinearProgressIndicator(progress = transferState.progress, modifier = Modifier.fillMaxWidth())
            Text(if (transferState.isComplete) "Complete!" else "${(transferState.progress * 100).toInt()}%")
        }
    }
}
