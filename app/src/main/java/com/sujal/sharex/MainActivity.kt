package com.sujal.sharex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.sujal.sharex.ui.theme.ShareXTheme

import androidx.compose.ui.Modifier
import com.sujal.sharex.nearby.NearbyManager
import com.sujal.sharex.transfer.TransferManager
import com.sujal.sharex.ui.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ShareXApplication
        val nearbyManager = NearbyManager(this)
        val transferManager = TransferManager(this, nearbyManager, app.database)

        setContent {
            ShareXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(transferManager, nearbyManager, app.database)
                }
            }
        }
    }
}
