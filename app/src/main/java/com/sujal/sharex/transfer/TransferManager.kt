package com.sujal.sharex.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.android.gms.nearby.connection.*
import com.sujal.sharex.database.TransferDatabase
import com.sujal.sharex.database.TransferEntity
import com.sujal.sharex.nearby.NearbyManager
import com.sujal.sharex.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class TransferState(
    val currentFileName: String = "",
    val currentFileProgress: Float = 0f,
    val isTransferring: Boolean = false,
    val isComplete: Boolean = false,
    val pendingTransferInfo: PendingTransferInfo? = null,
    val totalFiles: Int = 0,
    val completedFiles: Int = 0,
    val totalSize: Long = 0L,
    val totalTransferredBytes: Long = 0L
)

data class PendingTransferInfo(
    val totalFiles: Int,
    val totalSize: Long,
    val senderName: String = "Sender"
)

data class FileInfo(val fileName: String, val payloadId: Long, val size: Long = 0)
data class SentFileInfo(val fileName: String, val uri: Uri)

class TransferManager(
    private val context: Context,
    val nearbyManager: NearbyManager,
    private val database: TransferDatabase
) {
    val transferState = MutableStateFlow(TransferState())
    
    private val incomingFilePayloads = mutableMapOf<Long, FileInfo>()
    private val receivedPayloads = mutableMapOf<Long, Payload>()
    private val endpointNames = mutableMapOf<String, String>()
    
    // Sender side queue
    private val pendingSendPayloads = mutableListOf<Payload>()
    private val pendingSendFilesInfo = mutableMapOf<Long, SentFileInfo>()
    private var totalBytesToSend = 0L

    // For progress calculation
    private var completedFilesCount = 0
    private var transferredBytesByPayload = mutableMapOf<Long, Long>()

    val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            endpointNames[endpointId] = info.endpointName
            nearbyManager.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                nearbyManager.connectedEndpoint.value = endpointId
            }
        }
        override fun onDisconnected(endpointId: String) {
            if (nearbyManager.connectedEndpoint.value == endpointId) {
                nearbyManager.connectedEndpoint.value = null
            }
        }
    }

    val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val message = String(payload.asBytes()!!)
                    val json = JSONObject(message)
                    when (json.getString("type")) {
                        "FILE_INFO_LIST" -> {
                            val array = json.getJSONArray("files")
                            var totalSize = 0L
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                val fileName = obj.getString("fileName")
                                val payloadId = obj.getLong("payloadId")
                                val size = obj.optLong("size", 0L)
                                incomingFilePayloads[payloadId] = FileInfo(fileName, payloadId, size)
                                totalSize += size
                            }
                            
                            val pendingInfo = PendingTransferInfo(
                                totalFiles = array.length(),
                                totalSize = totalSize,
                                senderName = endpointNames[endpointId] ?: "Sender"
                            )
                            
                            CoroutineScope(Dispatchers.Main).launch {
                                var autoAccept = false
                                kotlinx.coroutines.withContext(Dispatchers.IO) {
                                    val devices = database.trustedDeviceDao().getAll()
                                    val senderName = endpointNames[endpointId] ?: ""
                                    val trusted = devices.find { it.deviceName == senderName }
                                    if (trusted != null && trusted.autoAccept && !trusted.isBlocked) {
                                        autoAccept = true
                                    }
                                }
                                
                                if (autoAccept) {
                                    transferState.value = transferState.value.copy(
                                        totalFiles = array.length(),
                                        totalSize = totalSize,
                                        completedFiles = 0,
                                        totalTransferredBytes = 0L
                                    )
                                    acceptTransfer(endpointId)
                                } else {
                                    transferState.value = transferState.value.copy(
                                        pendingTransferInfo = pendingInfo,
                                        totalFiles = array.length(),
                                        totalSize = totalSize,
                                        completedFiles = 0,
                                        totalTransferredBytes = 0L
                                    )
                                }
                            }
                        }
                        "TRANSFER_ACCEPT" -> {
                            startFileTransfer(endpointId)
                        }
                        "TRANSFER_REJECT" -> {
                            transferState.value = transferState.value.copy(pendingTransferInfo = null)
                        }
                    }
                }
                Payload.Type.FILE -> {
                    receivedPayloads[payload.id] = payload
                    val info = incomingFilePayloads[payload.id]
                    if (info != null) {
                        transferState.value = transferState.value.copy(
                            currentFileName = info.fileName,
                            isTransferring = true,
                            currentFileProgress = 0f,
                            pendingTransferInfo = null
                        )
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            transferredBytesByPayload[update.payloadId] = update.bytesTransferred
            
            // Calculate overall progress
            var totalTransferred = 0L
            transferredBytesByPayload.values.forEach { totalTransferred += it }

            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress = if (update.totalBytes > 0) {
                        update.bytesTransferred.toFloat() / update.totalBytes.toFloat()
                    } else 0f
                    
                    transferState.value = transferState.value.copy(
                        currentFileProgress = progress,
                        totalTransferredBytes = totalTransferred
                    )
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    completedFilesCount++
                    
                    val payload = receivedPayloads[update.payloadId]
                    if (payload != null && payload.type == Payload.Type.FILE) {
                        // Receiver side save file
                        val javaFile = payload.asFile()?.asJavaFile()
                        val info = incomingFilePayloads[update.payloadId]
                        if (javaFile != null && info != null) {
                            val filePath = FileUtils.copyToDownloads(context, javaFile, info.fileName)
                            CoroutineScope(Dispatchers.IO).launch {
                                database.transferDao().insert(TransferEntity(
                                    fileName = info.fileName,
                                    filePath = filePath ?: "",
                                    isSent = false,
                                    timestamp = System.currentTimeMillis(),
                                    status = "SUCCESS"
                                ))
                            }
                        }
                        receivedPayloads.remove(update.payloadId)
                    } else if (pendingSendFilesInfo.containsKey(update.payloadId)) {
                        // Sender side success
                        val info = pendingSendFilesInfo[update.payloadId]!!
                        CoroutineScope(Dispatchers.IO).launch {
                            database.transferDao().insert(TransferEntity(
                                fileName = info.fileName,
                                filePath = info.uri.toString(),
                                isSent = true,
                                timestamp = System.currentTimeMillis(),
                                status = "SUCCESS"
                            ))
                        }
                    }
                    
                    val isComplete = completedFilesCount == transferState.value.totalFiles
                    transferState.value = transferState.value.copy(
                        completedFiles = completedFilesCount,
                        isComplete = isComplete,
                        isTransferring = !isComplete,
                        totalTransferredBytes = totalTransferred
                    )
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    completedFilesCount++
                    val isComplete = completedFilesCount == transferState.value.totalFiles
                    transferState.value = transferState.value.copy(
                        completedFiles = completedFilesCount,
                        isComplete = isComplete,
                        isTransferring = !isComplete
                    )
                }
            }
        }
    }

    fun prepareFilesToSend(endpointId: String, uris: List<Uri>) {
        pendingSendPayloads.clear()
        pendingSendFilesInfo.clear()
        totalBytesToSend = 0L
        completedFilesCount = 0
        transferredBytesByPayload.clear()

        val jsonArray = JSONArray()

        uris.forEach { uri ->
            var fileName = "Unknown"
            var size = 0L
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) fileName = it.getString(nameIdx)
                    if (sizeIdx != -1) size = it.getLong(sizeIdx)
                }
            }

            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val payload = Payload.fromFile(pfd)
                pendingSendPayloads.add(payload)
                pendingSendFilesInfo[payload.id] = SentFileInfo(fileName, uri)
                totalBytesToSend += size

                val fileObj = JSONObject().apply {
                    put("fileName", fileName)
                    put("payloadId", payload.id)
                    put("size", size)
                }
                jsonArray.put(fileObj)
            }
        }

        val infoJson = JSONObject().apply {
            put("type", "FILE_INFO_LIST")
            put("files", jsonArray)
        }
        
        transferState.value = transferState.value.copy(
            totalFiles = pendingSendPayloads.size,
            totalSize = totalBytesToSend,
            completedFiles = 0,
            totalTransferredBytes = 0L
        )

        nearbyManager.sendPayload(endpointId, Payload.fromBytes(infoJson.toString().toByteArray()))
    }

    fun acceptTransfer(endpointId: String) {
        val acceptJson = JSONObject().apply { put("type", "TRANSFER_ACCEPT") }
        nearbyManager.sendPayload(endpointId, Payload.fromBytes(acceptJson.toString().toByteArray()))
        transferState.value = transferState.value.copy(pendingTransferInfo = null)
        completedFilesCount = 0
        transferredBytesByPayload.clear()
    }

    fun rejectTransfer(endpointId: String) {
        val rejectJson = JSONObject().apply { put("type", "TRANSFER_REJECT") }
        nearbyManager.sendPayload(endpointId, Payload.fromBytes(rejectJson.toString().toByteArray()))
        transferState.value = transferState.value.copy(pendingTransferInfo = null)
    }

    private fun startFileTransfer(endpointId: String) {
        transferState.value = transferState.value.copy(
            isTransferring = true,
            isComplete = false,
            completedFiles = 0
        )
        pendingSendPayloads.forEach { payload ->
            nearbyManager.sendPayload(endpointId, payload)
        }
    }
}
