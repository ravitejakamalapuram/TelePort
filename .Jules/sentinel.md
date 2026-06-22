## 2026-06-12 - Missing Sandbox Dependencies causing CI failures
**Vulnerability:** Not a vulnerability, but missing dependencies in build.gradle for ads and billing caused CI failures.
**Learning:** We need to actually add the AdMob and Play Billing dependencies to make CI pass, as well as fixing a manifest merger conflict and an API usage change in Play Billing Library.
**Prevention:** Make sure all dependencies used in source code are listed in the build.gradle file.

## 2024-06-22 - Fix missing authorization on mirror WebSocket
**Vulnerability:** The `/mirror` WebSocket endpoint in `LocalServerService.kt` relied solely on Cross-Site WebSocket Hijacking (CSWSH) Origin checks but lacked explicit user authorization state checks, allowing any local network device to connect and stream frames.
**Learning:** In local network Ktor applications, CSWSH protections do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., generating a clientId and verifying it against an approved clients list) within WebSocket endpoints before accepting connections or processing frames.
