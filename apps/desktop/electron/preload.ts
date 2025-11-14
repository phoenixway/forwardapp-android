import { contextBridge } from 'electron';

declare global {
  interface Window {
    __forwardapp?: Record<string, never>;
  }
}

contextBridge.exposeInMainWorld('__forwardapp', {});
