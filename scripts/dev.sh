#!/usr/bin/env bash
# ============================================================================
# dev.sh — TelePort Developer CLI Task Runner
#
# A unified runner to automate setup, assets, testing, building, and crash fixes.
# ============================================================================
set -euo pipefail

# Visual Color Output Helpers
NC='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'

log_info() { echo -e "${CYAN}[INFO]${NC} $*"; }
log_ok() { echo -e "${GREEN}[OK]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_err() { echo -e "${RED}[ERROR]${NC} $*"; }

# Get script parent directory and root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

show_help() {
  cat <<EOF
TelePort Automation Task Runner CLI

Usage:
  ./scripts/dev.sh <command> [options]

Commands:
  setup         Run environment checks and install required python dependencies.
  assets        Regenerate all client assets (Android mipmaps, TV banners, Chrome Extension icons).
  test          Run Android unit tests and validate Chrome Extension manifest configurations.
  build         Compile Android debug package and zip the Chrome Extension release package.
  mock-crash    Simulate a dry-run crash monitoring and auto-fixing AI execution locally.
  help          Display this help information.

Examples:
  ./scripts/dev.sh setup
  ./scripts/dev.sh assets
  ./scripts/dev.sh test
EOF
}

cmd_setup() {
  log_info "Running system diagnostics..."
  
  # Check JDK
  if command -v java &>/dev/null; then
    log_ok "Java Development Kit (JDK) found: $(java -version 2>&1 | head -n 1)"
  else
    log_err "JDK not found. Please install JDK 17+."
  fi

  # Check Android SDK / adb
  if command -v adb &>/dev/null; then
    log_ok "Android Debug Bridge (adb) found: $(adb version | head -n 1)"
  else
    log_warn "Android Command line tools (adb) not in PATH. Android debugging might be unavailable."
  fi

  # Check Python
  if command -v python3 &>/dev/null; then
    log_ok "Python 3 found: $(python3 --version)"
    log_info "Installing required Python packages (Pillow, requests, etc.)..."
    python3 -m pip install --quiet Pillow requests google-auth firebase-admin || {
      log_warn "Pip package install failed. Attempting with --break-system-packages..."
      python3 -m pip install --quiet --break-system-packages Pillow requests google-auth firebase-admin || log_err "Failed to install Python packages. Please install Pillow manually."
    }
    log_ok "Python environment configured."
  else
    log_err "Python 3 is required but not installed."
  fi

  log_ok "Setup verification complete!"
}

cmd_assets() {
  log_info "Compiling branding colors and metadata..."
  python3 scripts/compile_branding.py
  log_info "Compiling all project assets from single-source logo and banners..."
  if [[ -f "scripts/generate_assets.py" ]]; then
    python3 scripts/generate_assets.py
    log_ok "All Android assets and Chrome Extension icons updated successfully!"
  else
    log_err "Error: generate_assets.py script not found in scripts/."
    exit 1
  fi
}

cmd_test() {
  log_info "Compiling branding colors and metadata..."
  python3 scripts/compile_branding.py
  log_info "1. Validating Chrome Extension files..."
  local manifest="chrome-extension/manifest.json"
  if [[ ! -f "$manifest" ]]; then
    log_err "Manifest file not found: $manifest"
    exit 1
  fi

  # Verify JSON structural soundness
  if python3 -c "import json; json.load(open('$manifest'))" &>/dev/null; then
    log_ok "  - manifest.json structure is valid JSON."
  else
    log_err "  - manifest.json contains syntax/JSON formatting errors!"
    exit 1
  fi

  # Verify declared local icons exist
  log_info "Checking declared manifest icon assets..."
  python3 -c "
import json, os, sys
data = json.load(open('$manifest'))
err = False

# Inspect action and global icons
icons = []
if 'icons' in data:
    icons.extend(data['icons'].values())
if 'action' in data and 'default_icon' in data['action']:
    if isinstance(data['action']['default_icon'], dict):
        icons.extend(data['action']['default_icon'].values())
    else:
        icons.append(data['action']['default_icon'])

for icon_path in set(icons):
    full_path = os.path.join('chrome-extension', icon_path)
    if not os.path.exists(full_path):
        print(f'  ❌ Missing asset: {icon_path}', file=sys.stderr)
        err = True
    else:
        print(f'  ✅ Found asset: {icon_path}', file=sys.stderr)

if err:
    sys.exit(1)
" || {
    log_err "Chrome Extension declares icons in manifest that do not exist!"
    exit 1
  }
  log_ok "Chrome Extension files check passed successfully."

  log_info "2. Executing Android Unit Tests..."
  # Use Android Studio JBR or system JDK
  if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
  ./gradlew testDebugUnitTest
  log_ok "All unit tests completed successfully!"
}

cmd_build() {
  log_info "Compiling branding colors and metadata..."
  python3 scripts/compile_branding.py
  log_info "1. Packaging Chrome Extension..."
  rm -f teleport-chrome-extension.zip
  cd chrome-extension
  zip -r ../teleport-chrome-extension.zip . -x "*.DS_Store" "*__pycache__*" > /dev/null
  cd ..
  log_ok "Generated Chrome Extension package: teleport-chrome-extension.zip"

  log_info "2. Compiling Android Debug binaries..."
  if [[ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
  ./gradlew assembleDebug
  log_ok "Generated Android APK: app/build/outputs/apk/debug/app-debug.apk"
}

cmd_mock_crash() {
  log_info "Simulating local AI crash analysis..."
  if [[ ! -f "scripts/mock_crash.json" || ! -f "scripts/analyze_crash.sh" ]]; then
    log_err "Missing scripts/mock_crash.json or scripts/analyze_crash.sh"
    exit 1
  fi
  
  # Invoke dry-run crash monitoring
  DRY_RUN=true bash scripts/analyze_crash.sh scripts/mock_crash.json
  log_ok "Local AI mock crash execution completed successfully."
}

# Main Command Router
if [[ $# -lt 1 ]]; then
  show_help
  exit 0
fi

COMMAND="${1}"
shift

case "$COMMAND" in
  setup)
    cmd_setup
    ;;
  assets)
    cmd_assets
    ;;
  test)
    cmd_test
    ;;
  build)
    cmd_build
    ;;
  mock-crash)
    cmd_mock_crash
    ;;
  help)
    show_help
    ;;
  *)
    log_err "Unknown command: $COMMAND"
    show_help
    exit 1
    ;;
esac
