## 2026-06-12 - Missing Sandbox Dependencies causing CI failures
**Vulnerability:** Not a vulnerability, but missing dependencies in build.gradle for ads and billing caused CI failures.
**Learning:** We need to actually add the AdMob and Play Billing dependencies to make CI pass, as well as fixing a manifest merger conflict and an API usage change in Play Billing Library.
**Prevention:** Make sure all dependencies used in source code are listed in the build.gradle file.
## 2026-06-25 - Weak URL Extraction Vulnerability Fixed
**Vulnerability:** Weak URL extraction logic using regex `.split` and substring matching could lead to improper processing of links and potentially cause unexpected intents to fire if malicious or malformed URIs were passed.
**Learning:** Using basic string splits to match URIs is highly unreliable. `android.util.Patterns.WEB_URL` provides a standardized, robust regular expression specifically designed to identify URLs correctly and safely on Android.
**Prevention:** Always use standard regex libraries for specific tasks like `Patterns.WEB_URL.matcher(text)` to securely validate and extract URLs on Android instead of manual substring matching or splitting strings.
