package com.privat.bluetoothmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.privat.bluetoothmanager.ui.screens.DeviceListScreen
import com.privat.bluetoothmanager.ui.screens.MyDevicesScreen
import com.privat.bluetoothmanager.ui.theme.BluetoothManagerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BluetoothViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.refreshPermissions()
                viewModel.refreshAdapterState()
            }

            BluetoothManagerTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    var selectedTab by remember { mutableIntStateOf(0) }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                                    label = { Text("À proximité") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                                    label = { Text("Mes appareils") }
                                )
                            }
                        }
                    ) { padding ->
                        Surface(modifier = Modifier.padding(padding)) {
                            when (selectedTab) {
                                0 -> DeviceListScreen(
                                    viewModel = viewModel,
                                    onRequestPermissions = {
                                        permissionLauncher.launch(viewModel.requiredRuntimePermissions())
                                    }
                                )
                                else -> MyDevicesScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // L'utilisateur peut revenir des réglages système (Bluetooth activé, permission
        // accordée...) : on resynchronise l'état à chaque retour sur l'app.
        viewModel.refreshPermissions()
        viewModel.refreshAdapterState()
    }
}
