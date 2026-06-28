## 2026-06-21 - Isolate High-Frequency Compose Recomposition
**Learning:** Reading high-frequency changing state (like sensor-driven coordinates updated at 60Hz) inside a heavy parent Composable (like one containing an AndroidView or WebView) causes the entire view hierarchy to recompose on every frame, resulting in severe jank.
**Action:** Isolate high-frequency rendering logic by extracting it into its own separate, lightweight Composable to strictly confine recomposition.
## $(date +%Y-%m-%d) - Optimize High-Frequency Command Coroutine Allocations
**Learning:** In highly frequent events like cursor movements (up to 200Hz from sensors), launching a new coroutine (`launch(Dispatchers.IO) { ... }`) for every single event leads to extreme coroutine allocation overhead, GC thrashing, and thread starvation.
**Action:** Use a `Channel<Command>(Channel.UNLIMITED)` to act as a buffer. Launch a single consumer coroutine to read from this channel in a loop and send the commands, completely eliminating the per-event coroutine instantiation overhead.
