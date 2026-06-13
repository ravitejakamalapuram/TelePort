## ⚠️  PR Needs Rebase - Build Failing

This PR appears to be based on an old version of `main` and needs to be rebased.

### Current Issue

Build fails with compilation errors in `PaywallScreen.kt`:
```
e: Unresolved reference 'ProductDetails'.
e: Unresolved reference 'subscriptionOfferDetails'.
```

However, these errors **don't exist in the current `main` branch**, which means this PR's base is outdated.

### The Good News

The actual code change in this PR looks **excellent**:
- ✅ Clean performance optimization
- ✅ Well-documented with clear comments
- ✅ Uses `frame.data` directly instead of `frame.readBytes()` to avoid unnecessary allocations
- ✅ Good impact on GC pressure during high-frequency mirroring

### Required Action

Please **rebase this PR** on the latest `main` branch:

```bash
git checkout bolt-ktor-websocket-readbytes-optimization-2318371600714629223
git fetch origin
git rebase origin/main
git push --force-with-lease
```

After rebasing, the build should pass and this PR can be merged.

### Verification Gates Status

- ❌ `./scripts/dev.sh test` - **FAILED** (due to outdated base, not this PR's changes)
- ⏸️ `./scripts/dev.sh verify-ui` - **BLOCKED**
- ⏸️ `./scripts/dev.sh build` - **BLOCKED**

---

**Recommendation:** ⚠️  **Request Rebase** - The code change is good, but needs to be rebased on latest `main`.
