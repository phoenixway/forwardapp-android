import { contextBridge } from 'electron';
import { createRequire } from 'module';
import type { DesktopProject, DesktopProjectApi } from '../src/types/forward-shared';

type SharedProjectApi = {
  listProjects(): Promise<DesktopProject[]>;
  createProject(name: string, description: string | null, parentId: string | null): Promise<DesktopProject>;
  updateProject(id: string, name: string, description: string | null): Promise<DesktopProject>;
  deleteProject(id: string): Promise<void>;
  toggleProjectExpanded(id: string): Promise<DesktopProject | null>;
};

const requireFromHere = createRequire(__dirname);

const loadSharedApi = async (): Promise<SharedProjectApi> => {
  try {
    const sharedModule = requireFromHere('@forwardapp/shared-kmp') as {
      createDesktopProjectApi?: () => Promise<SharedProjectApi>;
      default?: () => Promise<SharedProjectApi>;
    };
    const factory = sharedModule.createDesktopProjectApi ?? sharedModule.default;
    if (!factory) {
      throw new Error('createDesktopProjectApi is missing from @forwardapp/shared-kmp');
    }
    return await factory();
  } catch (error) {
    console.error('[preload] Failed to load @forwardapp/shared-kmp', error);
    throw error;
  }
};

const sharedApiPromise = loadSharedApi();

const projectsBridge: DesktopProjectApi = {
  list: () => sharedApiPromise.then((api) => api.listProjects()),
  create: (name, description, parentId) =>
    sharedApiPromise.then((api) => api.createProject(name, description, parentId)),
  update: (id, name, description) =>
    sharedApiPromise.then((api) => api.updateProject(id, name, description)),
  remove: (id) => sharedApiPromise.then((api) => api.deleteProject(id)),
  toggle: (id) => sharedApiPromise.then((api) => api.toggleProjectExpanded(id)),
};

contextBridge.exposeInMainWorld('__forwardapp', {
  projects: projectsBridge,
});
