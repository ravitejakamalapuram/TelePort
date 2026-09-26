#!/usr/bin/env python3
"""Compose the Google Play feature graphic (1024x500) from the existing brand assets.

Google Play requires an exact 1024x500 JPG/PNG "feature graphic" for the store
listing header. assets/logo.png and assets/banner.png are both 1024x1024
icon-shaped art, so neither is usable as-is; this script builds a native
2.048:1 graphic using the app icon from logo.png and the brand palette / copy
sampled from banner.png.
"""
import os
import sys

def check_pillow():
    try:
        from PIL import Image, ImageDraw, ImageFont, ImageFilter
        return Image, ImageDraw, ImageFont, ImageFilter
    except ImportError:
        print("Pillow library is not installed. Installing it via pip...")
        import subprocess
        subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
        from PIL import Image, ImageDraw, ImageFont, ImageFilter
        return Image, ImageDraw, ImageFont, ImageFilter

FEATURE_WIDTH = 1024
FEATURE_HEIGHT = 500

# Brand palette sampled from assets/banner.png / the in-app ThemeTokens dark theme.
BG_TOP = (13, 15, 22)
BG_BOTTOM = (24, 18, 40)
GLOW_CYAN = (0, 223, 216)
GLOW_PURPLE = (124, 58, 237)
TEXT_MAIN = (255, 255, 255)
TEXT_SUB = (158, 162, 176)

FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"


def build_background(Image, ImageDraw, ImageFilter):
    bg = Image.new("RGB", (FEATURE_WIDTH, FEATURE_HEIGHT))
    pixels = bg.load()
    for y in range(FEATURE_HEIGHT):
        t = y / FEATURE_HEIGHT
        r = int(BG_TOP[0] + (BG_BOTTOM[0] - BG_TOP[0]) * t)
        g = int(BG_TOP[1] + (BG_BOTTOM[1] - BG_TOP[1]) * t)
        b = int(BG_TOP[2] + (BG_BOTTOM[2] - BG_TOP[2]) * t)
        for x in range(FEATURE_WIDTH):
            pixels[x, y] = (r, g, b)

    glow_layer = Image.new("RGB", (FEATURE_WIDTH, FEATURE_HEIGHT), (0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)
    glow_draw.ellipse((-200, -260, 620, 460), fill=GLOW_PURPLE)
    glow_draw.ellipse((600, 40, 1300, 620), fill=GLOW_CYAN)
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(140))

    return Image.blend(bg, glow_layer, alpha=0.35)


def paste_logo(Image, ImageDraw, ImageFilter, canvas, logo_path):
    logo = Image.open(logo_path).convert("RGBA")
    size = 360
    logo = logo.resize((size, size), Image.Resampling.LANCZOS)

    # logo.png's flat corner background shows up as a hard box against the
    # gradient backdrop, so feather a soft rounded mask over the icon glyph
    # (which only occupies the center of the source image) to blend it in.
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    inset = int(size * 0.05)
    mask_draw.rounded_rectangle(
        [inset, inset, size - inset, size - inset],
        radius=int(size * 0.22),
        fill=255,
    )
    mask = mask.filter(ImageFilter.GaussianBlur(size * 0.04))
    logo.putalpha(mask)

    x = 70
    y = (FEATURE_HEIGHT - size) // 2
    canvas.paste(logo, (x, y), logo)
    return x + size


def fit_text(ImageFont, draw, text, font_path, start_size, max_width, min_size=18):
    size = start_size
    while size > min_size:
        font = ImageFont.truetype(font_path, size)
        if draw.textlength(text, font=font) <= max_width:
            return font
        size -= 2
    return ImageFont.truetype(font_path, min_size)


def draw_copy(Image, ImageDraw, ImageFont, canvas, text_left):
    draw = ImageDraw.Draw(canvas)
    max_width = FEATURE_WIDTH - text_left - 50 - 40

    title_font = ImageFont.truetype(FONT_BOLD, 92)
    tagline_font = fit_text(ImageFont, draw, "TV Browser & Mobile Remote", FONT_BOLD, 34, max_width)
    benefit_font = fit_text(ImageFont, draw, "No cables. No extra remote.", FONT_REGULAR, 32, max_width)

    text_x = text_left + 50
    title_y = 168
    draw.text((text_x, title_y), "TelePort", font=title_font, fill=TEXT_MAIN)
    draw.text((text_x, title_y + 108), "TV Browser & Mobile Remote", font=tagline_font, fill=GLOW_CYAN)
    draw.text((text_x, title_y + 156), "No cables. No extra remote.", font=benefit_font, fill=TEXT_SUB)


def main():
    Image, ImageDraw, ImageFont, ImageFilter = check_pillow()

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    logo_path = os.path.join(project_root, "assets", "logo.png")

    if not os.path.exists(logo_path):
        print(f"Error: Base logo file not found at {logo_path}")
        sys.exit(1)

    canvas = build_background(Image, ImageDraw, ImageFilter)
    logo_right_edge = paste_logo(Image, ImageDraw, ImageFilter, canvas, logo_path)
    draw_copy(Image, ImageDraw, ImageFont, canvas, logo_right_edge)

    assert canvas.size == (FEATURE_WIDTH, FEATURE_HEIGHT)

    out_dir = os.path.join(project_root, "docs", "screenshots", "play_store")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "feature_graphic_1024x500.png")
    canvas.save(out_path, "PNG")
    print(f"Saved feature graphic: {out_path} ({canvas.size[0]}x{canvas.size[1]})")


if __name__ == "__main__":
    main()
