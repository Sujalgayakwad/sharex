package com.sujal.sharex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sujal.sharex.database.TransferDatabase
import com.sujal.sharex.database.TrustedDeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(navController: NavController, database: TransferDatabase) {
    val coroutineScope = rememberCoroutineScope()
    var devices by remember { mutableStateOf(emptyList<TrustedDeviceEntity>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            devices = database.trustedDeviceDao().getAll()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(24.dp)
        ) {
            Text("Trusted Devices", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Add Trusted Device Manually", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            items(devices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.deviceName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                            Text(if (device.autoAccept) "Auto-Accept: ON" else "Auto-Accept: OFF", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Switch(
                            checked = device.autoAccept,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    val updated = device.copy(autoAccept = checked)
                                    withContext(Dispatchers.IO) {
                                        database.trustedDeviceDao().update(updated)
                                        devices = database.trustedDeviceDao().getAll()
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    database.trustedDeviceDao().deleteById(device.endpointId)
                                    devices = database.trustedDeviceDao().getAll()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF44336))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var deviceName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Trusted Device", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (deviceName.isNotEmpty()) {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val newId = java.util.UUID.randomUUID().toString()
                                database.trustedDeviceDao().insert(TrustedDeviceEntity(endpointId = newId, deviceName = deviceName, autoAccept = true))
                                devices = database.trustedDeviceDao().getAll()
                            }
                            showAddDialog = false
                        }
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
