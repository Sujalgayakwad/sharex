package com.sujal.sharex.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Send File", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { launcher.launch("*/*") }) {
            Text("Select File")
        }
        if (selectedFileName.isNotEmpty()) {
            Text("Selected: $selectedFileName")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nearby Devices (Searching...):")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(discoveredDevices) { device ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    if (selectedUri != null) {
                        transferManager.prepareFileToSend(device.endpointId, selectedUri!!, selectedFileName)
                        nearbyManager.requestConnection(device.endpointId, "SenderDevice", transferManager.payloadCallback, transferManager.connectionLifecycleCallback)
                    }
                }) {
                    Text(device.endpointName, modifier = Modifier.padding(16.dp))
                }
            }
        }
        
        if (transferState.isTransferring || transferState.isComplete) {
            Text("Transfer Status:")
            LinearProgressIndicator(progress = transferState.progress, modifier = Modifier.fillMaxWidth())
            Text(if (transferState.isComplete) "Complete!" else "${(transferState.progress * 100).toInt()}%")
        }
    }
}
