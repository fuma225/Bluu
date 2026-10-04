package com.privat.bluetoothmanager.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.privat.bluetoothmanager.model.BtDevice
import com.privat.bluetoothmanager.model.DeviceStatus
import com.privat.bluetoothmanager.ui.theme.TextSecondary

/**
 * N'expose QUE des actions permises par Android pour une application tierce :
 *  - voir les infos de base,
 *  - lancer une demande d'association officielle (boîte de dialogue système),
 *  - ouvrir les réglages Bluetooth pour tout ce que l'app ne peut pas faire elle-même
 *    (connecter de force un appareil déjà associé, par exemple : Android réserve
 *    cette action aux réglages système, pas aux apps tierces).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
    device: BtDevice,
    onDismiss: () -> Unit,
    onRequestBond: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(text = device.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = device.address, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            when (device.status) {
                DeviceStatus.AVAILABLE -> {
                    Text(
                        "Cet appareil n'est pas encore associé à ton téléphone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestBond, modifier = Modifier.fillMaxWidth()) {
                        Text("Associer cet appareil")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Android affichera sa propre boîte de dialogue de confirmation : " +
                            "l'application ne peut pas associer un appareil sans cette étape.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                DeviceStatus.BONDED -> {
                    Text(
                        "Appareil associé mais pas connecté actuellement.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Android réserve la connexion effective d'un appareil déjà associé " +
                            "aux réglages système ou à l'appairage rapide — une app tierce ne peut pas " +
                            "la déclencher elle-même. Utilise le bouton ci-dessous.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ouvrir les réglages Bluetooth")
                    }
                }

                DeviceStatus.CONNECTED -> {
                    Text(
                        "Cet appareil est actuellement connecté à ton téléphone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gérer / déconnecter dans les réglages")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Déconnecter un appareil connecté est une action protégée par Android : " +
                            "elle se fait depuis les réglages système, pas depuis une app tierce.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                DeviceStatus.UNKNOWN -> {
                    Text(
                        "Statut inconnu pour cet appareil.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
