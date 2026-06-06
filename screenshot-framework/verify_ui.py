#!/usr/bin/env python3
import os
import sys
import math
import shutil

def check_pillow():
    try:
        from PIL import Image, ImageChops, ImageDraw
        return Image, ImageChops, ImageDraw
    except ImportError:
        print("Pillow is not installed. Installing it via pip...")
        import subprocess
        subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
        from PIL import Image, ImageChops, ImageDraw
        return Image, ImageChops, ImageDraw

def compare_images(img1_path, img2_path, diff_path):
    Image, ImageChops, ImageDraw = check_pillow()
    
    img1 = Image.open(img1_path).convert('RGB')
    img2 = Image.open(img2_path).convert('RGB')
    
    if img1.size != img2.size:
        return False, f"Dimensions differ: baseline={img1.size}, current={img2.size}"
        
    diff = ImageChops.difference(img1, img2)
    h = diff.histogram()
    
    # Calculate root-mean-square (RMS) difference
    total_pixels = float(img1.size[0] * img1.size[1]) * 3
    rms = math.sqrt(sum(value * ((idx % 256) ** 2) for idx, value in enumerate(h)) / total_pixels)
    
    # Threshold for comparison (RMS > 0.05 indicates visual difference)
    if rms > 0.1:
        # Create a red highlighting mask for changed pixels
        # Pixels with color difference > 5 are marked as changed
        diff_mask = diff.convert('L').point(lambda x: 255 if x > 5 else 0)
        highlight = Image.new('RGB', img1.size, (255, 0, 0))
        blended = Image.composite(highlight, img1, diff_mask)
        
        # Combine side-by-side: [Baseline]  [Diff Highlight]  [Current]
        spacing = 20
        label_height = 30
        canvas_width = img1.size[0] * 3 + spacing * 4
        canvas_height = img1.size[1] + spacing * 2 + label_height
        
        sxs = Image.new('RGB', (canvas_width, canvas_height), (20, 20, 28))
        sxs.paste(img1, (spacing, spacing))
        sxs.paste(blended, (img1.size[0] + spacing * 2, spacing))
        sxs.paste(img2, (img1.size[0] * 2 + spacing * 3, spacing))
        
        draw = ImageDraw.Draw(sxs)
        draw.text((spacing, img1.size[1] + spacing + 5), "BASELINE (GOLDEN)", fill=(158, 162, 176))
        draw.text((img1.size[0] + spacing * 2, img1.size[1] + spacing + 5), "DIFF HIGHLIGHT (RED)", fill=(255, 59, 48))
        draw.text((img1.size[0] * 2 + spacing * 3, img1.size[1] + spacing + 5), "CURRENT PR BRANCH", fill=(0, 223, 216))
        
        os.makedirs(os.path.dirname(diff_path), exist_ok=True)
        sxs.save(diff_path)
        return False, f"Mismatch (RMS={rms:.4f})"
        
    return True, "Match"

def main():
    Image, _, _ = check_pillow()
    
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    screenshots_dir = os.path.join(project_root, "docs", "screenshots")
    baseline_dir = os.path.join(screenshots_dir, "baseline")
    current_dir = os.path.join(screenshots_dir, "current")
    diff_dir = os.path.join(screenshots_dir, "diff")
    
    promote = "--promote" in sys.argv or "PROMOTE_BASELINES" in os.environ
    
    if promote:
        if not os.path.exists(current_dir):
            print("Error: No current screenshots found to promote.")
            sys.exit(1)
        print(f"Promoting current screenshots from {current_dir} to baseline...")
        os.makedirs(baseline_dir, exist_ok=True)
        for filename in os.listdir(current_dir):
            if filename.endswith(".png"):
                shutil.copy(os.path.join(current_dir, filename), os.path.join(baseline_dir, filename))
                print(f"Promoted: {filename}")
        print("Success! Baselines updated.")
        sys.exit(0)
        
    if not os.path.exists(current_dir):
        print("Error: No current screenshots found. Run tests first.")
        sys.exit(1)
        
    if not os.path.exists(baseline_dir):
        print(f"Warning: Baseline folder missing at {baseline_dir}. Creating empty baseline.")
        os.makedirs(baseline_dir, exist_ok=True)
        
    mismatches = []
    new_images = []
    
    print("-" * 80)
    print(f"{'Screenshot Name':<40} | {'Status':<15} | {'Details':<20}")
    print("-" * 80)
    
    for filename in sorted(os.listdir(current_dir)):
        if not filename.endswith(".png"):
            continue
            
        current_path = os.path.join(current_dir, filename)
        baseline_path = os.path.join(baseline_dir, filename)
        diff_path = os.path.join(diff_dir, filename)
        
        if not os.path.exists(baseline_path):
            new_images.append(filename)
            print(f"{filename:<40} | {'NEW':<15} | No baseline image found")
            # Automatically seed baseline if it is completely new to bootstrap the system
            shutil.copy(current_path, baseline_path)
            continue
            
        is_match, detail = compare_images(baseline_path, current_path, diff_path)
        if is_match:
            print(f"{filename:<40} | {'MATCH':<15} | {detail}")
            # Clean up old diff if it passed now
            if os.path.exists(diff_path):
                os.remove(diff_path)
        else:
            mismatches.append((filename, detail))
            print(f"{filename:<40} | {'MISMATCH':<15} | {detail}")
            
    print("-" * 80)
    
    if mismatches:
        print(f"\n❌ Failure: {len(mismatches)} visual regression mismatches detected!")
        print(f"Diff side-by-side images saved in: {diff_dir}")
        print("To approve and promote these changes to baseline, run:")
        print("  ./scripts/dev.sh update-baselines")
        sys.exit(1)
        
    if new_images:
        print(f"\n⚠️ Note: {len(new_images)} new screenshots were auto-seeded into baseline.")
        
    print("\n✅ Success: All visual regression tests match baseline images!")
    sys.exit(0)

if __name__ == "__main__":
    main()
