## 2026-07-05 - Optimize High-Frequency Sensor Emissions
**Learning:** High-frequency gyroscope sensors (~100-200Hz) emitting raw data over network protocols (like WebSockets) cause significant CPU strain, GC pressure, and frame drops. Directly calling callback methods on every single \`onSensorChanged\` event is an anti-pattern.
**Action:** Always accumulate rapid sensor deltas (e.g., dx, dy) and throttle emissions to a reasonable framerate like 60Hz (using \`SystemClock.uptimeMillis()\`) to batch updates while preserving total cursor movement.
