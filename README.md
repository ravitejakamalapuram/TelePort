# TelePort 📺📱

**TelePort** is a local-first, serverless, and cloud-independent ecosystem that connects an **Android TV** browser with a companion **Android Mobile Remote**. 

It is designed to bypass standard screen-casting and DRM restrictions (like those encountered during traditional screen mirroring) by running a native WebView browser directly on the Android TV and using a local Wi-Fi WebSocket connection to control it from a mobile device.

---

## Key Features

- **Multi-Tab TV Browsing**: Open, switch, and close multiple browsing tabs natively on the TV WebView.
- **Air Mouse / Motion Pointer**: Stream pitch and yaw data from the mobile device's gyroscope directly to the TV to move a smooth, hardware-accelerated virtual cursor overlay.
- **Virtual Touchpad & Keyboard**: Precise relative trackpad touch navigation, scrolling, D-pad control, and direct text input from the mobile keyboard to input fields on the TV.
- **Ad & Popup Blocker**: Content blocker integrated into the TV WebView to filter out invasive ad requests and prevent malicious popup windows.
- **ExoPlayer Native Stream Extractor**: Automatically detects video streams (`.mp4`, `.m3u8`, etc.) within pages, extracts them, and boots a full-screen native `Media3 ExoPlayer` on the TV. You can control play, pause, seek, and volume directly from the mobile remote.
- **Dynamic Device Detection**: Runs in a single unified `:app` module. The app automatically detects if it is running on a Television or Phone on launch, and mounts the appropriate UI and services.
- **Zero-Cloud Dependency**: Runs entirely on your local network using multicast DNS (mDNS) / Network Service Discovery (NSD) and local WebSockets via an embedded Ktor server on the TV.

---

## Architecture

TelePort is structured as a single-module Android project (`:app`) to streamline dependency management and shared protocols:

```
TelePort (Root)
 ├── gradle/
 ├── app/
 │    ├── src/main/
 │    │    ├── AndroidManifest.xml (Dual launcher & permissions)
 │    │    └── java/com/teleport/app/
 │    │         ├── MainActivity.kt (Runtime device detection)
 │    │         ├── protocol/
 │    │         │    └── Protocol.kt (Shared JSON WebSocket message contracts)
 │    │         ├── tv/
 │    │         │    ├── TvActivityContent.kt (TV layout & QR connection screen)
 │    │         │    ├── browser/
 │    │         │    │    ├── TabManager.kt (WebView controller & cursor engine)
 │    │         │    │    └── AdBlocker.kt (Domain-based request blocker)
 │    │         │    ├── player/
 │    │         │    │    └── NativePlayerActivity.kt (Native Media3 ExoPlayer)
 │    │         │    └── server/
 │    │         │         ├── LocalServerService.kt (Foreground Ktor server service)
 │    │         │         ├── NsdPublisher.kt (mDNS service registration)
 │    │         │         └── TvEventBus.kt (StateFlow event bus)
 │    │         └── mobile/
 │    │              ├── MobileRemoteScreen.kt (Remote Touchpad, D-pad, and tabs UI)
 │    │              ├── connection/
 │    │              │    └── TvConnectionManager.kt (WebSocket client coordinator)
 │    │              ├── nsd/
 │    │              │    └── NsdHelper.kt (mDNS service discovery helper)
 │    │              └── sensors/
 │    │                   └── GyroSensorTracker.kt (Pitch & yaw sensor stream)
```

---

## Local Development & Emulators

Since the Android TV and Phone emulators on a development machine run on isolated virtual networks, they cannot discover each other via mDNS out-of-the-box. You can bridge them using ADB port forwarding:

### 1. Boot up the Emulators
Ensure you have an Android TV AVD and an Android Phone AVD running.

### 2. Forward the Ports
Run the following commands in your terminal to allow the Phone to connect to the TV's embedded Ktor server on port `8080` via `127.0.0.1`:
```bash
# Forward port 8080 on the TV emulator to host machine port 8080
adb -s <tv-emulator-id> forward tcp:8080 tcp:8080

# Reverse port 8080 from the Phone emulator to host machine port 8080
adb -s <phone-emulator-id> reverse tcp:8080 tcp:8080
```
*(If you only have two emulators, their IDs are typically `emulator-5556` for the TV and `emulator-5554` for the Phone).*

### 3. Build & Install
Build and install the application debug build on both devices:
```bash
./gradlew installDebug
```

### 4. Start the Application
Start the MainActivity on both emulators:
```bash
# Launch on Android TV
adb -s emulator-5556 shell am start -n com.teleport.app/.MainActivity

# Launch on Android Phone
adb -s emulator-5554 shell am start -n com.teleport.app/.MainActivity
```

---

## How to Connect on Real Devices

When running on real physical devices on the same Wi-Fi network:
1. Open the **TelePort** app on your TV. A QR code containing the TV's local IP address and port (e.g., `ws://192.168.1.100:8080`) will be displayed, alongside an automatic mDNS broadcaster.
2. Open the **TelePort** app on your Mobile Phone.
3. The phone will automatically discover the TV using mDNS. If discovery is blocked by your router, tap the **QR Scanner** button on the phone and scan the TV screen's QR code to instantly pair.
