const tabSongs = {};
const downloadStatus = {};

function sanitizeFilename(name) {
  return name.replace(/[\/\\:*?"<>|]/g, '-').trim();
}

function detectExtension(url) {
  const lower = url.toLowerCase();
  for (const ext of ['.flac', '.m4a', '.wav', '.aac', '.ogg', '.mp3']) {
    if (lower.includes(ext)) return ext;
  }
  return '.mp3';
}

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
  if (msg.type === 'SONGS_FOUND') {
    const tabId = sender.tab?.id;
    if (tabId) {
      tabSongs[tabId] = msg.songs;
    }
    return;
  }

  if (msg.type === 'GET_TAB_SONGS') {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      const tabId = tabs[0]?.id;
      if (tabId && tabSongs[tabId]) {
        sendResponse({
          songs: tabSongs[tabId],
          status: downloadStatus[tabId] || {},
        });
      } else {
        sendResponse({ songs: [], status: {} });
      }
    });
    return true;
  }

  if (msg.type === 'REFRESH_SONGS') {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      const tab = tabs[0];
      if (!tab?.id) {
        sendResponse({ songs: [], status: {} });
        return;
      }
      chrome.tabs.sendMessage(tab.id, { type: 'GET_SONGS' }, (resp) => {
        if (chrome.runtime.lastError || !resp) {
          sendResponse({ songs: tabSongs[tab.id] || [], status: downloadStatus[tab.id] || {} });
          return;
        }
        tabSongs[tab.id] = resp.songs;
        sendResponse({
          songs: resp.songs,
          status: downloadStatus[tab.id] || {},
        });
      });
    });
    return true;
  }

  if (msg.type === 'DOWNLOAD_SONG') {
    handleDownload(msg.song, msg.tabId);
    sendResponse({ ok: true });
    return;
  }
});

function handleDownload(song, tabId) {
  const key = `${song.artist}-${song.name}`;

  if (!downloadStatus[tabId]) downloadStatus[tabId] = {};

  const artist = sanitizeFilename(song.artist);
  const name = sanitizeFilename(song.name);
  const ext = detectExtension(song.url);
  const filename = `${artist} - ${name}${ext}`;

  downloadStatus[tabId][key] = 'downloading';

  chrome.downloads.download(
    { url: song.url, filename: filename, saveAs: false },
    (downloadId) => {
      if (chrome.runtime.lastError) {
        console.error('Download failed:', chrome.runtime.lastError.message);
        downloadStatus[tabId][key] = 'error';
        return;
      }

      const listener = (delta) => {
        if (delta.id !== downloadId) return;
        if (delta.state) {
          if (delta.state.current === 'complete') {
            downloadStatus[tabId][key] = 'done';
            chrome.downloads.onChanged.removeListener(listener);
          } else if (delta.state.current === 'interrupted') {
            console.error('Download interrupted:', delta.error?.current);
            downloadStatus[tabId][key] = 'error';
            chrome.downloads.onChanged.removeListener(listener);
          }
        }
      };
      chrome.downloads.onChanged.addListener(listener);
    }
  );
}

chrome.tabs.onRemoved.addListener((tabId) => {
  delete tabSongs[tabId];
  delete downloadStatus[tabId];
});
