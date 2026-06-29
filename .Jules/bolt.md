## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .

## 2026-06-29 - Decoupling High-Frequency Hardware Interrupts
**Learning:** High-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) coupled directly with expensive network/allocation pipelines cause severe GC pressure, thread pool contention, and network flooding.
**Action:** Decouple these events by accumulating raw sensor changes over a small interval and throttling the emission (e.g., to ~60Hz or 16ms) using `android.os.SystemClock.uptimeMillis()`.
