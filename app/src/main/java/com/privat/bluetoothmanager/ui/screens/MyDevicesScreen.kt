package com.privat.bluetoothmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privat.bluetoothmanager.BluetoothViewModel
import com.privat.bluetoothmanager.model.BtDevice
import com.privat.bluetoothmanager.ui.components.DeviceCard
import com.privat.bluetoothmanager.ui.theme.TextSecondary

@Composable
fun MyDevicesScreen(viewModel: BluetoothViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDevice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<BtDevice?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mes appareils", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Appareils déjà associés à ce téléphone, avec leur état de connexion actuel.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            viewModel.refreshBondedDevices()
            viewModel.refreshConnectedDevices()
        }) {
            Text("Actualiser la liste")
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (!state.hasConnectPermission) {
            Text(
                "Autorisation Bluetooth requise pour afficher cette liste.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else if (state.bonded.isEmpty()) {
            Text(
                "Aucun appareil associé pour l'instant.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.bonded, key = { it.address }) { device ->
                DeviceCard(device = device) { selectedDevice = device }
            }
        }
    }

    selectedDevice?.let { device ->
        DeviceDetailSheet(
            device = device,
            onDismiss = { selectedDevice = null },
            onRequestBond = { selectedDevice = null }
        )
    }
}
