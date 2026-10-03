# ShareX (Android Local File Sharing App)

ShareX is a fast, private, and fully local Android file-sharing application designed to transfer files directly between devices without requiring an internet connection. It leverages Google's **Nearby Connections API** to establish secure, direct peer-to-peer connections over Wi-Fi Direct and Bluetooth.

## Features

- **Direct P2P Transfer**: Transfers files locally using device-to-device connectivity. Does not upload files to any cloud server.
- **Offline Functionality**: Works entirely without internet or mobile data.
- **Fast Speeds**: Uses `Payload.fromFile` streams over high-bandwidth Nearby Connection channels.
- **Cross-Device Discovery**: Effortlessly search and discover nearby Android devices.
- **Privacy & Security**: The receiver must explicitly accept any incoming file transfer.
- **Modern Jetpack Compose UI**: Clean and intuitive Send/Receive screens.
- **Transfer History**: View your past file transfers, fully persisted using a local Room database.
- **Direct Save**: Received files are automatically saved to your `Downloads/ShareX` folder.

## Setup Instructions

1. **Prerequisites**:
   - Install **Android Studio** (Koala or newer recommended).
   - JDK 17.

2. **Open the Project**:
   - Launch Android Studio.
   - Select **Open an existing project**.
   - Navigate to the `ShareX` directory and click **OK**.
   - Wait for Gradle to finish syncing (ensure you have an active internet connection for the initial sync to download dependencies).

3. **Build the Application**:
   - Connect your Android device or start an emulator (Android 8.0 / API 26 or higher).
   - Click the **Run** button (green play icon) in Android Studio.

## Testing Instructions

To test the application, you will need **two Android devices** (or one physical device and one emulator that supports Nearby Connections, though two physical devices are highly recommended).

1. **Install App**: Install the ShareX app on both Phone A (Sender) and Phone B (Receiver).
2. **Go Offline**: Turn off mobile data and disconnect from any Wi-Fi networks on both devices to verify that it works entirely offline. (Ensure Bluetooth and Wi-Fi toggles are ON, but not connected to any network).
3. **Set Up Receiver (Phone B)**:
   - Open the app and tap **Receive**.
   - Grant the required Nearby Devices, Location, and Storage permissions.
   - The device will now advertise itself as "ReceiverDevice".
4. **Set Up Sender (Phone A)**:
   - Open the app and tap **Send**.
   - Tap **Select File** and pick an image, video, or document.
   - Grant the required permissions.
   - Wait a few seconds for the "Searching..." list to populate.
   - Tap on **ReceiverDevice** in the list.
5. **Accept & Transfer**:
   - On Phone B, a dialog will appear showing the incoming file name. Tap **Accept**.
   - The transfer will begin immediately. You will see a progress bar on both screens.
   - Upon completion, the file will be saved directly to `Downloads/ShareX` on Phone B.
6. **Verify History**:
   - Navigate back to the Home screen on both devices to see the populated local transfer history.

## Architecture & Stack

- **Kotlin** (1.9.22)
- **Jetpack Compose** (Material 3)
- **Google Nearby Connections API** (19.0.0)
- **Room Database** (2.6.1 for Transfer History)
- **Storage Access Framework (SAF)** (For modern file selection)
- **Coroutines & StateFlow** (For asynchronous and reactive UI)
