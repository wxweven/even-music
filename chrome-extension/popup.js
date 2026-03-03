const contentEl = document.getElementById('content');
const actionsEl = document.getElementById('actions');
const btnDownloadAll = document.getElementById('btn-download-all');
const btnRefresh = document.getElementById('btn-refresh');

let currentTabId = null;
let currentSongs = [];

function statusLabel(s) {
  switch (s) {
    case 'resolving':  return '<span class="status-text">解析中...</span>';
    case 'downloading': return '<span class="status-text">下载中...</span>';
    case 'done':       return '<span class="status-text status-done">已下载 ✓</span>';
    case 'error':      return '<span class="status-text status-error">失败</span>';
    default:           return '';
  }
}

function songKey(song) {
  return `${song.artist}-${song.name}`;
}

function renderSongs(songs, status) {
  if (!songs || songs.length === 0) {
    contentEl.innerHTML = '<div class="empty">未检测到音乐<br><br>请确认当前页面是 hifiti.com 的音乐页面，<br>并等待页面加载完成后点击「刷新」</div>';
    actionsEl.style.display = 'none';
    return;
  }

  currentSongs = songs;
  actionsEl.style.display = 'flex';

  contentEl.innerHTML = songs.map((song, i) => {
    const key = songKey(song);
    const st = status[key];
    const isActive = st === 'resolving' || st === 'downloading';
    const isDone = st === 'done';

    let actionHtml;
    if (st === 'error') {
      actionHtml = `<button class="btn btn-retry" data-idx="${i}">重试</button>`;
    } else if (isDone) {
      actionHtml = statusLabel(st);
    } else if (isActive) {
      actionHtml = statusLabel(st);
    } else {
      actionHtml = `<button class="btn btn-download" data-idx="${i}">下载</button>`;
    }

    return `
      <div class="song-item">
        <img class="song-cover" src="${song.cover}" alt="">
        <div class="song-info">
          <div class="song-name" title="${song.name}">${song.name}</div>
          <div class="song-artist">${song.artist}</div>
        </div>
        ${actionHtml}
      </div>`;
  }).join('');

  const allDone = songs.every(s => status[songKey(s)] === 'done');
  const anyActive = songs.some(s => {
    const st = status[songKey(s)];
    return st === 'resolving' || st === 'downloading';
  });
  btnDownloadAll.disabled = allDone || anyActive;
  btnDownloadAll.textContent = allDone ? '全部已下载' : '全部下载';
}

function loadSongs(refresh = false) {
  contentEl.innerHTML = '<div class="loading">检测中...</div>';

  const msgType = refresh ? 'REFRESH_SONGS' : 'GET_TAB_SONGS';

  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    currentTabId = tabs[0]?.id;

    if (!tabs[0]?.url?.includes('hifiti.com/thread-')) {
      contentEl.innerHTML = '<div class="empty">请在 hifiti.com 音乐页面使用本插件</div>';
      actionsEl.style.display = 'none';
      return;
    }

    chrome.runtime.sendMessage({ type: msgType }, (resp) => {
      if (chrome.runtime.lastError || !resp) {
        contentEl.innerHTML = '<div class="empty">获取失败，请点击「刷新」重试</div>';
        actionsEl.style.display = 'none';
        return;
      }
      renderSongs(resp.songs, resp.status);
    });
  });
}

function downloadSong(idx) {
  const song = currentSongs[idx];
  if (!song) return;
  chrome.runtime.sendMessage({
    type: 'DOWNLOAD_SONG',
    song,
    tabId: currentTabId,
  });
  setTimeout(() => loadSongs(), 500);
}

contentEl.addEventListener('click', (e) => {
  const btn = e.target.closest('[data-idx]');
  if (!btn) return;
  downloadSong(Number(btn.dataset.idx));
});

btnDownloadAll.addEventListener('click', () => {
  chrome.runtime.sendMessage({ type: 'GET_TAB_SONGS' }, (resp) => {
    if (!resp?.songs) return;
    resp.songs.forEach((song, i) => {
      const key = songKey(song);
      const st = resp.status[key];
      if (!st || st === 'error') {
        setTimeout(() => downloadSong(i), i * 300);
      }
    });
  });
});

btnRefresh.addEventListener('click', () => loadSongs(true));

loadSongs();

setInterval(() => {
  if (currentSongs.length > 0) loadSongs();
}, 2000);
