
## 2024-05-18 - Isolate high-frequency state in Compose
**Learning:** Reading high-frequency changing state (like 60Hz sensor-driven coordinates) inside a heavy parent Composable (like one containing an `AndroidView` or `WebView`) causes the entire view hierarchy to recompose on every frame, resulting in severe jank.
**Action:** Isolate high-frequency rendering logic by extracting it into its own separate, lightweight Composable to strictly confine recomposition.
