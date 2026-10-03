ok its nice make uiux like a share it or share me and wehne i send file fron one devive to another files not vied and aldo take permission to connect the wifi blutteo and all make inter face like share it app




Build a complete Android file-sharing application similar in concept to Google Quick Share and SHAREit.

The core requirement is:

**The app must share files directly from one Android device to another Android device using local device-to-device connectivity. Do NOT upload files to a cloud server. Do NOT require an internet connection for file transfer.**

## App Name

Use a modern name such as **ShareX** or **NearbyShare**.

## Technology

Build it as a native Android application using:

- Kotlin
- Android Studio
- Jetpack Compose
- Android Storage Access Framework
- Google Nearby Connections API for nearby device discovery and communication
- Room database for transfer history
- Foreground Service for large/background transfers
- Android notification system for transfer progress

Use current Android APIs and follow modern Android permission requirements.

## Core functionality

### 1. Send Files

The user should be able to:

1. Open the app.
2. Tap "Send".
3. Select files directly from the phone's local storage.
4. Support:
   - Photos
   - Videos
   - Music
   - Documents
   - ZIP/RAR files
   - APK files
   - PDFs
   - Any other file type
5. Allow multiple files to be selected.
6. Calculate total size.
7. Search for nearby devices.
8. Display nearby devices with device name and icon.
9. User selects a device.
10. Send a connection request.
11. Receiver gets an incoming transfer request.
12. Receiver can Accept or Reject.
13. Start direct device-to-device transfer after acceptance.
14. Display real-time transfer progress.
15. Display transfer speed.
16. Display transferred amount and remaining amount.
17. Display estimated remaining time.
18. Notify the user when transfer is complete.

## 2. Receive Files

The receiver should be able to:

1. Open the app.
2. Tap "Receive".
3. Become discoverable to nearby devices.
4. Display:
   - Device name
   - Connection status
   - Nearby sender
5. When a sender wants to transfer a file, show:

"Incoming File"

Filename  
File type  
File size  
Sender device

Buttons:

[Reject] [Accept]

6. Only begin transfer after the user accepts.
7. Save received files to an appropriate local folder such as:

/Download/ShareX/

8. Show transfer progress.
9. Show completion notification.
10. Allow opening the received file after transfer.

## 3. Local device-to-device transfer

This is the most important requirement.

The application must transfer files directly between Android devices.

Architecture:

Android Device A
↓
Nearby device discovery
↓
Secure local connection
↓
Android Device B
↓
File transfer
↓
Local storage

Do NOT implement the primary transfer through:

- Firebase Storage
- Google Drive
- AWS S3
- Cloudinary
- Supabase Storage
- Any other cloud file-storage service

The actual file bytes must travel directly between the two devices using the local connection established by the nearby-device technology.

Internet should NOT be required for normal nearby file transfer.

## 4. Large file support

The application must support large files such as:

- 100 MB
- 500 MB
- 1 GB
- 5 GB+
 
Do NOT load the entire file into RAM.

Use streaming/chunked transfer.

Example:

File
↓
Chunk 1
↓
Chunk 2
↓
Chunk 3
↓
...
↓
Chunk N

Read and transmit the file incrementally.

The receiver must write chunks directly to disk.

## 5. Transfer protocol

Create a reliable transfer protocol containing messages such as:

DEVICE_INFO
CONNECTION_REQUEST
CONNECTION_ACCEPT
CONNECTION_REJECT
TRANSFER_REQUEST
TRANSFER_ACCEPT
TRANSFER_REJECT
FILE_INFO
FILE_CHUNK
FILE_COMPLETE
TRANSFER_CANCEL
TRANSFER_PAUSE
TRANSFER_RESUME
TRANSFER_ERROR

The FILE_INFO message should contain information such as:

- filename
- file size
- MIME type
- file ID
- checksum/hash
- number of chunks

Example:

{
  "type": "FILE_INFO",
  "fileName": "video.mp4",
  "fileSize": 524288000,
  "mimeType": "video/mp4",
  "fileId": "unique-id",
  "checksum": "..."
}

## 6. Resume interrupted transfers

If possible, implement transfer recovery.

If a 2 GB file reaches 80% and the connection is interrupted:

Do NOT restart from zero.

Store the received byte/chunk position and allow the transfer to continue from the last successfully received chunk.

Example:

Connection lost at:

1.6 GB / 2 GB

After reconnect:

Resume from approximately 1.6 GB.

## 7. Security

Security is important.

Before transferring files:

1. Discover device.
2. Establish connection.
3. Authenticate/pair devices.
4. Ask receiver for permission.
5. Transfer only after acceptance.

Implement secure communication using the security mechanisms provided by the selected nearby communication API.

Add optional PIN/verification-code pairing:

Sender:

"Verification code: 482931"

Receiver:

"Verification code: 482931"

Only continue when the codes match.

Never automatically accept files from unknown devices.

## 8. File integrity

After transfer, verify the received file.

Calculate a checksum/hash for the original file and compare it with the received file.

Example:

Original SHA-256
=
Received SHA-256

If they match:

"Transfer completed successfully."

If they don't:

"File verification failed."

## 9. User interface

Create a modern UI inspired by Quick Share and SHAREit but do NOT copy their exact branding or UI.

Home screen:

ShareX

"Fast. Private. Local."

[ Send ]

[ Receive ]

Nearby devices

Recent transfers

Settings

### Send screen

Show:

Selected files

File thumbnails/icons

Total size

[ Add Files ]

Nearby devices

Searching animation

Device cards:

📱 Sujal's Phone
📱 Rahul's Phone

### Receive screen

Show:

Ready to receive

"Your device is visible to nearby ShareX users."

Device name

[ Make Discoverable ]

### Transfer screen

Show:

Filename

File icon

Progress:

████████████░░░ 82%

82%

Transferred:
820 MB / 1 GB

Speed:
18.5 MB/s

Remaining:
10 seconds

[ Cancel ]

## 10. File picker

Use Android's Storage Access Framework.

Do not assume a fixed file path such as:

/storage/emulated/0/Download/

The user should be able to select files from Android's system file picker.

Support multiple file selection.

## 11. Received-file storage

Save received files locally.

Default directory:

Download/ShareX/

Organize files optionally:

Download/ShareX/
├── Images/
├── Videos/
├── Documents/
├── Music/
└── Other/

Do not overwrite an existing file automatically.

If:

photo.jpg

already exists, create:

photo (1).jpg

## 12. Transfer history

Create a local Room database.

Store:

- filename
- size
- sender/receiver
- date/time
- transfer status
- transfer duration
- transfer speed
- file path

History screen:

Recent Transfers

✓ video.mp4
1.2 GB
Sent
Today

✓ photo.jpg
4.5 MB
Received
Today

✕ project.zip
850 MB
Failed

Allow:

[Open]
[Share]
[Delete from History]

Deleting history should not accidentally delete the actual file unless the user explicitly chooses that option.

## 13. Background transfers

Large transfers should continue when the user temporarily leaves the screen.

Use an Android Foreground Service where appropriate.

Show notification:

"Sending video.mp4"

82% • 18.5 MB/s

The notification should provide:

Pause
Resume
Cancel

## 14. Multiple file transfer

Allow users to select:

photo1.jpg
photo2.jpg
video.mp4
document.pdf
music.mp3

and send them in one transfer session.

Show:

5 files
Total: 1.8 GB

Transfer each file sequentially and show overall progress.

## 15. Device discovery

Use Nearby Connections for discovering nearby Android devices.

The app should:

- advertise when receiving
- discover nearby senders/receivers
- show device names
- connect to selected devices
- stop discovery when appropriate
- disconnect cleanly after transfer

Handle:

- Bluetooth disabled
- Wi-Fi disabled
- permissions denied
- device unavailable
- connection timeout
- connection lost

with clear user messages.

## 16. Permissions

Use only the permissions actually required by the implementation and target Android SDK.

Request dangerous/runtime permissions at the appropriate time instead of requesting everything on first launch.

Explain why nearby-device permissions are required.

Handle permission denial gracefully.

## 17. Offline requirement

Normal nearby file sharing must work without internet.

Test this scenario:

Phone A:
Mobile data OFF
Internet OFF

Phone B:
Mobile data OFF
Internet OFF

Both devices are physically nearby.

They should still be able to discover/connect and transfer files using the supported local nearby transport.

Do not depend on a remote server for the actual file transfer.

## 18. Error handling

Handle:

- Permission denied
- Nearby device not found
- Connection timeout
- Connection lost
- Receiver rejected
- Sender cancelled
- Receiver cancelled
- Insufficient storage
- File not found
- File permission revoked
- Corrupted transfer
- Checksum mismatch
- Unsupported operation
- Bluetooth/Wi-Fi unavailable

Show understandable messages instead of crashes.

## 19. Settings

Add:

Settings

Device name
Visibility
Download folder
Auto-accept from trusted devices
Transfer notifications
Theme
Dark mode
Light mode
About

Do NOT enable unrestricted auto-accept by default.

## 20. Performance

Optimize for:

- large files
- low memory usage
- stable transfer
- efficient streaming
- minimal battery usage
- reconnect/resume
- multiple file transfers

Do not read entire files into memory.

## 21. Project architecture

Use clean architecture and separate responsibilities.

Suggested structure:

app/
├── ui/
├── nearby/
├── transfer/
├── security/
├── storage/
├── database/
├── service/
├── notification/
├── utils/
└── MainActivity.kt

Create separate classes for:

NearbyManager
DeviceDiscoveryManager
ConnectionManager
FilePickerManager
FileSender
FileReceiver
TransferManager
TransferSession
TransferRepository
ChecksumManager
SecurityManager
NotificationManager
TransferDatabase

## 22. Testing

Test with at least two physical Android devices.

Test:

1. Small image
2. Multiple images
3. 500 MB video
4. 1 GB+ file
5. Multiple files
6. Connection interruption
7. Receiver rejection
8. Sender cancellation
9. Low storage
10. Permissions denied
11. Internet disabled
12. Bluetooth/Wi-Fi state changes
13. App moved to background
14. Screen locked during transfer if supported

## 23. Important restriction

Do NOT create a fake file-sharing system where the app merely pretends to transfer files.

Implement actual Android-to-Android local transfer.

The final application must allow:

Phone A → Phone B

using local nearby connectivity, with the actual selected local file being transferred and saved on Phone B.

## 24. Deliverables

Generate the complete Android Studio project including:

- Gradle configuration
- AndroidManifest.xml
- Kotlin source code
- Jetpack Compose UI
- Nearby Connections integration
- File picker
- File sender
- File receiver
- Streaming/chunking
- Progress tracking
- Transfer history
- Room database
- Notifications
- Foreground service
- Error handling
- Permissions
- Security/pairing
- README
- Setup instructions
- Testing instructions

Make the project compile successfully.

Do not leave core functionality as TODOs or mock functions.

Start by implementing a working MVP:

**Select Local File → Discover Nearby Android Device → Connect → Receiver Accepts → Direct Local Transfer → Save File → Verify File → Show Completed Transfer.**

Then build the advanced features on top of the working MVP.SSSS