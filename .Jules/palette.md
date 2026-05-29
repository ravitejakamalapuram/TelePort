## 2024-05-18 - Missing Loading States on Async Screens
**Learning:** Async operations like NSD discovery or network connections can leave users confused if no visual feedback is provided. The `MobileRemoteScreen` lacked loading states.
**Action:** Always wrap async textual states with a `CircularProgressIndicator` to provide immediate feedback that an operation is active.
## 2026-05-25 - Optimize Input with KeyboardOptions and KeyboardActions
**Learning:** Using appropriate `KeyboardOptions` (like `KeyboardType.Uri`) customizes the keyboard layout for the expected input type, reducing user friction. Setting `ImeAction` combined with `KeyboardActions` allows users to submit forms or execute actions directly from the software keyboard, providing a significantly smoother inline UX compared to forcing the user to close the keyboard and tap a separate button.
**Action:** Always configure `KeyboardOptions` and `KeyboardActions` for text fields that act as primary input for actions (like entering an IP, URL, or chat message) to provide immediate, context-aware submission.
## 2026-05-26 - Modifier.toggleable() vs Raw Switch for Accessibility
**Learning:** Using a standalone `Switch` inside a Row forces users with motor impairments to tap a very small target, and screen readers read the descriptive text and the switch state as two separate elements. This causes a disjointed and frustrating accessibility experience.
**Action:** Always wrap the entire Row containing the setting description and the `Switch` with `Modifier.toggleable(value = state, role = Role.Switch, onValueChange = { ... })`, and set the `Switch`'s `onCheckedChange` to `null`. This unifies the semantics into a single accessible toggle with a massive, easy-to-hit touch target.
## 2024-05-27 - Inline Clear Buttons in TextFields
**Learning:** Users often need to rapidly clear entire inputs, such as manual IP addresses, URLs, or command inputs. Without an explicit UI affordance, users are forced to manually highlight text or hold the backspace key, causing unnecessary friction.
**Action:** Add a trailing IconButton (e.g., Icons.Filled.Clear) inside primary TextFields/OutlinedTextFields that appears when the input is not empty, enabling instant clearing of the field.
