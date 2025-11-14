import { app, BrowserWindow } from 'electron';
import path from 'node:path';

const resolvedDirname = __dirname;

const isDev = Boolean(process.env.VITE_DEV_SERVER_URL);

const createMainWindow = () => {
  const window = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 960,
    minHeight: 600,
    show: false,
    title: 'ForwardApp Desktop',
    backgroundColor: '#05010a',
    webPreferences: {
      contextIsolation: true,
      preload: path.join(resolvedDirname, 'preload.js'),
      sandbox: false
    }
  });

  if (isDev && process.env.VITE_DEV_SERVER_URL) {
    window.loadURL(process.env.VITE_DEV_SERVER_URL);
  } else {
    const htmlPath = path.join(resolvedDirname, '..', '..', 'dist', 'index.html');
    window.loadFile(htmlPath);
  }

  window.once('ready-to-show', () => window.show());
};

app.whenReady().then(() => {
  createMainWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
