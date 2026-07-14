## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-01 - Decouple High-Frequency Hardware Callbacks from Network Pipelines
**Learning:** High-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) bridged directly to expensive network pipelines (like WebSocket broadcasts) cause severe GC pressure, thread pool contention, and network flooding.
**Action:** Accumulate raw sensor changes over a small interval and throttle emitting to ~60Hz (16ms) using `android.os.SystemClock.uptimeMillis()` to decouple the hardware frequency from the processing pipeline.
## 2023-10-27 - [Network Collect Flow Performance]
**Learning:** Using `collectLatest` on high-frequency UI/command flow channels in network and service listeners leads to rapid coroutine cancellation and severe performance impact. Since these are streams of commands or actions that need to be processed without pre-empting the previous processing unecessarily, `collect` must be used over `collectLatest` to avoid dropping events or heavy GC load and CPU spinning.
**Action:** Replace `collectLatest` with `collect` when consuming flow data, particularly for fast streams like commands and sensors unless we explicitly want only the final update in a fast burst.
