import os
import subprocess
from PIL import Image, ImageDraw, ImageFont

def generate_video():
    screenshots_dir = "docs/screenshots"
    output_dir = "docs"
    temp_frames_dir = "temp_frames"
    os.makedirs(temp_frames_dir, exist_ok=True)
    os.makedirs(output_dir, exist_ok=True)

    # 1. Load screenshots
    tv_pairing = Image.open(os.path.join(screenshots_dir, "tv_pairing_screen.png"))
    mob_pairing = Image.open(os.path.join(screenshots_dir, "mobile_pairing_screen.png"))
    mob_trackpad = Image.open(os.path.join(screenshots_dir, "mobile_controller_trackpad.png"))
    mob_dpad = Image.open(os.path.join(screenshots_dir, "mobile_controller_dpad.png"))
    mob_tabs = Image.open(os.path.join(screenshots_dir, "mobile_controller_tabs.png"))

    # Canvas dimensions
    width, height = 1280, 720
    fps = 10
    total_seconds = 15
    total_frames = fps * total_seconds

    # Scale assets for placement
    # TV size on canvas: 680x382 (aspect ratio 16:9)
    tv_w, tv_h = 680, 382
    # Mobile size on canvas: 216x481 (aspect ratio 359:799)
    mob_w, mob_h = 216, 481

    # Try to load a nice system font, fallback to default if not found
    font_paths = [
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/System/Library/Fonts/Keyboard.ttf"
    ]
    title_font = None
    desc_font = None
    small_font = None

    for path in font_paths:
        if os.path.exists(path):
            try:
                title_font = ImageFont.truetype(path, 32)
                desc_font = ImageFont.truetype(path, 20)
                small_font = ImageFont.truetype(path, 14)
                break
            except Exception:
                continue

    if not title_font:
        # Fallback if no TTF font is found
        title_font = ImageFont.load_default()
        desc_font = ImageFont.load_default()
        small_font = ImageFont.load_default()

    print("Generating frames...")

    # Helper to draw a mock TV browser screen
    def draw_mock_tv_browser(url="https://www.google.com", search_text="", show_cursor=False, cursor_pos=(0, 0), scroll_y=0):
        # Create a TV screen-sized image
        img = Image.new("RGB", (960, 540), "#121212")
        draw = ImageDraw.Draw(img)
        
        # Draw browser header bar
        draw.rectangle([0, 0, 960, 50], fill="#1E1E1E")
        # Draw address bar
        draw.rectangle([100, 10, 860, 40], fill="#0D0E12", outline="#2E2E2E", width=1)
        draw.text((120, 16), url, fill="#94A3B8", font=small_font)
        
        # Draw browser content area
        draw.rectangle([0, 50, 960, 540], fill="#F8FAFC" if "google" in url else "#0D0E12")
        
        if "google" in url:
            # Draw mock Google Search Page
            draw.text((380, 150 - scroll_y), "Google", fill="#4285F4", font=title_font)
            # Draw search box
            draw.rectangle([280, 220 - scroll_y, 680, 260 - scroll_y], fill="#FFFFFF", outline="#E2E8F0", width=1)
            draw.text((300, 228 - scroll_y), search_text, fill="#0F172A", font=desc_font)
            
            # Draw mock search results if search text is typed
            if search_text:
                draw.text((280, 290 - scroll_y), "About 1,240,000 results", fill="#64748B", font=small_font)
                
                # Result 1
                draw.text((280, 320 - scroll_y), "TelePort Android TV & Mobile Link", fill="#1A0DAB", font=desc_font)
                draw.text((280, 345 - scroll_y), "https://github.com/ravitejakamalapuram/TelePort", fill="#006621", font=small_font)
                draw.text((280, 365 - scroll_y), "TelePort allows you to cast your phone screen and control your TV.", fill="#545454", font=small_font)
        else:
            # Video Player screen (e.g. YouTube)
            draw.rectangle([100, 100, 860, 480], fill="#000000", outline="#6200EE", width=2)
            # Draw Red Video Player Icon
            draw.polygon([(460, 250), (460, 330), (520, 290)], fill="#FF0000")
            draw.text((380, 420), "Streaming: Rick Astley - Never Gonna Give You Up", fill="#FFFFFF", font=desc_font)

        # Draw remote pointer if enabled
        if show_cursor:
            draw.ellipse([cursor_pos[0]-10, cursor_pos[1]-10, cursor_pos[0]+10, cursor_pos[1]+10], fill="#00E676")
            
        return img

    for frame_idx in range(total_frames):
        # Create blank 1280x720 canvas
        canvas = Image.new("RGB", (width, height), "#0D0E12")
        draw = ImageDraw.Draw(canvas)

        # Draw branding gradient bar on top
        draw.rectangle([0, 0, width, 5], fill="#6200EE")

        # Determine time-based phase
        # 1. Pairing (0-3s)
        if frame_idx < 30:
            step_title = "1. Device Discovery & Pairing"
            step_desc = "Start TelePort on TV to see pairing info. Open Mobile App to search."
            current_tv = tv_pairing
            current_mob = mob_pairing
            
        # 2. Connection Established (3-6s)
        elif frame_idx < 60:
            step_title = "2. Secure Local Connection"
            step_desc = "Tap on the discovered TV. Connected over local Wi-Fi via WebSockets."
            current_tv = tv_pairing
            current_mob = mob_pairing
            
            # Animate a "Connecting..." to "Connected" message
            progress = (frame_idx - 30) / 30.0
            if progress < 0.5:
                draw.text((900, 600), "Connecting...", fill="#03DAC6", font=desc_font)
            else:
                draw.text((900, 600), "Connected!", fill="#00E676", font=desc_font)

        # 3. Trackpad / Cursor Control (6-9s)
        elif frame_idx < 90:
            step_title = "3. Trackpad & Air Mouse Control"
            step_desc = "Swipe on phone trackpad to move the TV pointer. Tap to click."
            current_mob = mob_trackpad
            
            # Animate cursor moving on TV browser
            progress = (frame_idx - 60) / 30.0
            # Cursor moves in a diagonal arc
            cx = int(350 + progress * 400)
            cy = int(200 + progress * 200)
            current_tv = draw_mock_tv_browser(url="https://www.google.com", show_cursor=True, cursor_pos=(cx, cy))

        # 4. Dpad Tab / Remote Scrolling (9-12s)
        elif frame_idx < 120:
            step_title = "4. Remote Navigation & Typing"
            step_desc = "D-Pad buttons let you scroll pages or click elements easily."
            current_mob = mob_dpad
            
            # Animate Google search and scroll
            progress = (frame_idx - 90) / 30.0
            search_text = "TelePort GitHub"
            # Simulate typing
            chars_to_show = int(len(search_text) * min(progress * 1.5, 1.0))
            current_search = search_text[:chars_to_show]
            
            # Scroll down the results in the last half of the slide
            scroll = 0
            if progress > 0.6:
                scroll = int((progress - 0.6) * 200)
                
            current_tv = draw_mock_tv_browser(url="https://www.google.com", search_text=current_search, scroll_y=scroll)

        # 5. Tabs Tab / Video Stream Cast (12-15s)
        else:
            step_title = "5. Tabs Manager & Media Casting"
            step_desc = "Detect streamable media and cast video directly to TV's Native Player."
            current_mob = mob_tabs
            
            # TV switches to fullscreen media player
            current_tv = draw_mock_tv_browser(url="https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        # Resize and place TV Screen
        tv_scaled = current_tv.resize((tv_w, tv_h), Image.Resampling.LANCZOS)
        canvas.paste(tv_scaled, (40, 160))
        
        # Draw frame around TV Screen (giving it a TV bezel feel)
        draw.rectangle([35, 155, 40 + tv_w + 5, 160 + tv_h + 5], outline="#2E2E2E", width=5)
        # Stand
        draw.rectangle([340, 160 + tv_h + 5, 420, 160 + tv_h + 25], fill="#1E1E1E")
        # Base
        draw.ellipse([300, 160 + tv_h + 20, 460, 160 + tv_h + 30], fill="#1E1E1E")
        
        # Label above TV
        draw.text((40, 125), "Android TV Client", fill="#94A3B8", font=small_font)

        # Resize and place Mobile Screen
        mob_scaled = current_mob.resize((mob_w, mob_h), Image.Resampling.LANCZOS)
        canvas.paste(mob_scaled, (900, 120))
        
        # Draw Phone Bezel
        draw.rectangle([895, 115, 900 + mob_w + 5, 120 + mob_h + 5], outline="#1E1E1E", width=5)
        # Label above Mobile
        draw.text((900, 85), "Mobile Remote", fill="#94A3B8", font=small_font)

        # Draw Step Title and Description
        draw.text((40, 30), "TelePort Demo", fill="#03DAC6", font=small_font)
        draw.text((40, 50), step_title, fill="#FFFFFF", font=title_font)
        draw.text((40, 600), step_desc, fill="#94A3B8", font=desc_font)
        
        # Draw watermark/credit at the bottom
        draw.text((40, 670), "Powered by TelePort - Local Connectivity Utility", fill="#2E2E2E", font=small_font)

        # Save frame
        frame_name = f"frame_{frame_idx:04d}.png"
        canvas.save(os.path.join(temp_frames_dir, frame_name))

    print("Frames generation completed.")
    
    # 2. Compile to MP4 using ffmpeg
    output_video_path = os.path.join(output_dir, "demo_video.mp4")
    print(f"Stitching frames into video at {output_video_path}...")
    
    # Ffmpeg command: stitch pngs at 10fps, encode to H.264 mp4, compatible with web play
    cmd = [
        "/opt/homebrew/bin/ffmpeg",
        "-y",
        "-r", str(fps),
        "-i", os.path.join(temp_frames_dir, "frame_%04d.png"),
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        "-vf", "scale=1280:720",
        output_video_path
    ]
    
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if result.returncode == 0:
            print("Successfully compiled demo_video.mp4!")
        else:
            print("Error running ffmpeg:")
            print(result.stderr)
    except Exception as e:
        print("Failed to run ffmpeg subprocess:", e)

    # 3. Clean up temp frames
    print("Cleaning up temp frames...")
    for f in os.listdir(temp_frames_dir):
        os.remove(os.path.join(temp_frames_dir, f))
    os.rmdir(temp_frames_dir)
    print("Cleanup completed.")

if __name__ == "__main__":
    generate_video()
