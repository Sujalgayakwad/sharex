package com.sujal.sharex.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow

data class DiscoveredDevice(val endpointId: String, val endpointName: String)

class NearbyManager(private val context: Context) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val serviceId = "com.sujal.sharex.SERVICE_ID"

    val discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val connectedEndpoint = MutableStateFlow<String?>(null)
    
    var payloadCallback: PayloadCallback? = null
    var connectionLifecycleCallback: ConnectionLifecycleCallback? = null

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val list = discoveredDevices.value.toMutableList()
            list.add(DiscoveredDevice(endpointId, info.endpointName))
            discoveredDevices.value = list
        }

        override fun onEndpointLost(endpointId: String) {
            val list = discoveredDevices.value.toMutableList()
            list.removeAll { it.endpointId == endpointId }
            discoveredDevices.value = list
        }
    }

    fun startAdvertising(deviceName: String, pCallback: PayloadCallback, cCallback: ConnectionLifecycleCallback) {
        payloadCallback = pCallback
        connectionLifecycleCallback = cCallback
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(deviceName, serviceId, cCallback, options)
            .addOnSuccessListener { Log.d("Nearby", "Advertising started") }
            .addOnFailureListener { Log.e("Nearby", "Advertising failed", it) }
    }

    fun startDiscovery() {
        discoveredDevices.value = emptyList()
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d("Nearby", "Discovery started") }
            .addOnFailureListener { Log.e("Nearby", "Discovery failed", it) }
    }
    
    fun requestConnection(endpointId: String, myName: String, pCallback: PayloadCallback, cCallback: ConnectionLifecycleCallback) {
        payloadCallback = pCallback
        connectionLifecycleCallback = cCallback
        connectionsClient.requestConnection(myName, endpointId, cCallback)
            .addOnSuccessListener { Log.d("Nearby", "Connection requested") }
            .addOnFailureListener { Log.e("Nearby", "Connection request failed", it) }
    }

    fun acceptConnection(endpointId: String, pCallback: PayloadCallback) {
        connectionsClient.acceptConnection(endpointId, pCallback)
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoint.value = null
    }

    fun sendPayload(endpointId: String, payload: Payload) {
        connectionsClient.sendPayload(endpointId, payload)
    }
}
