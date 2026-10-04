package com.privat.bluetoothmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.privat.bluetoothmanager.model.BtDevice
import com.privat.bluetoothmanager.model.DeviceKind
import com.privat.bluetoothmanager.model.DeviceStatus
import com.privat.bluetoothmanager.ui.theme.AmberBonded
import com.privat.bluetoothmanager.ui.theme.GreenConnected
import com.privat.bluetoothmanager.ui.theme.TextSecondary

private fun iconFor(kind: DeviceKind): ImageVector = when (kind) {
    DeviceKind.HEADPHONES -> Icons.Filled.Headset
    DeviceKind.SPEAKER -> Icons.Filled.SpeakerGroup
    DeviceKind.PHONE -> Icons.Filled.Smartphone
    DeviceKind.CAR -> Icons.Filled.DirectionsCar
    DeviceKind.COMPUTER -> Icons.Filled.Laptop
    DeviceKind.WATCH -> Icons.Filled.Watch
    DeviceKind.UNKNOWN -> Icons.Filled.Bluetooth
}

private fun labelFor(status: DeviceStatus): String = when (status) {
    DeviceStatus.CONNECTED -> "Connecté"
    DeviceStatus.BONDED -> "Associé"
    DeviceStatus.AVAILABLE -> "Disponible"
    DeviceStatus.UNKNOWN -> "Non associé"
}

private fun colorFor(status: DeviceStatus) = when (status) {
    DeviceStatus.CONNECTED -> GreenConnected
    DeviceStatus.BONDED -> AmberBonded
    else -> TextSecondary
}

@Composable
fun DeviceCard(device: BtDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = iconFor(device.kind),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                Text(text = device.address, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = colorFor(device.status).copy(alpha = 0.12f)
            ) {
                Text(
                    text = labelFor(device.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorFor(device.status),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
