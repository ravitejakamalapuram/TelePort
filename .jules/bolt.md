## 2026-06-21 - Isolate High-Frequency Compose Recomposition
**Learning:** Reading high-frequency changing state (like sensor-driven coordinates updated at 60Hz) inside a heavy parent Composable (like one containing an AndroidView or WebView) causes the entire view hierarchy to recompose on every frame, resulting in severe jank.
**Action:** Isolate high-frequency rendering logic by extracting it into its own separate, lightweight Composable to strictly confine recomposition.
