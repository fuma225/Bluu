package com.privat.bluetoothmanager.model

/**
 * Catégorie utilisée uniquement pour choisir une icône dans l'interface.
 * Dérivée de BluetoothClass.Device quand Android nous donne cette information.
 */
enum class DeviceKind {
    HEADPHONES,
    SPEAKER,
    PHONE,
    CAR,
    COMPUTER,
    WATCH,
    UNKNOWN
}

/**
 * État d'association/connexion tel que rapporté par les API Android officielles.
 */
enum class DeviceStatus {
    CONNECTED,   // Associé ET actuellement connecté (via BluetoothProfile public)
    BONDED,      // Associé mais pas connecté actuellement
    AVAILABLE,   // Détecté par le scan, non associé
    UNKNOWN
}

/**
 * Représentation simplifiée d'un BluetoothDevice pour l'UI.
 * Le champ [address] n'est lisible que si Android a accordé BLUETOOTH_CONNECT
 * (depuis Android 12). Sinon Android renvoie une adresse anonymisée "02:00:00:00:00:00".
 */
data class BtDevice(
    val name: String,
    val address: String,
    val kind: DeviceKind,
    val status: DeviceStatus,
    val rssi: Int? = null // force du signal si connue (lors d'un scan), sinon null
)
