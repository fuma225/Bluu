package com.privat.bluetoothmanager

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privat.bluetoothmanager.model.BtDevice
import com.privat.bluetoothmanager.model.DeviceKind
import com.privat.bluetoothmanager.model.DeviceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Toute la logique Bluetooth de l'application repose UNIQUEMENT sur des API publiques
 * documentées par Android :
 *  - BluetoothAdapter.getBondedDevices()      -> appareils déjà associés
 *  - BluetoothAdapter.startDiscovery()        -> scan des appareils à proximité
 *  - BluetoothDevice.createBond()             -> lancer un appairage (demande une confirmation système)
 *  - BluetoothProfile / getProfileProxy()     -> savoir quels appareils associés sont connectés
 *  - Settings.ACTION_BLUETOOTH_SETTINGS       -> ouvrir les réglages système
 *
 * Aucune API cachée, aucune réflexion, aucune tentative de déconnecter ou de piloter
 * un appareil qui n'est pas associé à ce téléphone. Quand Android ne permet pas une
 * action (ex: forcer la connexion d'un appareil déjà connecté à un autre profil),
 * l'UI explique la limitation et propose d'ouvrir les réglages système.
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context get() = getApplication<Application>().applicationContext

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    data class UiState(
        val bluetoothSupported: Boolean = true,
        val bluetoothEnabled: Boolean = false,
        val hasScanPermission: Boolean = false,
        val hasConnectPermission: Boolean = false,
        val isScanning: Boolean = false,
        val discovered: List<BtDevice> = emptyList(),
        val bonded: List<BtDevice> = emptyList(),
        val connectedAddresses: Set<String> = emptySet(),
        val limitationMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState(bluetoothSupported = adapter != null))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Proxies de profil ouverts pour lire les appareils connectés (API publique officielle)
    private val openProfiles = mutableListOf<Pair<Int, BluetoothProfile>>()

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableDeviceExtra() ?: return
                    if (!hasConnectPermission()) return
                    val btDevice = device.toBtDevice(isBonded = false, isConnected = false)
                    _uiState.update { state ->
                        val already = state.discovered.any { it.address == btDevice.address }
                        if (already) state else state.copy(discovered = state.discovered + btDevice)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _uiState.update { it.copy(isScanning = true) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _uiState.update { it.copy(isScanning = false) }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    refreshAdapterState()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(discoveryReceiver, filter)
        }
        refreshPermissions()
        refreshAdapterState()
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { appContext.unregisterReceiver(discoveryReceiver) }
        if (hasConnectPermission()) {
            openProfiles.forEach { (profileId, proxy) -> adapter?.closeProfileProxy(profileId, proxy) }
        }
        if (hasScanPermission() && uiState.value.isScanning) {
            stopScan()
        }
    }

    // ---- Permissions ----------------------------------------------------

    fun hasScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun hasConnectPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true // BLUETOOTH / BLUETOOTH_ADMIN sont des permissions "normales" avant Android 12
    }

    /** Permissions à demander dynamiquement, propre à la version d'Android de l'utilisateur. */
    fun requiredRuntimePermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun refreshPermissions() {
        _uiState.update {
            it.copy(hasScanPermission = hasScanPermission(), hasConnectPermission = hasConnectPermission())
        }
        if (hasConnectPermission()) {
            refreshBondedDevices()
            refreshConnectedDevices()
        }
    }

    // ---- État de l'adaptateur --------------------------------------------

    fun refreshAdapterState() {
        val enabled = adapter?.isEnabled == true
        _uiState.update { it.copy(bluetoothEnabled = enabled, bluetoothSupported = adapter != null) }
        if (enabled && hasConnectPermission()) {
            refreshBondedDevices()
            refreshConnectedDevices()
        }
    }

    // ---- Scan (découverte) -------------------------------------------------

    @SuppressLint("MissingPermission")
    fun startScan() {
        val bt = adapter ?: return
        if (!hasScanPermission()) {
            _uiState.update { it.copy(limitationMessage = "La recherche d'appareils nécessite l'autorisation Bluetooth / localisation. Ouvre les réglages de l'application pour l'accorder.") }
            return
        }
        if (!bt.isEnabled) {
            _uiState.update { it.copy(limitationMessage = "Le Bluetooth est désactivé. Active-le pour lancer une recherche.") }
            return
        }
        _uiState.update { it.copy(discovered = emptyList(), limitationMessage = null) }
        if (bt.isDiscovering) bt.cancelDiscovery()
        bt.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        val bt = adapter ?: return
        if (!hasScanPermission()) return
        if (bt.isDiscovering) bt.cancelDiscovery()
    }

    // ---- Appareils associés ("Mes appareils") -------------------------------

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val bt = adapter ?: return
        if (!hasConnectPermission()) return
        val connected = uiState.value.connectedAddresses
        val list = bt.bondedDevices.map { device ->
            device.toBtDevice(isBonded = true, isConnected = connected.contains(device.address))
        }.sortedBy { it.name.lowercase() }
        _uiState.update { it.copy(bonded = list) }
    }

    // ---- Appareils actuellement connectés (via proxies de profil publics) ----

    @SuppressLint("MissingPermission")
    fun refreshConnectedDevices() {
        val bt = adapter ?: return
        if (!hasConnectPermission()) return

        val profileIds = listOf(
            BluetoothProfile.HEADSET,
            BluetoothProfile.A2DP,
            BluetoothProfile.GATT,
            BluetoothProfile.HEALTH
        )

        profileIds.forEach { profileId ->
            bt.getProfileProxy(appContext, object : BluetoothProfile.ServiceListener {
                @SuppressLint("MissingPermission")
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    openProfiles.add(profile to proxy)
                    val addresses = runCatching { proxy.connectedDevices.map { it.address } }.getOrDefault(emptyList())
                    _uiState.update { state ->
                        val merged = state.connectedAddresses + addresses
                        state.copy(connectedAddresses = merged)
                    }
                    refreshBondedDevices()
                }

                override fun onServiceDisconnected(profile: Int) {
                    openProfiles.removeAll { it.first == profile }
                }
            }, profileId)
        }
    }

    // ---- Association (pairing) --------------------------------------------

    @SuppressLint("MissingPermission")
    fun requestBond(address: String) {
        if (!hasConnectPermission()) {
            _uiState.update { it.copy(limitationMessage = "L'association nécessite l'autorisation Bluetooth.") }
            return
        }
        val device = adapter?.getRemoteDevice(address) ?: return
        viewModelScope.launch {
            // createBond() déclenche la boîte de dialogue système standard d'Android.
            // Android gère seul la confirmation utilisateur ; l'app ne peut pas la contourner.
            val started = device.createBond()
            if (!started) {
                _uiState.update { it.copy(limitationMessage = "Android n'a pas pu démarrer l'association avec cet appareil.") }
            }
        }
    }

    fun clearLimitationMessage() {
        _uiState.update { it.copy(limitationMessage = null) }
    }

    fun noteUnsupportedAction(explanation: String) {
        _uiState.update { it.copy(limitationMessage = explanation) }
    }

    // ---- Helpers ------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toBtDevice(isBonded: Boolean, isConnected: Boolean): BtDevice {
        val displayName = if (hasConnectPermission()) (name ?: "Appareil inconnu") else "Appareil (autorisation requise)"
        val displayAddress = if (hasConnectPermission()) address else "Adresse masquée"
        val status = when {
            isConnected -> DeviceStatus.CONNECTED
            isBonded -> DeviceStatus.BONDED
            else -> DeviceStatus.AVAILABLE
        }
        return BtDevice(
            name = displayName,
            address = displayAddress,
            kind = bluetoothClass.toDeviceKind(),
            status = status
        )
    }

    private fun BluetoothClass?.toDeviceKind(): DeviceKind {
        val majorClass = this?.majorDeviceClass ?: return DeviceKind.UNKNOWN
        return when (majorClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                when (this.deviceClass) {
                    BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                    BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> DeviceKind.HEADPHONES
                    BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> DeviceKind.SPEAKER
                    BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> DeviceKind.CAR
                    else -> DeviceKind.HEADPHONES
                }
            }
            BluetoothClass.Device.Major.PHONE -> DeviceKind.PHONE
            BluetoothClass.Device.Major.COMPUTER -> DeviceKind.COMPUTER
            BluetoothClass.Device.Major.WEARABLE -> DeviceKind.WATCH
            else -> DeviceKind.UNKNOWN
        }
    }

    private fun Intent.getParcelableDeviceExtra(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
}
