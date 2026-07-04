## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-04 - Replaced collectLatest with collect in high-frequency Flows
**Learning:** In high-frequency Kotlin Flow collections (like sensor commands updated at 60Hz/100Hz), using `collectLatest` causes severe GC pressure and potential dropped frames because it continuously cancels and restarts the coroutine for every single emission. `collect` is more efficient.
**Action:** Always use `collect` instead of `collectLatest` for fast, fire-and-forget streams of events like sensor inputs, to avoid coroutine cancellation overhead.
