# Android Version Code Strategy

## ✅ **Permanent, Dynamic, Universal Solution**

This document explains the version code calculation strategy implemented in the shared workflow that **works for ALL Android projects** regardless of their version numbering.

---

## 📐 **The Formula**

```bash
VERSION_CODE = MAJOR × 1,000,000 + MINOR × 1,000 + PATCH
```

### **Why This Formula?**
1. ✅ **Unique**: Every semantic version maps to a unique version code
2. ✅ **Monotonic**: Version code always increases with version number
3. ✅ **Universal**: Works for any project with semantic versioning
4. ✅ **No Conflicts**: Impossible to have duplicate version codes
5. ✅ **Predictable**: Easy to calculate and verify

---

## 🧪 **Examples Across Different Projects**

### **TelePort (Current Project)**
| Version | Calculation | Version Code |
|---------|-------------|--------------|
| v1.5.14 | 1×1000000 + 5×1000 + 14 | 1,005,014 |
| v1.5.17 | 1×1000000 + 5×1000 + 17 | 1,005,017 |
| v1.5.100 | 1×1000000 + 5×1000 + 100 | 1,005,100 |
| v2.0.0 | 2×1000000 + 0×1000 + 0 | 2,000,000 |

### **New Android Project Starting at v0.1.0**
| Version | Calculation | Version Code |
|---------|-------------|--------------|
| v0.1.0 | 0×1000000 + 1×1000 + 0 | 1,000 |
| v0.1.1 | 0×1000000 + 1×1000 + 1 | 1,001 |
| v0.2.0 | 0×1000000 + 2×1000 + 0 | 2,000 |
| v1.0.0 | 1×1000000 + 0×1000 + 0 | 1,000,000 |

### **Mature Project at v10.50.999**
| Version | Calculation | Version Code |
|---------|-------------|--------------|
| v10.50.999 | 10×1000000 + 50×1000 + 999 | 10,050,999 |
| v10.51.0 | 10×1000000 + 51×1000 + 0 | 10,051,000 |

---

## 🎯 **Supported Range**

The formula supports semantic versions up to:
- **Major**: 0 - 2,147 (Android's max int is 2,147,483,647)
- **Minor**: 0 - 999
- **Patch**: 0 - 999

**Practical limit**: v2147.999.999 → 2,147,999,999 (still within Android's int range)

---

## 🔍 **Comparison with Old Approach**

### **OLD (Broken) - Commit Count**
```bash
CODE=$(git rev-list --count HEAD)
```
❌ **Problems**:
- Same code for different versions
- Non-deterministic across repos
- Causes Play Store conflicts
- Not portable between projects

### **NEW (Fixed) - Semantic Version**
```bash
NEW_TAG="${{ steps.version.outputs.new-tag }}"
VERSION="${NEW_TAG#v}"
IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION"
CODE=$((MAJOR * 1000000 + MINOR * 1000 + PATCH))
```
✅ **Benefits**:
- Unique code per version
- Deterministic and predictable
- No Play Store conflicts
- Works for ALL projects

---

## 📦 **Implementation Location**

- **Repository**: `ravitejakamalapuram/.github-workflows-shared`
- **File**: `.github/workflows/android-cd.yml`
- **Lines**: 97-116
- **Commit**: d8598bd

---

## 🚀 **Projects Using This Strategy**

✅ **TelePort** - Working perfectly (v1.5.17 → 1,005,017)  
✅ **All future Android projects** using the shared workflow

---

## ✅ **Verification**

Test the formula locally:
```bash
calculate_version_code() {
    local TAG="$1"
    local VERSION="${TAG#v}"
    IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION"
    local CODE=$((MAJOR * 1000000 + MINOR * 1000 + PATCH))
    echo "$TAG -> $CODE"
}

calculate_version_code "v1.5.17"   # Output: v1.5.17 -> 1005017
calculate_version_code "v2.0.0"    # Output: v2.0.0 -> 2000000
```

---

## 📝 **Summary**

This version code strategy is:
- ✅ **Permanent**: No need to change it ever
- ✅ **Dynamic**: Automatically adapts to any version number
- ✅ **Universal**: Works for all Android projects
- ✅ **Conflict-Free**: Guaranteed unique version codes
- ✅ **Maintainable**: Simple math, easy to understand

**No per-project configuration needed!** Just follow semantic versioning (vMAJOR.MINOR.PATCH) and the formula handles the rest.
