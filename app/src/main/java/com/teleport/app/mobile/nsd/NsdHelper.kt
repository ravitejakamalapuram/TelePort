package com.teleport.app.mobile.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredTv(
    val name: String,
    val ipAddress: String,
    val port: Int
)

class NsdHelper(context: Context) {
    private val TAG = "NsdHelper"
    private val SERVICE_TYPE = "_teleport._tcp."

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val product = Build.PRODUCT ?: ""
        val hardware = Build.HARDWARE ?: ""
        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("sdk_x86")
                || product.contains("vbox86p")
                || product.contains("emulator")
                || product.contains("simulator")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
    }

    fun startDiscovery() {
        if (discoveryListener != null) {
            stopDiscovery()
        }

        _discoveredTvs.value = if (isEmulator()) {
            listOf(DiscoveredTv("Local TV Emulator (127.0.0.1)", "127.0.0.1", 8080))
        } else {
            emptyList()
        }

        discoveryListener = object : NsdManager.DiscoveryListener {

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: Error code $errorCode")
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: Error code $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: $serviceInfo")
                if (serviceInfo.serviceType == SERVICE_TYPE || serviceInfo.serviceType.startsWith(SERVICE_TYPE)) {
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: $serviceInfo")
                removeTvByName(serviceInfo.serviceName)
            }
        }

        try {
            nsdManager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service discovery", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed: Error code $errorCode")
            }

            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve succeeded: $resolvedServiceInfo")
                val host = resolvedServiceInfo.host
                val ipAddress = host?.hostAddress ?: return
                val port = resolvedServiceInfo.port
                val name = resolvedServiceInfo.serviceName

                val newTv = DiscoveredTv(name, ipAddress, port)
                val currentList = _discoveredTvs.value.toMutableList()
                
                // Add or replace
                val index = currentList.indexOfFirst { it.name == name }
                if (index != -1) {
                    currentList[index] = newTv
                } else {
                    currentList.add(newTv)
                }
                _discoveredTvs.value = currentList
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service", e)
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
        }
        discoveryListener = null
    }

    private fun removeTvByName(name: String) {
        val currentList = _discoveredTvs.value.toMutableList()
        if (currentList.removeAll { it.name == name }) {
            _discoveredTvs.value = currentList
        }
    }
}
