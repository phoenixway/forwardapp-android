import { contextBridge } from 'electron';
import type { DesktopProject, DesktopProjectApi } from '../src/types/forward-shared';

type SharedProjectApi = {
  listProjects(): Promise<DesktopProject[]>;
  createProject(name: string, description: string | null, parentId: string | null): Promise<DesktopProject>;
  updateProject(id: string, name: string, description: string | null): Promise<DesktopProject>;
  deleteProject(id: string): Promise<void>;
  toggleProjectExpanded(id: string): Promise<DesktopProject | null>;
};

let lastLoadError: Error | null = null;

const describeDirname = typeof __dirname === 'string' ? __dirname : '<unavailable>';
const describeCwd = typeof process !== 'undefined' && typeof process.cwd === 'function' ? process.cwd() : '<unavailable>';

type SharedModuleFactory = () => Promise<SharedProjectApi>;
type SharedModuleNamespace = {
  default?: unknown;
  createDesktopProjectApi?: SharedModuleFactory;
};

const isFactory = (value: unknown): value is SharedModuleFactory => typeof value === 'function';

const isFactoryContainer = (value: unknown): value is { createDesktopProjectApi: SharedModuleFactory } =>
  typeof value === 'object' &&
  value !== null &&
  'createDesktopProjectApi' in (value as Record<string, unknown>) &&
  typeof (value as Record<string, unknown>).createDesktopProjectApi === 'function';

const readNamespacedFactory = (candidate: unknown): SharedModuleFactory | null => {
  if (!candidate || typeof candidate !== 'object') {
    return null;
  }
  try {
    const root = candidate as Record<string, unknown>;
    const desktopNamespace = (root.com as Record<string, unknown> | undefined)
      ?.romankozak as Record<string, unknown> | undefined;
    const sharedNamespace = desktopNamespace?.forwardappmobile as Record<string, unknown> | undefined;
    const featuresNamespace = sharedNamespace?.shared as Record<string, unknown> | undefined;
    const desktopFeatures = featuresNamespace?.features as Record<string, unknown> | undefined;
    const desktopModule = desktopFeatures?.desktop as Record<string, unknown> | undefined;
    const factory = desktopModule?.createDesktopProjectApi;
    return typeof factory === 'function' ? (factory as SharedModuleFactory) : null;
  } catch {
    return null;
  }
};

const loadSharedApi = async (): Promise<SharedProjectApi> => {
  console.info('[preload] Attempting to load @forwardapp/shared-kmp (CJS entry)');
  try {
    console.info('[preload] dirname=%s cwd=%s', describeDirname, describeCwd);
    console.info('[preload] Attempting dynamic require of @forwardapp/shared-kmp');
    const sharedModule = require('@forwardapp/shared-kmp') as SharedModuleNamespace;
    console.info(
      '[preload] Loaded shared module keys=%o defaultType=%s',
      Object.keys(sharedModule ?? {}),
      typeof sharedModule.default
    );
    const defaultExport = sharedModule.default;

    let rootFactory: SharedModuleFactory | null = null;

    if (isFactory(sharedModule.createDesktopProjectApi)) {
      rootFactory = sharedModule.createDesktopProjectApi;
    } else if (isFactory(defaultExport)) {
      rootFactory = defaultExport;
    } else if (isFactoryContainer(defaultExport)) {
      rootFactory = defaultExport.createDesktopProjectApi;
    } else {
      rootFactory = readNamespacedFactory(sharedModule) ?? readNamespacedFactory(defaultExport);
    }

    if (rootFactory) {
      console.info('[preload] Found factory on shared export, invoking…');
      return await rootFactory();
    }
    if (defaultExport && typeof defaultExport === 'object') {
      console.warn('[preload] Falling back to legacy default object export');
      return defaultExport as SharedProjectApi;
    }
    throw new Error('createDesktopProjectApi is missing from @forwardapp/shared-kmp root export');
  } catch (error) {
    console.error('[preload] Failed to load shared KMP bundle', error);
    lastLoadError = error instanceof Error ? error : new Error(String(error));
    throw error;
  }
};

const sharedApiPromise = loadSharedApi()
  .then((api) => {
    console.info('[preload] Shared API is ready');
    lastLoadError = null;
    return api;
  })
  .catch((err) => {
    console.error('[preload] Shared API failed to initialise', err);
    throw err;
  });

const projectsBridge: DesktopProjectApi = {
  list: () =>
    sharedApiPromise.then((api) => {
      console.info('[preload] listProjects called');
      return api.listProjects();
    }),
  create: (name, description, parentId) =>
    sharedApiPromise.then((api) => {
      console.info('[preload] createProject called');
      return api.createProject(name, description, parentId);
    }),
  update: (id, name, description) =>
    sharedApiPromise.then((api) => {
      console.info('[preload] updateProject called');
      return api.updateProject(id, name, description);
    }),
  remove: (id) =>
    sharedApiPromise.then((api) => {
      console.info('[preload] deleteProject called');
      return api.deleteProject(id);
    }),
  toggle: (id) =>
    sharedApiPromise.then((api) => {
      console.info('[preload] toggleProjectExpanded called');
      return api.toggleProjectExpanded(id);
    }),
};

contextBridge.exposeInMainWorld('__forwardapp', {
  projects: projectsBridge,
});

contextBridge.exposeInMainWorld('__forwardappDiagnostics', {
  getSharedLoadError: () => {
    if (!lastLoadError) return null;
    return {
      message: lastLoadError.message,
      stack: lastLoadError.stack,
    };
  },
});
