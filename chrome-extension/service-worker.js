const transparentPixel = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

function showNotification(title, message) {
  chrome.notifications.create({
    type: "basic",
    iconUrl: transparentPixel,
    title: title,
    message: message
  });
}

// Helper to check if offscreen document is active
async function hasOffscreenDocument() {
  if (chrome.runtime.getContexts) {
    const contexts = await chrome.runtime.getContexts({
      contextTypes: ['OFFSCREEN_DOCUMENT']
    });
    return contexts.length > 0;
  }
  return false;
}

// Beam a URL directly to the TV by opening a WS connection and sending OpenUrl
function beamUrl(tvIp, url) {
  return new Promise((resolve) => {
    const ws = new WebSocket(`ws://${tvIp}:8080/control`);
    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: "com.teleport.app.protocol.Command.OpenUrl",
        url: url
      }));
      setTimeout(() => {
        ws.close();
        showNotification("Link Beamed", `Beamed page to TV successfully.`);
        resolve(true);
      }, 150);
    };
    ws.onerror = (err) => {
      console.error("WebSocket error beaming URL:", err);
      showNotification("Connection Failed", `Could not connect to TV at ${tvIp}.`);
      resolve(false);
    };
  });
}

// Start tab mirroring capture
async function startCasting(tvIp, tab) {
  try {
    // Notify TV to open the mirror screen
    const notifySuccess = await notifyTvToStartMirror(tvIp);
    if (!notifySuccess) {
      await chrome.storage.session.set({ recordingState: 'idle' });
      return;
    }

    // Get tab capture stream ID
    const streamId = await chrome.tabCapture.getMediaStreamId({ targetTabId: tab.id });

    // Open offscreen document if not already open
    const hasOffscreen = await hasOffscreenDocument();
    if (!hasOffscreen) {
      await chrome.offscreen.createDocument({
        url: 'offscreen/offscreen.html',
        reasons: ['USER_MEDIA'],
        justification: 'Encode and stream captured tab audio/video frames to TV'
      });
    }

    // Send capture parameters to offscreen
    await chrome.runtime.sendMessage({
      type: 'START_CAPTURE',
      streamId,
      tvIp
    });

    await chrome.storage.session.set({ recordingState: 'recording', castingTabId: tab.id });
    await chrome.action.setBadgeText({ text: 'CAST' });
    await chrome.action.setBadgeBackgroundColor({ color: '#FF0000' });
    showNotification("Mirroring Started", "Active tab is now mirroring to the TV.");
  } catch (err) {
    console.error("Failed to start casting:", err);
    await chrome.storage.session.set({ recordingState: 'idle' });
    await chrome.action.setBadgeText({ text: '' });
    showNotification("Mirroring Failed", err.message || "Failed to start tab capture.");
  }
}

// Notify TV via control WebSocket to display MirrorPlayerScreen
function notifyTvToStartMirror(tvIp) {
  return new Promise((resolve) => {
    const ws = new WebSocket(`ws://${tvIp}:8080/control`);
    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: "com.teleport.app.protocol.Command.StartMirroring"
      }));
      setTimeout(() => {
        ws.close();
        resolve(true);
      }, 150);
    };
    ws.onerror = (err) => {
      console.error("Failed to notify TV to start mirror:", err);
      showNotification("Connection Failed", `Could not connect to TV at ${tvIp} to start cast.`);
      resolve(false);
    };
  });
}

// Notify TV via control WebSocket to stop displaying MirrorPlayerScreen
function notifyTvToStopMirror(tvIp) {
  return new Promise((resolve) => {
    const ws = new WebSocket(`ws://${tvIp}:8080/control`);
    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: "com.teleport.app.protocol.Command.StopMirroring"
      }));
      setTimeout(() => {
        ws.close();
        resolve(true);
      }, 150);
    };
    ws.onerror = (err) => {
      console.error("Failed to notify TV to stop mirror:", err);
      resolve(false);
    };
  });
}

// Stop tab mirroring capture
async function stopCasting(tvIp) {
  try {
    const hasOffscreen = await hasOffscreenDocument();
    if (hasOffscreen) {
      try {
        await chrome.runtime.sendMessage({ type: 'STOP_CAPTURE' });
      } catch (e) {}
      await chrome.offscreen.closeDocument();
    }

    if (tvIp) {
      await notifyTvToStopMirror(tvIp);
    }

    await chrome.storage.session.set({ recordingState: 'idle', castingTabId: null });
    await chrome.action.setBadgeText({ text: '' });
    showNotification("Mirroring Stopped", "Tab mirroring ended.");
  } catch (err) {
    console.error("Failed to stop casting:", err);
    await chrome.storage.session.set({ recordingState: 'idle' });
    await chrome.action.setBadgeText({ text: '' });
  }
}

// Handle Context Menu items
chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: "send-link",
    title: "Send active page to TelePort TV",
    contexts: ["page"]
  });
  chrome.contextMenus.create({
    id: "mirror-tab",
    title: "Mirror active tab to TelePort TV",
    contexts: ["page"]
  });
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  const { tvIp } = await chrome.storage.local.get("tvIp");
  if (!tvIp) {
    showNotification("No TV Configured", "Please open the extension popup and configure your TV IP address.");
    return;
  }

  if (info.menuItemId === "send-link") {
    await beamUrl(tvIp, tab.url);
  } else if (info.menuItemId === "mirror-tab") {
    const { recordingState = 'idle' } = await chrome.storage.session.get('recordingState');
    if (recordingState === 'idle') {
      await chrome.storage.session.set({ recordingState: 'starting' });
      await startCasting(tvIp, tab);
    } else if (recordingState === 'recording') {
      await chrome.storage.session.set({ recordingState: 'stopping' });
      await stopCasting(tvIp);
    }
  }
});

// Handle incoming messages from popup or offscreen scripts
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  (async () => {
    const { tvIp } = await chrome.storage.local.get("tvIp");
    
    if (message.type === 'START_CAST') {
      if (!tvIp) {
        showNotification("No TV Configured", "Please configure your TV IP.");
        sendResponse({ success: false, error: "No TV IP" });
        return;
      }
      const { recordingState = 'idle' } = await chrome.storage.session.get('recordingState');
      if (recordingState === 'idle') {
        await chrome.storage.session.set({ recordingState: 'starting' });
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab) {
          await startCasting(tvIp, tab);
          sendResponse({ success: true });
        } else {
          await chrome.storage.session.set({ recordingState: 'idle' });
          sendResponse({ success: false, error: "No active tab found" });
        }
      } else {
        sendResponse({ success: false, error: "Already casting" });
      }
    } else if (message.type === 'STOP_CAST') {
      const { recordingState = 'idle' } = await chrome.storage.session.get('recordingState');
      if (recordingState === 'recording') {
        await chrome.storage.session.set({ recordingState: 'stopping' });
        await stopCasting(tvIp);
        sendResponse({ success: true });
      } else {
        sendResponse({ success: false, error: "Not casting" });
      }
    } else if (message.type === 'BEAM_URL') {
      if (!tvIp) {
        sendResponse({ success: false, error: "No TV IP" });
        return;
      }
      const success = await beamUrl(tvIp, message.url);
      sendResponse({ success });
    } else if (message.type === 'CAST_ERROR') {
      console.warn("Cast error received from offscreen:", message.error);
      await stopCasting(tvIp);
      showNotification("Casting Disconnected", `The connection to the TV was lost: ${message.error}`);
      sendResponse({ success: true });
    }
  })();
  return true; // Keep message channel open for async response
});
