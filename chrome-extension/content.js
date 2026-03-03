(() => {
  const AUDIO_PATTERN = /name:'([^']+)',artist:'([^']+)',url:'([^']+)',cover:'([^']+)'/g;

  function extractSongs() {
    const songs = [];
    const scripts = document.querySelectorAll('script');

    for (const script of scripts) {
      const text = script.textContent;
      if (!text.includes('APlayer')) continue;

      let match;
      AUDIO_PATTERN.lastIndex = 0;
      while ((match = AUDIO_PATTERN.exec(text)) !== null) {
        songs.push({
          name: match[1],
          artist: match[2],
          url: match[3],
          cover: match[4],
        });
      }
    }

    if (songs.length === 0) {
      const titleMatch = document.title.match(/([^《]+)《([^》]+)》/);
      if (titleMatch) {
        // Fallback: got title info but no audio URL from JS
        console.log('[HiFiTi] No APlayer config found, title info:', titleMatch[1], titleMatch[2]);
      }
    }

    return songs;
  }

  function sendSongs() {
    const songs = extractSongs();
    if (songs.length > 0) {
      chrome.runtime.sendMessage({ type: 'SONGS_FOUND', songs, url: location.href });
    }
  }

  sendSongs();

  chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
    if (msg.type === 'GET_SONGS') {
      const songs = extractSongs();
      sendResponse({ songs, url: location.href });
    }
    return true;
  });
})();
