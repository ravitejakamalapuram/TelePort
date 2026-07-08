## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .

## 2024-07-08 - Use collect instead of collectLatest in high-frequency Flows
**Learning:** Using `collectLatest` on high-frequency `Flow` streams (like 100Hz gyroscope updates or rapid state changes) causes severe overhead because it cancels and restarts a coroutine for every emission. Additionally, if the downstream collector performs suspending operations (like WebSocket `send()`), `collectLatest` can dangerously cancel these operations mid-flight, dropping data or crashing the stream.
**Action:** Always prefer `collect` over `collectLatest` in high-frequency event streams or when performing suspending network operations, unless explicit cancellation of previous emissions is functionally required and safe.
