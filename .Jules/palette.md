## 2024-05-18 - Missing Loading States on Async Screens
**Learning:** Async operations like NSD discovery or network connections can leave users confused if no visual feedback is provided. The `MobileRemoteScreen` lacked loading states.
**Action:** Always wrap async textual states with a `CircularProgressIndicator` to provide immediate feedback that an operation is active.
## 2026-05-25 - Optimize Input with KeyboardOptions and KeyboardActions
**Learning:** Using appropriate `KeyboardOptions` (like `KeyboardType.Uri`) customizes the keyboard layout for the expected input type, reducing user friction. Setting `ImeAction` combined with `KeyboardActions` allows users to submit forms or execute actions directly from the software keyboard, providing a significantly smoother inline UX compared to forcing the user to close the keyboard and tap a separate button.
**Action:** Always configure `KeyboardOptions` and `KeyboardActions` for text fields that act as primary input for actions (like entering an IP, URL, or chat message) to provide immediate, context-aware submission.
