# Bluetooth Manager

Application Android (Kotlin + Jetpack Compose) qui permet de rechercher les
appareils Bluetooth à proximité, de consulter les appareils déjà associés à
ton téléphone, et d'accéder rapidement aux actions **officiellement permises
par Android** (association, ouverture des réglages système).

## Ce que l'application fait

- Recherche des appareils Bluetooth visibles à proximité (`BluetoothAdapter.startDiscovery()`).
- Affiche nom, adresse et type (écouteurs, enceinte, voiture, téléphone, ordinateur, montre)
  quand Android autorise l'accès à ces informations.
- Distingue : **Connecté**, **Associé**, **Disponible**, **Non associé**.
- Écran **Mes appareils** : liste des appareils déjà associés (`BluetoothAdapter.getBondedDevices()`)
  et de ceux actuellement connectés (via les API publiques `BluetoothProfile`).
- Bouton **Actualiser** (scan) et bouton **Paramètres Bluetooth** (ouvre les réglages système).
- Au clic sur un appareil : fiche détail avec les actions qu'Android autorise réellement
  pour une app tierce (lancer une demande d'association, ouvrir les réglages pour
  connecter/déconnecter).

## Ce que l'application ne fait PAS (volontairement)

- Elle ne déconnecte jamais un appareil appartenant à quelqu'un d'autre.
- Elle ne tente pas de « prendre la place » d'un téléphone déjà connecté à un
  accessoire Bluetooth.
- Elle n'utilise aucune API cachée, aucune réflexion, aucun brouillage/jamming.
- Elle ne pilote pas le Bluetooth d'un autre appareil à distance.

Quand Android interdit une action à une app tierce (par exemple forcer la
connexion ou la déconnexion d'un appareil déjà associé — réservé aux réglages
système), l'app l'explique clairement et propose un bouton pour ouvrir le
réglage correspondant.

## Permissions utilisées (toutes officielles)

| Permission | Pourquoi |
|---|---|
| `BLUETOOTH_SCAN` (Android 12+) | Lancer une recherche d'appareils |
| `BLUETOOTH_CONNECT` (Android 12+) | Lire nom/adresse, lancer une association |
| `ACCESS_FINE_LOCATION` (Android ≤ 11) | Exigée par Android pour le scan Bluetooth avant la 12 |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` (Android ≤ 11) | Équivalents historiques des deux permissions ci-dessus |

Aucune permission supplémentaire n'est demandée.

## Structure du projet

```
BluetoothManager/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/privat/bluetoothmanager/
│       │   ├── MainActivity.kt              # Navigation + demande de permissions
│       │   ├── BluetoothViewModel.kt         # Toute la logique Bluetooth (API officielles)
│       │   ├── model/BtDevice.kt             # Modèle de données utilisé par l'UI
│       │   └── ui/
│       │       ├── screens/
│       │       │   ├── DeviceListScreen.kt   # Écran "À proximité"
│       │       │   ├── MyDevicesScreen.kt    # Écran "Mes appareils"
│       │       │   └── DeviceDetailSheet.kt  # Fiche détail + actions autorisées
│       │       ├── components/DeviceCard.kt  # Carte réutilisable
│       │       └── theme/                    # Couleurs, typographie, thème Material 3
│       └── res/                              # Icônes, strings, thème XML de base
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Compiler l'APK avec Android Studio

1. Installe **Android Studio** (version récente, ex. Koala/2024.1 ou plus récente) :
   https://developer.android.com/studio
2. Décompresse ce dossier `BluetoothManager/` quelque part sur ton disque.
3. Ouvre Android Studio → **File → Open…** → sélectionne le dossier `BluetoothManager`.
4. Laisse Android Studio télécharger Gradle et synchroniser le projet
   (première fois : ça peut prendre quelques minutes).
5. Branche un téléphone Android (mode développeur + débogage USB activés),
   ou crée un émulateur avec Bluetooth (AVD récent).
6. Clique sur **Run ▶** pour installer et lancer l'app directement.

### Générer un fichier APK à partager/installer manuellement

Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
L'APK généré se trouve ensuite dans :
```
app/build/outputs/apk/debug/app-debug.apk
```
Tu peux transférer ce fichier sur un téléphone Android et l'installer
(il faut autoriser « Installer des apps inconnues » pour la source utilisée).

### Générer un APK signé (release) — optionnel

**Build → Generate Signed Bundle / APK…**, choisis **APK**, crée ou utilise
un keystore existant, puis suis l'assistant. C'est nécessaire uniquement si
tu veux publier l'app ou la distribuer en dehors du débogage.

## Notes pour un débutant

- Le projet utilise **Jetpack Compose** (l'UI moderne d'Android, en Kotlin,
  sans fichiers XML de mise en page).
- Toute la logique Bluetooth est centralisée dans `BluetoothViewModel.kt` :
  c'est le seul fichier à regarder si tu veux comprendre comment l'app parle
  au Bluetooth du téléphone.
- Les écrans (`DeviceListScreen`, `MyDevicesScreen`) ne font qu'afficher
  l'état exposé par le ViewModel (`uiState`) — ils ne contiennent aucune
  logique Bluetooth directe.
- Si l'app n'affiche aucun nom/adresse d'appareil : vérifie que la permission
  Bluetooth a bien été accordée (bouton « Autoriser l'accès Bluetooth » sur
  l'écran principal si besoin).
