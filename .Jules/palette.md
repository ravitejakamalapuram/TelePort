## 2024-05-18 - Missing Loading States on Async Screens
**Learning:** Async operations like NSD discovery or network connections can leave users confused if no visual feedback is provided. The `MobileRemoteScreen` lacked loading states.
**Action:** Always wrap async textual states with a `CircularProgressIndicator` to provide immediate feedback that an operation is active.
