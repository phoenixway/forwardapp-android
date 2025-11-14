import { contextBridge } from 'electron';

// Reserved for future IPC bridging. Keeps the preload script explicit.
contextBridge.exposeInMainWorld('__forwardapp', {});
