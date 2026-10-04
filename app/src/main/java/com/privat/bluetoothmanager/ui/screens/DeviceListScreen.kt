package com.privat.bluetoothmanager.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privat.bluetoothmanager.BluetoothViewModel
import com.privat.bluetoothmanager.model.BtDevice
import com.privat.bluetoothmanager.ui.components.DeviceCard
import com.privat.bluetoothmanager.ui.theme.TextSecondary

@Composable
fun DeviceListScreen(
    viewModel: BluetoothViewModel,
    onRequestPermissions: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedDevice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<BtDevice?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Appareils à proximité", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Recherche officielle via Android — aucun appareil tiers n'est modifié ou déconnecté.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!state.hasScanPermission || !state.hasConnectPermission) {
            PermissionNotice(onRequestPermissions)
            Spacer(modifier = Modifier.height(12.dp))
        } else if (!state.bluetoothEnabled) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Le Bluetooth est désactivé.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }) { Text("Activer dans les réglages") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        state.limitationMessage?.let { message ->
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp))
                Text(if (state.isScanning) " Arrêter" else " Actualiser")
            }
            OutlinedButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text(" Paramètres")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isScanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                Text("Recherche en cours…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.discovered.isEmpty() && !state.isScanning) {
            Text(
                "Aucun appareil détecté pour l'instant. Appuie sur Actualiser pour lancer une recherche.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.discovered, key = { it.address }) { device ->
                DeviceCard(device = device) { selectedDevice = device }
            }
        }
    }

    selectedDevice?.let { device ->
        DeviceDetailSheet(
            device = device,
            onDismiss = { selectedDevice = null },
            onRequestBond = {
                viewModel.requestBond(device.address)
                selectedDevice = null
            }
        )
    }
}

@Composable
private fun PermissionNotice(onRequestPermissions: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Android exige une autorisation explicite pour rechercher et afficher les appareils Bluetooth.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRequestPermissions) {
                Text("Autoriser l'accès Bluetooth")
            }
        }
    }
}
