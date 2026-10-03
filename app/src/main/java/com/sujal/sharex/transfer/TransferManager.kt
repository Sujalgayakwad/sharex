package com.sujal.sharex.transfer

import android.content.Context
import android.net.Uri
import com.google.android.gms.nearby.connection.*
import com.sujal.sharex.database.TransferDatabase
import com.sujal.sharex.database.TransferEntity
import com.sujal.sharex.nearby.NearbyManager
import com.sujal.sharex.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class TransferState(
    val fileName: String = "",
    val progress: Float = 0f,
    val isTransferring: Boolean = false,
    val isComplete: Boolean = false,
    val pendingFileInfo: FileInfo? = null
)

data class FileInfo(val fileName: String, val payloadId: Long)

class TransferManager(
    private val context: Context,
    val nearbyManager: NearbyManager,
    private val database: TransferDatabase
) {
    val transferState = MutableStateFlow(TransferState())
    
    private val incomingFilePayloads = mutableMapOf<Long, FileInfo>()
    private val receivedPayloads = mutableMapOf<Long, Payload>()
    
    private var pendingFilePayload: Payload? = null
    private var pendingFileName: String = ""
    private var pendingUriToSend: Uri? = null

    val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
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
                        "FILE_INFO" -> {
                            val fileName = json.getString("fileName")
                            val payloadId = json.getLong("payloadId")
                            incomingFilePayloads[payloadId] = FileInfo(fileName, payloadId)
                            transferState.value = transferState.value.copy(
                                pendingFileInfo = FileInfo(fileName, payloadId)
                            )
                        }
                        "TRANSFER_ACCEPT" -> {
                            startFileTransfer(endpointId)
                        }
                        "TRANSFER_REJECT" -> {
                            transferState.value = transferState.value.copy(pendingFileInfo = null)
                        }
                    }
                }
                Payload.Type.FILE -> {
                    receivedPayloads[payload.id] = payload
                    val info = incomingFilePayloads[payload.id]
                    if (info != null) {
                        transferState.value = transferState.value.copy(
                            fileName = info.fileName,
                            isTransferring = true,
                            progress = 0f,
                            pendingFileInfo = null
                        )
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress = if (update.totalBytes > 0) {
                        update.bytesTransferred.toFloat() / update.totalBytes.toFloat()
                    } else 0f
                    transferState.value = transferState.value.copy(progress = progress)
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    transferState.value = transferState.value.copy(
                        isTransferring = false,
                        isComplete = true,
                        progress = 1f
                    )
                    
                    val payload = receivedPayloads[update.payloadId]
                    if (payload != null && payload.type == Payload.Type.FILE) {
                        val javaFile = payload.asFile()?.asJavaFile()
                        val info = incomingFilePayloads[update.payloadId]
                        if (javaFile != null && info != null) {
                            val filePath = FileUtils.copyToDownloads(context, javaFile, info.fileName)
                            CoroutineScope(Dispatchers.IO).launch {
                                database.transferDao().insert(TransferEntity(
                                    fileName = info.fileName,
                                    filePath = filePath,
                                    isSent = false,
                                    timestamp = System.currentTimeMillis(),
                                    status = "SUCCESS"
                                ))
                            }
                        }
                        receivedPayloads.remove(update.payloadId)
                        incomingFilePayloads.remove(update.payloadId)
                    } else {
                        if (update.payloadId == pendingFilePayload?.id) {
                            CoroutineScope(Dispatchers.IO).launch {
                                database.transferDao().insert(TransferEntity(
                                    fileName = pendingFileName,
                                    filePath = pendingUriToSend?.toString() ?: "",
                                    isSent = true,
                                    timestamp = System.currentTimeMillis(),
                                    status = "SUCCESS"
                                ))
                            }
                            pendingFilePayload = null
                        }
                    }
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    transferState.value = transferState.value.copy(isTransferring = false)
                }
            }
        }
    }

    fun prepareFileToSend(endpointId: String, uri: Uri, fileName: String) {
        pendingFileName = fileName
        pendingUriToSend = uri
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd != null) {
            val payload = Payload.fromFile(pfd)
            pendingFilePayload = payload
            val infoJson = JSONObject().apply {
                put("type", "FILE_INFO")
                put("fileName", fileName)
                put("payloadId", payload.id)
            }
            nearbyManager.sendPayload(endpointId, Payload.fromBytes(infoJson.toString().toByteArray()))
        }
    }

    fun acceptTransfer(endpointId: String) {
        val acceptJson = JSONObject().apply { put("type", "TRANSFER_ACCEPT") }
        nearbyManager.sendPayload(endpointId, Payload.fromBytes(acceptJson.toString().toByteArray()))
        transferState.value = transferState.value.copy(pendingFileInfo = null)
    }

    fun rejectTransfer(endpointId: String) {
        val rejectJson = JSONObject().apply { put("type", "TRANSFER_REJECT") }
        nearbyManager.sendPayload(endpointId, Payload.fromBytes(rejectJson.toString().toByteArray()))
        transferState.value = transferState.value.copy(pendingFileInfo = null)
    }

    private fun startFileTransfer(endpointId: String) {
        pendingFilePayload?.let { payload ->
            nearbyManager.sendPayload(endpointId, payload)
            transferState.value = transferState.value.copy(
                fileName = pendingFileName,
                isTransferring = true,
                progress = 0f
            )
        }
    }
}
