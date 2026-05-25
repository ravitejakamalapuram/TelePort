# TelePort Development Guidelines & Standards

This document establishes the platform-agnostic development standards, design systems, and automated validation requirements for the TelePort repository. All human developers and AI coding assistants (e.g. Copilot, Cursor, JetBrains AI, Neovim LLMs) must follow these rules.

---

## 1. Visual Identity & Brand Design Tokens

All clients (Android, Web Remote, Chrome Extension) must adhere to the design system to ensure visual consistency. Do not use ad-hoc colors or styling.

| Token Name | Value | Purpose |
| :--- | :--- | :--- |
| `color-bg` | `#0d0d11` | Primary background color for all dark-theme screens. |
| `color-card-bg` | `rgba(26, 26, 36, 0.55)` | Transparent base for glassmorphism card surfaces. |
| `color-border` | `rgba(255, 255, 255, 0.08)` | Subtle border for panels and inputs. |
| `color-primary` | `#7928ca` | Violet. Used for primary buttons, focus highlights, and gradients. |
| `color-accent` | `#00dfd8` | Neon cyan/aqua. Used for active states, logos, cursors, and glow details. |
| `color-success` | `#02c39a` | Emerald. Used for "Connected" badges. |
| `color-error` | `#ff3b30` | Red. Used for "Disconnected" status. |

### Visual Layout Rules
- **Micro-Animations**: All interactive buttons must support scale styling on hover (`scale(1.03)`) and click compression (`scale(0.97)`) using smooth cubic-bezier transitions (`transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1)`).
- **WebView Dark Mode Inversion**: Invert backgrounds via `filter: invert(1) hue-rotate(180deg)`. Media tags (`img, video, canvas, iframe`) must be double-inverted to preserve original colors.

---

## 2. Asset & Icon Rules (Single Source of Truth)

- **Source Files**: 
  - Logo source: `/assets/logo.png`
  - Leanback Banner source: `/assets/banner.png`
- **Rule**:
  - **NEVER** manually crop, scale, or copy-paste icon assets or Android TV launcher banners.
  - Always update the source assets in `/assets` and run the asset generation command to compile outputs:
    ```bash
    ./scripts/dev.sh assets
    ```

---

## 3. Automation Task Runner CLI

The project includes an automated task runner (`scripts/dev.sh`) to automate local workflows:

| Command | Action |
| :--- | :--- |
| `./scripts/dev.sh setup` | Audits system requirements and installs pip dependencies (e.g. Pillow). |
| `./scripts/dev.sh assets` | Compiles Android mipmaps, TV banners, and Chrome Extension icons. |
| `./scripts/dev.sh test` | Validates manifest structures, verifies icon paths exist, and runs unit tests. |
| `./scripts/dev.sh build` | Packages local extension zips and compiles debug Android APKs. |
| `./scripts/dev.sh mock-crash` | Runs a dry-run local simulation of the Firebase crash auto-fix pipeline. |

---

## 4. Verification Gate

Before submitting any Pull Request or staging commits:
1. You **must** run `./scripts/dev.sh test` to ensure that all local assets are verified and Kotlin unit tests pass.
2. Run `./scripts/dev.sh build` to verify the code packages successfully.

---

## 5. Security Guidelines

- **JS Injection Avoidance**:
  - Never sanitize TV JavaScript string parameters using simple string replacement (e.g. `replace("'", "\\'")`).
  - Always use a proper JSON serializer (like `org.json.JSONObject.quote(text)` in Kotlin) to securely encode input strings into valid JS string literals.
- **XSS Prevention**:
  - Sanitize all strings before writing to `innerHTML` by escaping HTML entities (`&`, `<`, `>`, `"`, `'`).
