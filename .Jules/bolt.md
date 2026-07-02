## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-02 - Replace collectLatest with collect for high-frequency sensor flows
**Learning:** In high-frequency Kotlin Flow collections (e.g., 100Hz+ event streams like sensor commands for Air Mouse),  causes severe GC pressure, memory allocation overhead, and potential dropped frames because it continuously cancels and restarts coroutines on every single emission.
**Action:** Use  over  when handling high-frequency sensor or UI state streams where every intermediate value matters or the processing lambda is fast/non-blocking.
## 2026-07-02 - Replace collectLatest with collect for high-frequency sensor flows
**Learning:** In high-frequency Kotlin Flow collections (e.g., 100Hz+ event streams like sensor commands for Air Mouse), collectLatest causes severe GC pressure, memory allocation overhead, and potential dropped frames because it continuously cancels and restarts coroutines on every single emission.
**Action:** Use collect over collectLatest when handling high-frequency sensor or UI state streams where every intermediate value matters or the processing lambda is fast/non-blocking.
