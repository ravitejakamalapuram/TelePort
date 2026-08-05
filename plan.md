1. **Explore issue/Identify issue:** The prompt requires implementing ONE micro-UX improvement. The memory states:
   *Web/HTML Accessibility Insight: To improve accessibility and usability (Fitts's Law) for custom toggles or switches in a list, wrap the entire row (including the description and the switch UI) in a single `<label>` element linked to the `<input type="checkbox">` via the `for` attribute, and apply `cursor: pointer`. This expands the touch target and properly groups the text with the input for screen readers. When doing this, ensure you avoid nested interactive elements by changing any inner `.switch` `<label>` elements to `<div>` or `<span>`.*
2. **Examine `chrome-extension/popup/popup.html`**: The UI contains toggles for "Tab Cast" and "TV Dark Mode". Currently, they are structured as `<div class="action-row">` with a nested `<label class="switch">`. We will modify them to conform to the accessibility insight.
3. **Fix `chrome-extension/popup/popup.html`**:
   - For Tab Cast:
     Change `<div class="action-row">` to `<label class="action-row" for="castToggle" style="cursor: pointer;">`
     Change `<label class="switch">` to `<span class="switch">`
     Close the `<label>` properly.
   - For TV Dark Mode:
     Change `<div class="action-row">` to `<label class="action-row" for="darkModeToggle" style="cursor: pointer;">`
     Change `<label class="switch">` to `<span class="switch">`
     Close the `<label>` properly.
4. **Verify the Edit**: Run `git diff` to confirm the changes are exactly as intended.
5. **Test the build**: Run `./gradlew assembleDebug lintDebug` in bash to ensure there are no compilation errors or lint issues. There is no `npm` or `yarn` to run.
6. **Pre-commit**: Run `frontend_verification_instructions` because we modified frontend UI files. Then complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
