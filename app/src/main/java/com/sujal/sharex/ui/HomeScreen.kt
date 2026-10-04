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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF080B14), Color(0xFF111827))
                    )
                )
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ShareX", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("My Device", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { navController.navigate("settings") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Primary Actions
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(
                modifier = Modifier.weight(1f),
                title = "SEND",
                icon = Icons.Default.Send,
                color1 = Color(0xFF00C6FF),
                color2 = Color(0xFF0072FF)
            ) {
                navController.navigate("send")
            }
            ActionCard(
                modifier = Modifier.weight(1f),
                title = "RECEIVE",
                icon = Icons.Default.ArrowDownward,
                color1 = Color(0xFF8E2DE2),
                color2 = Color(0xFF4A00E0)
            ) {
                navController.navigate("receive")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Recent Transfers Section
        Text(
            "Recent Transfers", 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            items(history) { transfer ->
                TransferHistoryItem(transfer)
            }
            if (history.isEmpty()) {
                item {
                    Text("No recent transfers", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ActionCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, color1: Color, color2: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(color1, color2))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(42.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransferHistoryItem(transfer: TransferEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (transfer.isSent) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (transfer.isSent) Color(0xFF00C6FF) else Color(0xFF8E2DE2),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transfer.fileName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White, maxLines = 1)
                val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                Text(sdf.format(Date(transfer.timestamp)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
