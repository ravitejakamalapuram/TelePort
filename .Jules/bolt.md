## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-25 - Sensor Throttling
**Learning:** High-frequency hardware callbacks (like `SensorEventListener.onSensorChanged` for Gyroscopes) can trigger events up to 200Hz. If these events are directly bridged to network sockets or trigger object allocations without throttling, it causes massive GC pressure, jank, and network flooding.
**Action:** Always accumulate raw sensor changes over a small interval and throttle emitting them (e.g., to ~60Hz / 16ms using `SystemClock.uptimeMillis()`) rather than pushing every hardware event directly through expensive pipelines.
