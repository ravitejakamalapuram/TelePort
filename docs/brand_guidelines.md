# TelePort Design System & Brand Guidelines

This document serves as the **Single Source of Truth** for the visual identity, UI themes, and assets of the TelePort ecosystem (Android TV app, Companion Mobile app, Zero-Install Web Remote, and Chrome Extension). All future UI development, component styles, and marketing assets must adhere to these rules.

---

## 1. Visual Identity & Color Tokens

TelePort uses a futuristic, premium dark theme accented by neon gradients and subtle glassmorphic elements. 

| Token Name | Value | Purpose |
| :--- | :--- | :--- |
| `color-bg` | `#0d0d11` | Primary background color for all dark-theme screens. |
| `color-card-bg` | `rgba(26, 26, 36, 0.55)` | Transparent base for glassmorphism card surfaces. |
| `color-border` | `rgba(255, 255, 255, 0.08)` | Subtle border for panels and inputs. |
| `color-primary` | `#7928ca` | Dark violet. Used for primary buttons, focus highlights, and gradients. |
| `color-accent` | `#00dfd8` | Neon cyan/aqua. Used for active states, logos, cursors, and glow details. |
| `color-success` | `#02c39a` | Emerald. Used for "Connected" badges and positive confirmations. |
| `color-error` | `#ff3b30` | Red. Used for "Disconnected" badges and warning inputs. |
| `color-text-main` | `#ffffff` | Primary text and high-contrast labels. |
| `color-text-sub` | `#9ea2b0` | Muted secondary descriptions and placeholders. |

### Gradient Configuration
- **Accent Gradient**: Linear gradient from `color-accent` to `#00b0ff` (Neon Cyan to Sky Blue) at `135deg`.
- **Primary Gradient**: Linear gradient from `color-primary` to `#512da8` (Deep Violet to Royal Blue) at `135deg`.
- **Glassmorphic Card Effect**:
  ```css
  background: var(--color-card-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--color-border);
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);
  ```

---

## 2. Typography & Fonts

To maintain clean and readable displays, avoid generic browser serif styles.

- **Primary Heading Font**: **Outfit** (Weights: 300, 400, 500, 700). Used for titles, logos, and headers.
- **Secondary Body Font**: **Inter** or standard system sans-serif fallback. Used for buttons, descriptions, and settings.
- **Typography Scale**:
  - Main titles (Logo headers): `18px` to `20px` bold.
  - Section headers: `13px` uppercase, letter-spacing `1px`.
  - Body text: `14px` regular.
  - Subtext/Captions: `11px` regular.

---

## 3. UI Component Standards

### Tactile Interactions
- **Hover Transitions**: All interactive elements (buttons, inputs, sliders) must use `transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1)`.
- **Micro-Animations**:
  - Buttons on hover: scale `1.03` or increase background brightness.
  - Buttons on active click: scale down `0.97` to provide tactile compression feedback.
- **Glow Effects**: Primary or active inputs should use `box-shadow: 0 0 8px var(--glow-color)`.

### WebView & Remote Smart Dark Mode
- When dark mode is active on the TV, it uses CSS filters to invert backgrounds:
  ```css
  filter: invert(1) hue-rotate(180deg);
  ```
- Media elements (`img`, `video`, `canvas`, `iframe`) must be double-inverted to retain their correct colors:
  ```css
  filter: invert(1) hue-rotate(180deg);
  ```

---

## 4. Asset Generation & Dimensions

All app store assets, mipmaps, and extension icons must be generated from a single source directory (`/assets`) using the unified asset automation script (`scripts/generate_assets.py`).

### Source Assets
- **Logo**: [assets/logo.png](file:///Users/rkamalapuram/git-personal/TelePort/assets/logo.png) - High-resolution logo file (minimum 512x512px, transparent background).
- **Banner**: [assets/banner.png](file:///Users/rkamalapuram/git-personal/TelePort/assets/banner.png) - High-resolution leanback banner file (minimum 1920x1080px).

### Output Specifications (Automated)

| Target Client | Output Path | Resolution | Description |
| :--- | :--- | :--- | :--- |
| **Android Phone** | `app/src/main/res/mipmap-xxxx/ic_launcher.png` | Various | Standard square app launcher icon. |
| **Android Phone** | `app/src/main/res/mipmap-xxxx/ic_launcher_round.png` | Various | Circle-masked app launcher icon. |
| **Android TV** | `app/src/main/res/drawable-xhdpi/ic_banner.png` | 320x180 px | Leanback launcher home banner. |
| **Chrome Extension** | `chrome-extension/icons/icon-16.png` | 16x16 px | Extension settings / taskbar icon. |
| **Chrome Extension** | `chrome-extension/icons/icon-48.png` | 48x48 px | Extension manager dashboard icon. |
| **Chrome Extension** | `chrome-extension/icons/icon-128.png` | 128x128 px | Web Store / installation page icon. |

No assets should be hand-resized or copy-pasted manually. Run the compiler script:
```bash
python scripts/generate_assets.py
```
