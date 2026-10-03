package com.sujal.sharex.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sujal.sharex.database.TransferDatabase
import com.sujal.sharex.database.TransferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(navController: NavController, database: TransferDatabase) {
    var history by remember { mutableStateOf(emptyList<TransferEntity>()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            history = database.transferDao().getAll()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ShareX", style = MaterialTheme.typography.headlineLarge)
        Text("Fast. Private. Local.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Button(onClick = { navController.navigate("send") }, modifier = Modifier.weight(1f)) {
                Text("Send")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { navController.navigate("receive") }, modifier = Modifier.weight(1f)) {
                Text("Receive")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Recent Transfers", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(history) { transfer ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(transfer.fileName, style = MaterialTheme.typography.bodyLarge)
                        Text(if (transfer.isSent) "Sent" else "Received", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
