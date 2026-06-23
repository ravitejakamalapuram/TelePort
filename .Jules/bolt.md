## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-23 - Jetpack Compose High-Frequency Rendering Isolation
**Learning:** In Jetpack Compose, reading high-frequency changing state (like sensor-driven cursor coordinates updated at 60Hz) inside a heavy parent Composable (such as one containing an `AndroidView` or `WebView`) causes the entire view hierarchy to recompose on every frame, resulting in severe jank.
**Action:** Isolate high-frequency rendering logic by extracting it into its own separate, lightweight Composable to strictly confine recomposition.
