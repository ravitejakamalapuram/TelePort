let controlWs = null;
let currentTvIp = '';
let isMouseDown = false;
let lastX = 0;
let lastY = 0;
let dragDistance = 0;

// UI Elements
const connectionBadge = document.getElementById('connectionBadge');
const tvIpInput = document.getElementById('tvIpInput');
const saveIpBtn = document.getElementById('saveIpBtn');

const actionsCard = document.getElementById('actionsCard');
const castToggle = document.getElementById('castToggle');
const beamLinkBtn = document.getElementById('beamLinkBtn');
const darkModeToggle = document.getElementById('darkModeToggle');

const remoteControlsContainer = document.getElementById('remoteControlsContainer');
const tabTrackpadBtn = document.getElementById('tabTrackpadBtn');
const tabDpadBtn = document.getElementById('tabDpadBtn');
const contentTrackpad = document.getElementById('contentTrackpad');
const contentDpad = document.getElementById('contentDpad');
const trackpad = document.getElementById('trackpad');

const textCard = document.getElementById('textCard');
const keyboardInput = document.getElementById('keyboardInput');
const sendTextBtn = document.getElementById('sendTextBtn');

// Buttons
const dpadUpBtn = document.getElementById('dpadUpBtn');
const dpadDownBtn = document.getElementById('dpadDownBtn');
const dpadLeftBtn = document.getElementById('dpadLeftBtn');
const dpadRightBtn = document.getElementById('dpadRightBtn');
const dpadOkBtn = document.getElementById('dpadOkBtn');
const backBtn = document.getElementById('backBtn');
const playPauseBtn = document.getElementById('playPauseBtn');

// Initialize Extension Popup
document.addEventListener('DOMContentLoaded', async () => {
  // Load TV IP from storage
  const { tvIp } = await chrome.storage.local.get('tvIp');
  if (tvIp) {
    tvIpInput.value = tvIp;
    currentTvIp = tvIp;
    connectToTv(tvIp);
  }

  // Load active casting state
  const { recordingState = 'idle' } = await chrome.storage.session.get('recordingState');
  castToggle.checked = (recordingState === 'recording');

  // Bind Setup IP events
  saveIpBtn.addEventListener('click', saveIpSetting);
  tvIpInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') saveIpSetting();
  });

  // Bind Cast Toggle
  castToggle.addEventListener('change', toggleCastState);

  // Bind Link Beaming
  beamLinkBtn.addEventListener('click', beamActiveTabUrl);

  // Bind Dark Mode
  darkModeToggle.addEventListener('change', toggleTvDarkMode);

  // Bind Tab switching
  tabTrackpadBtn.addEventListener('click', () => switchTab('trackpad'));
  tabDpadBtn.addEventListener('click', () => switchTab('buttons'));

  // Bind Trackpad gestures
  setupTrackpadGestures();

  // Bind D-Pad click events
  dpadUpBtn.addEventListener('click', () => sendDpadDirection(0, -150));
  dpadDownBtn.addEventListener('click', () => sendDpadDirection(0, 150));
  dpadLeftBtn.addEventListener('click', () => sendDpadDirection(-150, 0));
  dpadRightBtn.addEventListener('click', () => sendDpadDirection(150, 0));
  dpadOkBtn.addEventListener('click', () => sendCommand({ type: 'com.teleport.app.protocol.Command.Click' }));
  
  backBtn.addEventListener('click', () => sendCommand({ type: 'com.teleport.app.protocol.Command.GoBack' }));
  playPauseBtn.addEventListener('click', () => sendCommand({ type: 'com.teleport.app.protocol.Command.PlayPause' }));

  // Bind Keyboard sending
  sendTextBtn.addEventListener('click', sendKeyboardText);
  keyboardInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendKeyboardText();
  });
});

// Switch UI tabs (Trackpad vs D-pad buttons)
function switchTab(tabName) {
  if (tabName === 'trackpad') {
    tabTrackpadBtn.classList.add('active');
    tabDpadBtn.classList.remove('active');
    contentTrackpad.classList.add('active');
    contentDpad.classList.remove('active');
  } else {
    tabTrackpadBtn.classList.remove('active');
    tabDpadBtn.classList.add('active');
    contentTrackpad.classList.remove('active');
    contentDpad.classList.add('active');
  }
}

// Connect to TV Local Server
function connectToTv(tvIp) {
  updateBadgeState('connecting');

  if (controlWs) {
    controlWs.close();
  }

  const wsUrl = `ws://${tvIp}:8080/control`;
  controlWs = new WebSocket(wsUrl);

  controlWs.onopen = () => {
    console.log('Connected to TV WebSocket control channel');
    updateBadgeState('connected');
    enableControls(true);
  };

  controlWs.onmessage = (event) => {
    try {
      const state = JSON.parse(event.data);
      // Sync UI with TV State (e.g. if we get dark mode status or stream url)
    } catch (e) {
      console.warn('Failed to parse incoming TV state:', e);
    }
  };

  controlWs.onerror = (err) => {
    console.error('WebSocket connection error:', err);
    updateBadgeState('disconnected');
    enableControls(false);
  };

  controlWs.onclose = () => {
    console.log('TV WebSocket connection closed');
    updateBadgeState('disconnected');
    enableControls(false);
  };
}

// Update Badge UI State
function updateBadgeState(state) {
  connectionBadge.className = 'status-badge';
  if (state === 'connected') {
    connectionBadge.classList.add('connected');
    connectionBadge.textContent = 'Connected';
  } else if (state === 'connecting') {
    connectionBadge.classList.add('disconnected');
    connectionBadge.textContent = 'Connecting...';
  } else {
    connectionBadge.classList.add('disconnected');
    connectionBadge.textContent = 'Disconnected';
  }
}

// Enable/Disable Remote Controls Card
function enableControls(enabled) {
  if (enabled) {
    actionsCard.classList.remove('disabled');
    remoteControlsContainer.classList.remove('disabled');
    textCard.classList.remove('disabled');
  } else {
    actionsCard.classList.add('disabled');
    remoteControlsContainer.classList.add('disabled');
    textCard.classList.add('disabled');
  }
}

// Save IP address setting
async function saveIpSetting() {
  const ip = tvIpInput.value.trim();
  if (!ip) return;
  
  await chrome.storage.local.set({ tvIp: ip });
  currentTvIp = ip;
  connectToTv(ip);
}

// Send Command via WebSocket
function sendCommand(cmd) {
  if (controlWs && controlWs.readyState === WebSocket.OPEN) {
    controlWs.send(JSON.stringify(cmd));
  } else if (currentTvIp) {
    // Attempt lazy reconnect
    connectToTv(currentTvIp);
  }
}

// Beam current tab URL to TV
async function beamActiveTabUrl() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab || !tab.url) return;

  chrome.runtime.sendMessage({
    type: 'BEAM_URL',
    url: tab.url
  });
}

// Toggle TV Smart Dark Mode
function toggleTvDarkMode() {
  sendCommand({
    type: 'com.teleport.app.protocol.Command.ToggleDarkMode',
    enabled: darkModeToggle.checked
  });
}

// Toggle screen mirroring cast state
function toggleCastState() {
  const isCasting = castToggle.checked;
  if (isCasting) {
    chrome.runtime.sendMessage({ type: 'START_CAST' }, (response) => {
      if (!response || !response.success) {
        castToggle.checked = false;
      }
    });
  } else {
    chrome.runtime.sendMessage({ type: 'STOP_CAST' }, (response) => {
      if (!response || !response.success) {
        castToggle.checked = true;
      }
    });
  }
}

// Send D-pad navigation direction as a cursor scroll
function sendDpadDirection(dx, dy) {
  sendCommand({
    type: 'com.teleport.app.protocol.Command.Scroll',
    dx: dx,
    dy: dy
  });
}

// Send text to input element
function sendKeyboardText() {
  const text = keyboardInput.value;
  if (!text) return;

  sendCommand({
    type: 'com.teleport.app.protocol.Command.SendText',
    text: text
  });
  keyboardInput.value = '';
}

// Setup DND Trackpad Gestures
function setupTrackpadGestures() {
  // Mouse Drag Events -> Cursor Move
  trackpad.addEventListener('mousedown', (e) => {
    isMouseDown = true;
    lastX = e.clientX;
    lastY = e.clientY;
    dragDistance = 0;
  });

  trackpad.addEventListener('mousemove', (e) => {
    if (!isMouseDown) return;
    const dx = e.clientX - lastX;
    const dy = e.clientY - lastY;
    lastX = e.clientX;
    lastY = e.clientY;
    dragDistance += Math.abs(dx) + Math.abs(dy);

    sendCommand({
      type: 'com.teleport.app.protocol.Command.MoveCursor',
      dx: dx * 1.8,
      dy: dy * 1.8
    });
  });

  window.addEventListener('mouseup', () => {
    if (isMouseDown) {
      isMouseDown = false;
    }
  });

  trackpad.addEventListener('click', () => {
    // If the click was a tap (little/no drag), trigger TV click
    if (dragDistance < 6) {
      sendCommand({ type: 'com.teleport.app.protocol.Command.Click' });
    }
  });

  // Mouse Wheel Event -> Natural Scroll
  trackpad.addEventListener('wheel', (e) => {
    e.preventDefault();
    sendCommand({
      type: 'com.teleport.app.protocol.Command.Scroll',
      dx: e.deltaX,
      dy: e.deltaY
    });
  }, { passive: false });
}
