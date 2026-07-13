## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-05 - Optimize High-Frequency Sensor Emissions
**Learning:** High-frequency gyroscope sensors (~100-200Hz) emitting raw data over network protocols (like WebSockets) cause significant CPU strain, GC pressure, and frame drops. Directly calling callback methods on every single \`onSensorChanged\` event is an anti-pattern.
**Action:** Always accumulate rapid sensor deltas (e.g., dx, dy) and throttle emissions to a reasonable framerate like 60Hz (using \`SystemClock.uptimeMillis()\`) to batch updates while preserving total cursor movement.
## 2026-07-13 - Replace collectLatest with collect in high-frequency flows
**Learning:** In high-frequency flow collections (e.g. 100Hz+ event streams), using `collectLatest` is an anti-pattern. `collectLatest` cancels its ongoing suspending block and launches a new one on every emission. This continuous coroutine cancellation and creation causes severe GC pressure, memory allocation overhead, and potential dropped frames.
**Action:** For high-frequency event flows, replace `collectLatest` with `collect` to safely process emissions sequentially and avoid coroutine cancellation overhead.
