## 2026-06-12 - Missing Sandbox Dependencies causing CI failures
**Vulnerability:** Not a vulnerability, but missing dependencies in build.gradle for ads and billing caused CI failures.
**Learning:** We need to actually add the AdMob and Play Billing dependencies to make CI pass, as well as fixing a manifest merger conflict and an API usage change in Play Billing Library.
**Prevention:** Make sure all dependencies used in source code are listed in the build.gradle file.
