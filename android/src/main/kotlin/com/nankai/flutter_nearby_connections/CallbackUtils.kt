package com.nankai.flutter_nearby_connections

import android.app.Activity
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import io.flutter.plugin.common.MethodChannel

const val connecting = 1
const val connected = 2
const val notConnected = 3

class CallbackUtils constructor(private val channel: MethodChannel, private val activity: Activity) {

    private val devices = mutableListOf<DeviceJson>()
    private val gson = Gson()
    private fun deviceExists(deviceId: String) = devices.any { element -> element.deviceID == deviceId }
    private fun device(deviceId: String): DeviceJson? = devices.find { element -> element.deviceID == deviceId }
    fun updateStatus(deviceId: String, state: Int) {
        devices.find { element -> element.deviceID == deviceId }?.state = state
        val json = gson.toJson(devices)
        channel.invokeMethod(INVOKE_CHANGE_STATE_METHOD, json)
    }

    fun addDevice(device: DeviceJson) {
        if (deviceExists(device.deviceID)) {
            updateStatus(device.deviceID, device.state)
        } else {
            devices.add(device)
        }
        val json = gson.toJson(devices)
        channel.invokeMethod(INVOKE_CHANGE_STATE_METHOD, json)
    }

    fun removeDevice(deviceId: String) {
        devices.remove(device(deviceId))
        val json = gson.toJson(devices)
        channel.invokeMethod(INVOKE_CHANGE_STATE_METHOD, json)
    }

    val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            if(!deviceExists(endpointId)) {
                val data = DeviceJson(endpointId, discoveredEndpointInfo.endpointName, notConnected)
                addDevice(data)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            if (deviceExists(endpointId)) {
                Nearby.getConnectionsClient(activity).disconnectFromEndpoint(endpointId)
            }
            removeDevice(endpointId)
        }
    }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        var tempPayload: String = ""
        var tempPayloadId: String = ""

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            tempPayload = String(payload.asBytes()!!)
            val args = mutableMapOf(
                "deviceId" to endpointId,
                "message" to String(payload.asBytes()!!),
                "payloadId" to payload.id
                    .toString().split("-")[1]
            )
            channel.invokeMethod(INVOKE_MESSAGE_RECEIVE_METHOD, args)
        }

        override fun onPayloadTransferUpdate(endpointId: String, payloadTransferUpdate: PayloadTransferUpdate) {
            // required for files and streams
            if (payloadTransferUpdate.status == PayloadTransferUpdate.Status.IN_PROGRESS) {
                if (tempPayload == "") {
                    tempPayloadId = payloadTransferUpdate.payloadId.toString()
                    val args = mutableMapOf(
                        "deviceId" to endpointId,
                        "status" to payloadTransferUpdate.status,
                        "payloadId" to payloadTransferUpdate.payloadId
                            .toString().split("-")[1]
                    )
                    channel.invokeMethod(INVOKE_PAYLOAD_TRANSFER_UPDATE_METHOD, args)
                } else {
                    tempPayload = ""
                }
            }

            if (payloadTransferUpdate.status == PayloadTransferUpdate.Status.SUCCESS) {
                if (tempPayloadId == payloadTransferUpdate.payloadId.toString()) {
                    val args = mutableMapOf(
                        "deviceId" to endpointId,
                        "status" to payloadTransferUpdate.status,
                        "payloadId" to payloadTransferUpdate.payloadId
                            .toString().split("-")[1]
                    )
                    channel.invokeMethod(INVOKE_PAYLOAD_TRANSFER_UPDATE_METHOD, args)
                    tempPayloadId = ""
                }
            }
        }
    }

    val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val data = DeviceJson(endpointId, connectionInfo.endpointName, connecting)
            addDevice(data)
            Nearby.getConnectionsClient(activity).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val data = when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    DeviceJson(endpointId,
                        if (device(endpointId)?.outletName == null) "Null"
                        else device(endpointId)?.outletName!!, connected)
                }
                else -> {
                    DeviceJson(endpointId,
                        if (device(endpointId)?.outletName == null) "Null"
                        else device(endpointId)?.outletName!!, notConnected)
                }
            }
            addDevice(data)

            val args = mutableMapOf("deviceId" to endpointId, "status" to result.status.statusCode)
            channel.invokeMethod(INVOKE_CALLBACK_UTIL_METHOD, args)
        }

        override fun onDisconnected(endpointId: String) {
            if (deviceExists(endpointId)) {
                updateStatus(endpointId, notConnected)
            } else {
                val data = DeviceJson(endpointId,
                        if (device(endpointId)?.outletName == null) "Null"
                        else device(endpointId)?.outletName!!, notConnected)
                addDevice(data)
            }
        }
    }
}
