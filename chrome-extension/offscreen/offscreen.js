let activeStream = null;
let activeWs = null;
let activeEncoder = null;
let activeReader = null;
let isCapturing = false;
let frameCount = 0;

// Listen for messages from background service-worker
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'START_CAPTURE') {
    startCapture(message.streamId, message.tvIp);
    sendResponse({ success: true });
  } else if (message.type === 'STOP_CAPTURE') {
    stopCapture();
    sendResponse({ success: true });
  }
  return true;
});

// Setup tab capture and start WebCodecs encoding pipeline
async function startCapture(streamId, tvIp) {
  try {
    // 1. Establish Ktor /mirror binary WebSocket connection
    const wsUrl = `ws://${tvIp}:8080/mirror`;
    console.log(`Connecting mirror stream to ${wsUrl}`);
    activeWs = new WebSocket(wsUrl);
    activeWs.binaryType = 'arraybuffer';

    activeWs.onopen = () => {
      console.log('Mirror WebSocket connection established');
    };

    activeWs.onerror = (err) => {
      console.error('Mirror WebSocket error:', err);
      chrome.runtime.sendMessage({ type: 'CAST_ERROR', error: 'WebSocket connection failed' });
    };

    activeWs.onclose = () => {
      console.log('Mirror WebSocket connection closed');
      if (isCapturing) {
        chrome.runtime.sendMessage({ type: 'CAST_ERROR', error: 'WebSocket connection closed unexpectedly' });
      }
    };

    // 2. Get MediaStream using target tab ID
    activeStream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        mandatory: {
          chromeMediaSource: 'tab',
          chromeMediaSourceId: streamId,
          maxWidth: 960,
          maxHeight: 540,
          maxFrameRate: 24
        }
      }
    });

    const videoTrack = activeStream.getVideoTracks()[0];
    if (!videoTrack) {
      throw new Error('No video track found in tab stream');
    }

    // 3. Initialize WebCodecs VideoEncoder
    activeEncoder = new VideoEncoder({
      output: (chunk) => {
        if (activeWs && activeWs.readyState === WebSocket.OPEN) {
          const buffer = new ArrayBuffer(chunk.byteLength);
          chunk.copyTo(buffer);
          activeWs.send(buffer);
        }
      },
      error: (err) => {
        console.error('WebCodecs VideoEncoder error:', err);
        chrome.runtime.sendMessage({ type: 'CAST_ERROR', error: `Encoder error: ${err.message}` });
      }
    });

    // Configure for low-latency H.264 baseline streaming (avc1.42e01f = Baseline Level 3.1)
    activeEncoder.configure({
      codec: 'avc1.42e01f',
      width: 960,
      height: 540,
      bitrate: 1500000, // 1.5 Mbps
      framerate: 24,
      latencyMode: 'realtime',
      hardwareAcceleration: 'prefer-hardware'
    });

    // 4. Start frame extraction loop
    isCapturing = true;
    frameCount = 0;
    const processor = new MediaStreamTrackProcessor({ track: videoTrack });
    activeReader = processor.readable.getReader();

    readVideoFrames();

  } catch (err) {
    console.error('Failed to start offscreen capture:', err);
    chrome.runtime.sendMessage({ type: 'CAST_ERROR', error: err.message });
  }
}

// Frame reading loop
async function readVideoFrames() {
  try {
    while (isCapturing && activeReader) {
      const { done, value: videoFrame } = await activeReader.read();
      if (done) break;

      if (activeEncoder && activeEncoder.state === 'configured') {
        const forceKey = (frameCount % 24 === 0);
        activeEncoder.encode(videoFrame, { keyFrame: forceKey });
        frameCount++;
      }
      videoFrame.close(); // Must release resource!
    }
  } catch (err) {
    console.error('Error during video frame processing:', err);
    chrome.runtime.sendMessage({ type: 'CAST_ERROR', error: err.message });
  }
}

// Cleanup and release all capture resources
function stopCapture() {
  isCapturing = false;
  
  if (activeReader) {
    activeReader.cancel();
    activeReader = null;
  }

  if (activeStream) {
    activeStream.getTracks().forEach(track => track.stop());
    activeStream = null;
  }

  if (activeEncoder) {
    try {
      if (activeEncoder.state === 'configured') {
        activeEncoder.close();
      }
    } catch (e) {}
    activeEncoder = null;
  }

  if (activeWs) {
    try {
      activeWs.close(1000, 'Capture stopped');
    } catch (e) {}
    activeWs = null;
  }

  console.log('Offscreen capture resources cleaned up');
}
