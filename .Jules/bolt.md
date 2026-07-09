## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .

## 2026-06-28 - Coroutine Cancellation Overhead
**Learning:** Using `collectLatest` on a high-frequency Flow stream (like 100Hz gyroscope events) causes severe GC pressure and frame drops. Every new emission triggers a cancellation exception for the currently processing item, leading to constant coroutine recreation.
**Action:** Always use `collect` instead of `collectLatest` for high-frequency coordinate/movement streams to allow sequential processing without constant interruption.
