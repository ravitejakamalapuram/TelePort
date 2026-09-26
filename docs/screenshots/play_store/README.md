# Play Store screenshot set — draft (POR-176)

Recaptured from the real app UI via the existing Robolectric screenshot pipeline
(`./gradlew :app:testDebugUnitTest --tests com.teleport.app.ExhaustiveScreenshotsTest`,
Option B "store asset" tests in `app/src/test/java/com/teleport/app/ExhaustiveScreenshotsTest.kt`).
Every pixel is a real render of shipped Compose UI; only the title/description
caption band and device bezel around it are drawn on top by `ScreenshotEngine.decorate()`.

Recommended Play Console upload order:

| # | File | Shows | Why here |
|---|------|-------|----------|
| 1 | `01_browse_multiple_tabs.png` | Phone remote's Tabs screen, two real open tabs | Leads with the core "browse the web on your TV" value prop, matches searches like "cast tabs to TV" |
| 2 | `02_trackpad_air_mouse.png` | Phone remote's Trackpad screen, clean free/unlocked state | Shows the cursor-control interaction; matches "TV remote from phone" |
| 3 | `03_tv_native_casting.png` | TV-side native video resolver/player | Proves the TV side of the experience, not just the phone |
| 4 | `04_dpad_keyboard_control.png` | Phone remote's D-Pad screen | Rounds out the control surface (menus, text entry) |
| 5 | `05_pairing_setup.png` | TV pairing/QR dashboard | Setup is reassuring, not the hook — moved from lead to last |
| — | `feature_graphic_1024x500.png` | Logo + wordmark on brand gradient | Play Console "Feature graphic" slot (exact 1024x500) |

## What changed from the previous live set (`docs/screenshots/*.png`)

- The old lead image was the bare QR pairing screen. It's now last in the
  recommended order, and 1–4 above lead with the actual product instead.
- `mobile_controller_trackpad.png` showed a "Mirror Phone Screen — PRO" badge,
  an "AD" badge on Air Mouse Mode, and "Watch ad to remove ads for 1 hour".
  `ENABLE_ADVERTISEMENTS` and `ENABLE_PAID_SUBSCRIPTIONS` are both `false` in
  `FeatureFlags.kt`, so the shipping app never shows any of that — the
  checked-in screenshot was simply stale. Regenerating from current source
  (`docs/screenshots/mobile_controller_trackpad.png`, and `02_trackpad_air_mouse.png`
  here) reproduces the real free/unlocked UI with no ads or paywall visible.
- `screenshot-framework/ScreenshotEngine.kt` (and its copy in
  `app/src/test/java/com/teleport/app/ScreenshotEngine.kt`) had a caption bug:
  long descriptions were drawn as a single centered line and ran off the edge
  of the canvas. Added word-wrapping so captions never clip.
- No 1024x500 feature graphic existed in the repo. `assets/logo.png` and
  `assets/banner.png` are both 1024x1024 icon-shaped art, not usable directly
  in the feature-graphic slot. `scripts/generate_feature_graphic.py` composes
  a native 1024x500 graphic from the app icon (`assets/logo.png`) and the
  brand palette/copy, feathering the icon's square edges so it blends into
  the gradient instead of showing a hard box.

## Not done here

This is a draft for board approval. Nothing has been uploaded to Play
Console — release-platform / Play Console credentials are not available to
agents. `docs/screenshots/*.png` (the flat, undecorated root set) has also
been refreshed in place from the same regeneration run, since it's the set
the ticket flagged directly, but the captioned/ordered set in this folder is
the one recommended for the actual listing.
