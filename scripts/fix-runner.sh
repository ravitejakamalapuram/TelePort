#!/bin/bash
# Fix GitHub Actions Runner Issues for TelePort
# This script restarts the runner service to clear network timeout issues

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

RUNNER_DIR="$HOME/github-runners/TelePort/Runner1"

if [ ! -d "$RUNNER_DIR" ]; then
    log_error "Runner directory not found at $RUNNER_DIR"
    exit 1
fi

cd "$RUNNER_DIR"

log_step "Checking runner service status..."
launchctl list | grep "actions.runner.ravitejakamalapuram-TelePort" || log_warn "Service not found in launchctl"

log_step "Stopping runner service..."
./svc.sh stop || log_warn "Service might not be running"

log_info "Waiting 5 seconds for graceful shutdown..."
sleep 5

log_step "Checking for stuck processes..."
STUCK_PROCESSES=$(ps aux | grep "Runner.Listener" | grep TelePort | grep -v grep || true)
if [ -n "$STUCK_PROCESSES" ]; then
    log_warn "Found stuck runner processes, killing them..."
    echo "$STUCK_PROCESSES"
    pkill -f "Runner.Listener.*TelePort" || log_warn "No processes to kill"
    sleep 2
fi

log_step "Clearing runner work directory..."
if [ -d "$RUNNER_DIR/_work" ]; then
    log_info "Found work directory, clearing contents..."
    rm -rf "$RUNNER_DIR/_work"/*
    log_info "Work directory cleared"
fi

log_step "Clearing diagnostic logs (keeping last 5 files)..."
if [ -d "$RUNNER_DIR/_diag" ]; then
    ls -t "$RUNNER_DIR/_diag"/Runner_*.log | tail -n +6 | xargs -r rm --
    log_info "Old diagnostic logs cleared"
fi

log_step "Starting runner service..."
./svc.sh start

log_info "Waiting 5 seconds for runner to initialize..."
sleep 5

log_step "Verifying runner status..."
if launchctl list | grep -q "actions.runner.ravitejakamalapuram-TelePort"; then
    log_info "✅ Runner service is running"
else
    log_error "❌ Runner service is NOT running"
    exit 1
fi

if ps aux | grep -q "[R]unner.Listener.*TelePort"; then
    log_info "✅ Runner process is active"
else
    log_error "❌ Runner process is NOT active"
    exit 1
fi

log_step "Checking GitHub connection..."
sleep 3
gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | select(.name=="TelePort-Runner1") | {name, status, busy}' || log_error "Failed to query GitHub API"

echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}  ✅ Runner restart completed!${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo ""
log_info "Runner logs: tail -f $RUNNER_DIR/_diag/Runner_*.log"
log_info "Service status: launchctl list | grep actions.runner"
echo ""
