## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-01 - Decouple High-Frequency Hardware Callbacks from Network Pipelines
**Learning:** High-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) bridged directly to expensive network pipelines (like WebSocket broadcasts) cause severe GC pressure, thread pool contention, and network flooding.
**Action:** Accumulate raw sensor changes over a small interval and throttle emitting to ~60Hz (16ms) using `android.os.SystemClock.uptimeMillis()` to decouple the hardware frequency from the processing pipeline.
## 2026-06-29 - Broken Sensor Throttling Pattern
**Learning:** In GyroSensorTracker, a time-based throttle (`currentTime - lastEmitTime >= INTERVAL`) was nested alongside an `else if (accumulatedDx != 0f)` catch-all designed to flush remaining movement when the user stops. This caused the throttle to be completely bypassed during normal movement because the time constraint was ignored if any movement had accumulated, resulting in heavy network/GC pressure from rapid WebSocket emission.
**Action:** When implementing time-based throttling for high-frequency hardware sensors, ensure the time check is the strict outermost condition. If you need to flush state after a stationary period, handle it by updating `lastEmitTime` independently of the emission condition rather than bypassing the time check entirely.
## 2026-07-25 - Avoid Logging High-Frequency Flow Events
**Learning:** High-frequency events (like 60Hz-100Hz cursor/scroll movements from sensors) were being fully evaluated and logged (via `Log.d`) inside central UI `collectLatest` pipelines. This string formatting and system IPC overhead causes heavy GC pressure, jank, and severe logcat spam on both `TvActivityContent` and `NativePlayerActivity`.
**Action:** Always wrap `Log.d` inside high-frequency flow collectors or event buses with strict condition filters to exclude rapid, continuous events (`Command.MoveCursor`, `Command.Scroll`) during general-purpose debugging or logging.
