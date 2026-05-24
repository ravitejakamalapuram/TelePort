## 2026-05-24 - Optimized AdBlocker.isAd domain matching
**Learning:** Found a performance bottleneck in AdBlocker where matching host URLs against ad domains used an O(N * L) linear scan across a HashSet with `endsWith` checks, rather than taking advantage of O(1) hash map lookups. Since this is checked on every web request interception, it impacts WebView performance.
**Action:** When matching nested domains or paths against a blocklist, iterate through the path components (O(M)) and use O(1) set lookups instead of checking every blocked pattern (O(N)).
