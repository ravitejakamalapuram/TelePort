## 2024-05-30 - XSS in sendTextActive JS Evaluation
**Vulnerability:** In `TabManager.kt`, `sendTextActive` uses `text.replace("'", "\\'")` which is insufficient. A payload like `\'; alert(1); //` or newline characters will break out of the JS string or cause a syntax error, potentially executing arbitrary JS.
**Learning:** Manual JS escaping is error-prone.
**Prevention:** Use a proper JSON serializer like `org.json.JSONObject.quote(text)` to safely encode input into a JS string literal.

## 2024-05-30 - XSS in Web Remote via HTML Injection
**Vulnerability:** In `LocalServerService.kt`, the Web Remote `renderTabs` function injects tab titles and URLs directly into `innerHTML` using string interpolation without escaping.
**Learning:** XSS occurs because the tab URL or title (received from the TV State) could be arbitrary (e.g., if a user visits a maliciously crafted title).
**Prevention:** Sanitize the strings by escaping HTML entities (`&`, `<`, `>`, `"`, `'`) before injecting them via `innerHTML`.

## 2024-05-30 - XSS in sendTextActive JS Evaluation
**Vulnerability:** In `TabManager.kt`, `sendTextActive` uses `text.replace("'", "\\'")` which is insufficient. A payload like `\'; alert(1); //` or newline characters will break out of the JS string or cause a syntax error, potentially executing arbitrary JS.
**Learning:** Manual JS escaping is error-prone.
**Prevention:** Use `org.json.JSONObject.quote(text)` to securely encode input strings into valid JS string literals.

## 2024-05-30 - XSS in Web Remote via HTML Injection
**Vulnerability:** In `LocalServerService.kt`, the Web Remote `renderTabs` function injects tab titles and URLs directly into `innerHTML` using string interpolation without escaping.
**Learning:** XSS occurs because the tab URL or title (received from the TV State) could contain arbitrary code (e.g. from malicious sites).
**Prevention:** Added an `escapeHtml` JavaScript function that escapes `&`, `<`, `>`, `"`, `'` and used it to sanitize variables injected into `innerHTML`.
