# Headless Screenshot Testing & Visual Regression Framework

This directory contains a generic, framework-agnostic screenshot generation and visual regression testing system for **Android Jetpack Compose** projects. It runs entirely headlessly on JVM utilizing **Robolectric Native Graphics** (no emulator required for tests).

## 📂 Framework Contents

1. **`ScreenshotEngine.kt`**: Reusable Kotlin engine. Generates screenshots of Compose components in custom dimensions (Phone, Tablet, TV), themes (Dark/Light), and locales. Supports automatic decoration (wrapping inside a sleek device bezel mockup with gradient background and marketing titles).
2. **`verify_ui.py`**: Python script that performs pixel-by-pixel RMS (Root-Mean-Square) checks between newly generated screenshots and baseline golden reference images, outputting side-by-side visual diff overlays for mismatched pixels.
3. **`github_pr_diff.py`**: Python script that scans diff files and outputs a markdown report (`pr_comment.md`) perfect for automated comments in Pull Requests.

---

## 🚀 Setup Guide for other Android Projects

To copy this framework into another project, follow these simple steps:

### 1. Copy Files
- Copy `ScreenshotEngine.kt` to your test source set (e.g. `app/src/test/java/com/yourpackage/`). Update the `package` name at the top.
- Copy `verify_ui.py` and `github_pr_diff.py` to a `scripts/` or similar directory in your repository.

### 2. Configure Gradle Dependencies
Add Robolectric and Compose testing dependencies in your app module's `build.gradle` or `build.gradle.kts`:

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Robolectric & Graphics NATIVE requirements
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    
    // Compose Testing
    testImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    testImplementation("androidx.activity:activity-compose:1.8.0")
}
```

### 3. Write JUnit Screenshot Tests
Create a test class in `src/test/` marked with `@GraphicsMode(GraphicsMode.Mode.NATIVE)`.

Example test class:
```kotlin
package com.yourpackage

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreScreenshotsTest {

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureLoginScreen() {
        ScreenshotEngine.capture(
            name = "login_screen.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            LoginScreen()
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMarketingAsset() {
        ScreenshotEngine.capture(
            name = "marketing_dashboard.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            decoration = ScreenshotEngine.DecorationConfig(
                title = "Clean Dashboard",
                description = "Monitor your metrics in real-time"
            )
        ) {
            DashboardScreen()
        }
    }
}
```

### 4. Running Verification and Promotion
Create commands or scripts to execute the verification pipeline:

```bash
# 1. Run Unit Tests to generate current screenshots
./gradlew testDebugUnitTest

# 2. Run verification script to compare with baselines
python3 scripts/verify_ui.py

# 3. Promote current screenshots to baseline golden files
python3 scripts/verify_ui.py --promote
```

---

## 🤖 CI/CD Pull Request Integration (GitHub Actions)

Include visual regression checking as part of your CI pipeline to prevent broken UIs from merging. You can use the following GitHub Actions job configuration:

```yaml
name: UI Visual Regression Check

on: [pull_request]

jobs:
  visual-testing:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'zulu'
          java-version: '17'

      - name: Cache Pip Packages
        uses: actions/cache@v3
        with:
          path: ~/.cache/pip
          key: ${{ runner.os }}-pip-pillow

      - name: Install Python Pillow
        run: pip install Pillow

      - name: Generate Current Screenshots
        run: ./gradlew testDebugUnitTest --continue

      - name: Run Visual Regression Script
        id: verify_ui
        run: python3 scripts/verify_ui.py
        continue-on-error: true

      - name: Format PR Comment Markdown
        run: python3 scripts/github_pr_diff.py

      - name: Post PR Preview Diff Comment
        if: steps.verify_ui.outcome == 'failure'
        uses: mshick/add-pr-comment@v2
        with:
          message-path: docs/screenshots/pr_comment.md
          
      - name: Fail if visual regression failed
        if: steps.verify_ui.outcome == 'failure'
        run: exit 1
```
