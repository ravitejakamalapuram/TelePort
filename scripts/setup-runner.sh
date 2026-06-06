#!/bin/bash
# GitHub Actions Runner Setup Script for TelePort
# This script automates the setup of a self-hosted GitHub Actions runner

set -e

REPO_URL="https://github.com/ravitejakamalapuram/TelePort"
RUNNER_NAME="TelePort-Runner1"
RUNNER_VERSION="2.334.0"
RUNNER_DIR="$HOME/actions-runner"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    log_error "GitHub CLI (gh) is not installed. Please install it first:"
    echo "  brew install gh"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    log_error "Not authenticated with GitHub CLI. Please run:"
    echo "  gh auth login"
    exit 1
fi

log_info "Setting up GitHub Actions runner for TelePort..."

# Create runner directory
if [ ! -d "$RUNNER_DIR" ]; then
    log_info "Creating runner directory at $RUNNER_DIR"
    mkdir -p "$RUNNER_DIR"
fi

cd "$RUNNER_DIR"

# Download runner if not already present
if [ ! -f "run.sh" ]; then
    log_info "Downloading GitHub Actions runner v${RUNNER_VERSION}..."
    
    # Determine architecture
    ARCH=$(uname -m)
    if [ "$ARCH" = "arm64" ]; then
        RUNNER_FILE="actions-runner-osx-arm64-${RUNNER_VERSION}.tar.gz"
        CHECKSUM="760899b29fd4e942076bcd1160a662bf83c15d9ce8a8cc466763aec7e582b21b"
    else
        RUNNER_FILE="actions-runner-osx-x64-${RUNNER_VERSION}.tar.gz"
        CHECKSUM="73a979ff7e9ce8a70244f3a959d896870be486fac92bb08ed90684f961474e0d"
    fi
    
    curl -o "$RUNNER_FILE" -L "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/${RUNNER_FILE}"
    
    # Verify checksum
    echo "${CHECKSUM}  ${RUNNER_FILE}" | shasum -a 256 -c
    
    log_info "Extracting runner..."
    tar xzf "$RUNNER_FILE"
    rm "$RUNNER_FILE"
fi

# Get registration token
log_info "Generating registration token..."
TOKEN=$(gh api --method POST /repos/ravitejakamalapuram/TelePort/actions/runners/registration-token --jq '.token')

if [ -z "$TOKEN" ]; then
    log_error "Failed to generate registration token"
    exit 1
fi

log_info "Token generated successfully"

# Configure runner
log_info "Configuring runner..."
./config.sh --url "$REPO_URL" --token "$TOKEN" --name "$RUNNER_NAME" --labels self-hosted,macOS,X64 --unattended

log_info "Runner configured successfully!"

# Ask user how to run
echo ""
read -p "Do you want to install as a service (runs automatically on boot)? [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    log_info "Installing runner as a service..."
    sudo ./svc.sh install
    sudo ./svc.sh start
    log_info "Runner service installed and started"
    log_info "To check status: sudo ./svc.sh status"
    log_info "To view logs: tail -f $RUNNER_DIR/_diag/*.log"
else
    log_info "Runner configured but not started as service"
    log_info "To run interactively: cd $RUNNER_DIR && ./run.sh"
    log_info "To install as service later: cd $RUNNER_DIR && sudo ./svc.sh install && sudo ./svc.sh start"
fi

echo ""
log_info "✅ Runner setup complete!"
log_info "Verify in GitHub: gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | {name, status}'"
