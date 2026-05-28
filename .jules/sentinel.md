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

## 2024-05-30 - Insecure WebView Configuration (Mixed Content & File Access)
**Vulnerability:** The WebView in `TabManager.kt` had `mixedContentMode` set to `MIXED_CONTENT_ALWAYS_ALLOW` and did not explicitly disable `allowFileAccess` (which can be vulnerable in older Android versions). This could allow malicious active mixed content (scripts) to be executed or local files to be accessed via path traversal/LFI vulnerabilities if untrusted URLs are loaded.
**Learning:** WebView configurations must be hardened against common Web vulnerabilities (MITM XSS, local file theft).
**Prevention:** Set `mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE` (or `NEVER_ALLOW`) and explicitly set `allowFileAccess = false`.

## 2024-06-25 - Local File Inclusion (LFI) via content:// URIs and Malicious Schemes in WebView
**Vulnerability:** The WebView in `TabManager.kt` did not explicitly disable `allowContentAccess`, and `openTab` allowed arbitrary URL strings to be loaded. This could permit LFI via `content://` URIs or cross-site scripting/arbitrary code execution via `javascript:`, `intent:`, or `file:` schemes if an attacker provided a malicious link.
**Learning:** WebViews inherently trust the URLs they are given and the `content://` scheme is permitted by default. Input sanitization is critical before invoking `loadUrl`.
**Prevention:** Added `allowContentAccess = false` to the WebView configuration. Added URL scheme validation in `openTab` to ensure the parsed URI scheme is `http`, `https`, `about`, or `data`, falling back to `about:blank` for unsupported schemes or prepending `https://` if no scheme is provided.
