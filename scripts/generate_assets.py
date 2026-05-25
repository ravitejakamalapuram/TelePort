#!/usr/bin/env python3
import os
import sys

def check_pillow():
    try:
        from PIL import Image, ImageDraw, ImageOps
        return Image, ImageDraw, ImageOps
    except ImportError:
        print("Pillow library is not installed. Installing it via pip...")
        import subprocess
        subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
        from PIL import Image, ImageDraw, ImageOps
        return Image, ImageDraw, ImageOps

def create_circular_mask(image, size):
    Image, ImageDraw, ImageOps = check_pillow()
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    
    # Scale input image
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    
    # Apply circular mask
    output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    output.paste(resized, (0, 0), mask=mask)
    return output

def main():
    Image, ImageDraw, ImageOps = check_pillow()
    
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    logo_path = os.path.join(project_root, "assets", "logo.png")
    banner_path = os.path.join(project_root, "assets", "banner.png")
    res_dir = os.path.join(project_root, "app", "src", "main", "res")
    
    if not os.path.exists(logo_path):
        print(f"Error: Base logo file not found at {logo_path}")
        sys.exit(1)
        
    if not os.path.exists(banner_path):
        print(f"Error: Base banner file not found at {banner_path}")
        sys.exit(1)
        
    print(f"Loading base logo from: {logo_path}")
    logo_img = Image.open(logo_path)
    
    # Android Mipmap launcher icon target sizes
    mipmap_configs = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    for folder, size in mipmap_configs.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # 1. Standard square launcher icon
        launcher_img = logo_img.resize((size, size), Image.Resampling.LANCZOS)
        launcher_output = os.path.join(folder_path, "ic_launcher.png")
        launcher_img.save(launcher_output, "PNG")
        print(f"Saved: {launcher_output} ({size}x{size})")
        
        # 2. Round launcher icon
        round_img = create_circular_mask(logo_img, size)
        round_output = os.path.join(folder_path, "ic_launcher_round.png")
        round_img.save(round_output, "PNG")
        print(f"Saved: {round_output} ({size}x{size})")
        
    # Android TV Banner (Leanback launcher banner)
    # Required size: 320x180 px (placed in drawable-xhdpi)
    print(f"Loading base banner from: {banner_path}")
    banner_img = Image.open(banner_path)
    drawable_xhdpi = os.path.join(res_dir, "drawable-xhdpi")
    os.makedirs(drawable_xhdpi, exist_ok=True)
    
    banner_resized = banner_img.resize((320, 180), Image.Resampling.LANCZOS)
    banner_output = os.path.join(drawable_xhdpi, "ic_banner.png")
    banner_resized.save(banner_output, "PNG")
    print(f"Saved Android TV Banner: {banner_output} (320x180)")

    # 3. Chrome Extension Icons
    chrome_icons_dir = os.path.join(project_root, "chrome-extension", "icons")
    os.makedirs(chrome_icons_dir, exist_ok=True)
    chrome_configs = {
        "icon-16.png": 16,
        "icon-48.png": 48,
        "icon-128.png": 128
    }
    for filename, size in chrome_configs.items():
        icon_resized = logo_img.resize((size, size), Image.Resampling.LANCZOS)
        icon_output = os.path.join(chrome_icons_dir, filename)
        icon_resized.save(icon_output, "PNG")
        print(f"Saved Chrome Extension Icon: {icon_output} ({size}x{size})")


if __name__ == "__main__":
    main()
