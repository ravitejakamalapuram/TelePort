## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-28 - Throttle High-Frequency Sensor Events
**Learning:** Decoupling high-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) from expensive network/allocation pipelines prevents GC pressure, thread pool contention, and WebSocket network flooding.
**Action:** Always accumulate raw sensor event changes over a small interval and throttle the emitting of events to the necessary frame rate (e.g., 60Hz or 16ms) instead of directly bridging hardware callbacks to network or rendering logic.
