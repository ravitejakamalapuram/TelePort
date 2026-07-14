## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-05 - Optimize High-Frequency Sensor Emissions
**Learning:** High-frequency gyroscope sensors (~100-200Hz) emitting raw data over network protocols (like WebSockets) cause significant CPU strain, GC pressure, and frame drops. Directly calling callback methods on every single \`onSensorChanged\` event is an anti-pattern.
**Action:** Always accumulate rapid sensor deltas (e.g., dx, dy) and throttle emissions to a reasonable framerate like 60Hz (using \`SystemClock.uptimeMillis()\`) to batch updates while preserving total cursor movement.
## 2026-07-13 - Optimize Coroutine Flow Collection in WebSockets
**Learning:** Using `collectLatest` on high-frequency StateFlows or SharedFlows inside a WebSocket loop is an anti-pattern. `collectLatest` cancels the ongoing operation when a new emission occurs. If a rapid state update triggers during an active WebSocket `send()`, it can throw a `CancellationException`, disrupting the connection or dropping frames.
**Action:** Always use `collect` instead of `collectLatest` for continuous streams where every emission (or the current state over time) needs to be reliably sent over the network, ensuring the ongoing `send` completes without interruption.
