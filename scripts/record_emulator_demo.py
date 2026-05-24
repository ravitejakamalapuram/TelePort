import os
import sys
import time
import subprocess

def run_cmd(args):
    result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return result.returncode, result.stdout, result.stderr

def detect_devices():
    _, stdout, _ = run_cmd(["adb", "devices"])
    devices = []
    for line in stdout.splitlines():
        if "device" in line and not "devices" in line:
            parts = line.split()
            if len(parts) > 0:
                devices.append(parts[0])
    
    tv_id = None
    phone_id = None
    
    for dev in devices:
        _, characteristics, _ = run_cmd(["adb", "-s", dev, "shell", "getprop", "ro.build.characteristics"])
        characteristics = characteristics.strip().lower()
        print(f"Device {dev} characteristics: '{characteristics}'")
        
        if "tv" in characteristics or "leanback" in characteristics:
            tv_id = dev
        else:
            # Fallback or check model if characteristics is empty
            _, model, _ = run_cmd(["adb", "-s", dev, "shell", "getprop", "ro.product.model"])
            model = model.strip().lower()
            print(f"Device {dev} model: '{model}'")
            if "tv" in model or "television" in model:
                tv_id = dev
            else:
                phone_id = dev
                
    # If we couldn't distinguish but have 2 devices, assign arbitrarily as fallback
    if len(devices) == 2 and (not tv_id or not phone_id):
        print("Warning: Could not automatically distinguish devices. Assigning based on ports.")
        # Usually 5554 is TV (Television_1080p) or phone depending on boot order
        tv_id = devices[0]
        phone_id = devices[1]
        
    return tv_id, phone_id

def wait_for_boot(device_id, timeout=90):
    start_time = time.time()
    print(f"Waiting for {device_id} to boot...")
    while time.time() - start_time < timeout:
        _, stdout, _ = run_cmd(["adb", "-s", device_id, "shell", "getprop", "sys.boot_completed"])
        if stdout.strip() == "1":
            print(f"{device_id} booted successfully!")
            return True
        time.sleep(2)
    print(f"Timeout waiting for {device_id} to boot.")
    return False

def record_demo():
    emulator_path = "/Users/rkamalapuram/Library/Android/sdk/emulator/emulator"
    
    # 1. Start Emulators
    print("Launching Television_1080p emulator...")
    tv_process = subprocess.Popen([
        emulator_path,
        "-avd", "Television_1080p",
        "-no-audio",
        "-no-window",
        "-gpu", "swiftshader_indirect"
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    # Give it a short moment before starting the second to avoid port collision issues
    time.sleep(5)
    
    print("Launching Pixel_7 emulator...")
    phone_process = subprocess.Popen([
        emulator_path,
        "-avd", "Pixel_7",
        "-no-audio",
        "-no-window",
        "-gpu", "swiftshader_indirect"
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # Wait for adb to recognize the devices
    print("Waiting for devices to connect to adb...")
    time.sleep(15)
    
    tv_id, phone_id = detect_devices()
    
    if not tv_id or not phone_id:
        print(f"Error: Could not detect both TV and Phone. Detected: TV={tv_id}, Phone={phone_id}")
        # Cleanup processes
        tv_process.terminate()
        phone_process.terminate()
        sys.exit(1)

    print(f"Detected TV Emulator: {tv_id}")
    print(f"Detected Phone Emulator: {phone_id}")

    # Wait for both to complete booting
    if not wait_for_boot(tv_id) or not wait_for_boot(phone_id):
        print("Error: One or both emulators failed to boot.")
        tv_process.terminate()
        phone_process.terminate()
        sys.exit(1)

    # 2. Setup Port Forwarding
    # Forward host port 8080 to TV emulator port 8080
    print("Setting up ADB port forwarding from host to TV...")
    run_cmd(["adb", "-s", tv_id, "forward", "tcp:8080", "tcp:8080"])

    # 3. Install debug APK
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"
    print(f"Installing app on TV ({tv_id})...")
    run_cmd(["adb", "-s", tv_id, "install", "-r", apk_path])
    
    print(f"Installing app on Phone ({phone_id})...")
    run_cmd(["adb", "-s", phone_id, "install", "-r", apk_path])

    # 4. Launch App
    print("Launching app on TV...")
    run_cmd(["adb", "-s", tv_id, "shell", "am", "start", "-n", "com.carfry369.teleport/.MainActivity"])
    
    print("Launching app on Phone...")
    run_cmd(["adb", "-s", phone_id, "shell", "am", "start", "-n", "com.carfry369.teleport/.MainActivity"])

    # Wait for apps to initialize
    time.sleep(5)

    # 5. Connect Phone to TV via manual IP (10.0.2.2)
    # Tapping manual IP text field on Pixel 7 (dimensions are typically 1080x2400)
    # The text field is located around X: 400, Y: 2100.
    # The "Go" button is around X: 950, Y: 2100.
    print("Simulating phone manual IP typing...")
    run_cmd(["adb", "-s", phone_id, "shell", "input", "tap", "400", "2100"])
    time.sleep(1)
    run_cmd(["adb", "-s", phone_id, "shell", "input", "text", "10.0.2.2"])
    time.sleep(1)
    
    # 6. Start Screen Recording on both devices (limit 15 seconds)
    print("Starting screen recording on both emulators...")
    tv_record_process = subprocess.Popen([
        "adb", "-s", tv_id, "shell", "screenrecord", "--size", "1280x720", "--time-limit", "15", "/sdcard/tv.mp4"
    ])
    phone_record_process = subprocess.Popen([
        "adb", "-s", phone_id, "shell", "screenrecord", "--size", "1080x2400", "--time-limit", "15", "/sdcard/phone.mp4"
    ])

    time.sleep(2)
    
    # Click "Go" button on phone to trigger pairing
    print("Clicking Go button to connect...")
    run_cmd(["adb", "-s", phone_id, "shell", "input", "tap", "950", "2100"])
    
    # Let it stay connected for 4 seconds
    time.sleep(4)
    
    # Switch to D-Pad tab on phone controller (Trackpad is tab 0, Dpad is tab 1, Tabs is tab 2)
    # Tab Row width is divided by 3. Tab 1 is in the middle: X: 540, Y: 330.
    print("Switching phone to D-Pad tab...")
    run_cmd(["adb", "-s", phone_id, "shell", "input", "tap", "540", "330"])
    time.sleep(3)
    
    # Click "OK" button on Dpad (centered around X: 540, Y: 1200)
    print("Simulating D-Pad OK click...")
    run_cmd(["adb", "-s", phone_id, "shell", "input", "tap", "540", "1200"])
    time.sleep(2)
    
    # Switch to Tabs tab (Tab 2 is on the right: X: 900, Y: 330)
    print("Switching phone to Tabs tab...")
    run_cmd(["adb", "-s", phone_id, "shell", "input", "tap", "900", "330"])
    time.sleep(3)

    # Wait for recording processes to finish
    print("Waiting for screen recordings to complete...")
    tv_record_process.wait()
    phone_record_process.wait()

    # 7. Pull recordings
    print("Pulling video recordings to host...")
    run_cmd(["adb", "-s", tv_id, "pull", "/sdcard/tv.mp4", "docs/tv.mp4"])
    run_cmd(["adb", "-s", phone_id, "pull", "/sdcard/phone.mp4", "docs/phone.mp4"])

    # Clean up emulator sdcard files
    run_cmd(["adb", "-s", tv_id, "shell", "rm", "/sdcard/tv.mp4"])
    run_cmd(["adb", "-s", phone_id, "shell", "rm", "/sdcard/phone.mp4"])

    # 8. Kill emulators
    print("Stopping emulators...")
    run_cmd(["adb", "-s", tv_id, "emu", "kill"])
    run_cmd(["adb", "-s", phone_id, "emu", "kill"])
    
    # Force kill processes just in case
    tv_process.terminate()
    phone_process.terminate()

    # 9. Stitch side-by-side using ffmpeg
    print("Stitching TV and Phone videos side-by-side...")
    output_video_path = "docs/demo_video.mp4"
    
    # Scale TV to height 720 (1280x720) and Phone to height 720 (324x720)
    # Combine them using hstack (total width 1604x720)
    # Pad to standard 1920x1080 canvas with dark background
    cmd = [
        "/opt/homebrew/bin/ffmpeg",
        "-y",
        "-i", "docs/tv.mp4",
        "-i", "docs/phone.mp4",
        "-filter_complex", 
        "[0:v]scale=1280:720[tv];[1:v]scale=324:720[phone];[tv][phone]hstack=inputs=2[stacked];[stacked]pad=1920:1080:(ow-iw)/2:(oh-ih)/2:color=0x0D0E12[v]",
        "-map", "[v]",
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        output_video_path
    ]
    
    ret, stdout, stderr = run_cmd(cmd)
    if ret == 0:
        print("Success! Generated side-by-side video from real emulators at docs/demo_video.mp4")
        # Clean up separate tv and phone files
        os.remove("docs/tv.mp4")
        os.remove("docs/phone.mp4")
    else:
        print("Failed to stitch videos with ffmpeg:")
        print(stderr)

if __name__ == "__main__":
    record_demo()
