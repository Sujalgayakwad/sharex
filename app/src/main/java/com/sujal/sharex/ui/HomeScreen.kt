package com.sujal.sharex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sujal.sharex.database.TransferDatabase
import com.sujal.sharex.database.TransferEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(navController: NavController, database: TransferDatabase) {
    var history by remember { mutableStateOf(emptyList<TransferEntity>()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            history = database.transferDao().getAll()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6FA))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 64.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Text("ShareX", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text("Fast. Private. Local.", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActionCard("Send", Icons.Default.Send, Color(0xFF00C6FF), Color(0xFF0072FF)) {
                        navController.navigate("send")
                    }
                    ActionCard("Receive", Icons.Default.Check, Color(0xFFFDC830), Color(0xFFF37335)) {
                        navController.navigate("receive")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Recent Transfers", 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color(0xFF333333)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            items(history) { transfer ->
                TransferHistoryItem(transfer)
            }
            if (history.isEmpty()) {
                item {
                    Text("No recent transfers", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color1: Color, color2: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .size(150.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(color1, color2))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransferHistoryItem(transfer: TransferEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(if (transfer.isSent) Color(0xFFE3F2FD) else Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (transfer.isSent) Icons.Default.Share else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (transfer.isSent) Color(0xFF1976D2) else Color(0xFF388E3C),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.fileName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333))
                val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                Text(sdf.format(Date(transfer.timestamp)), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
