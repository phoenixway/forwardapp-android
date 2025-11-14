import { contextBridge } from 'electron';
import type { DesktopProject, DesktopProjectApi } from '../src/types/forward-shared';

type SharedProjectApi = {
  listProjects(): Promise<DesktopProject[]>;
  createProject(name: string, description: string | null, parentId: string | null): Promise<DesktopProject>;
  updateProject(id: string, name: string, description: string | null): Promise<DesktopProject>;
  deleteProject(id: string): Promise<void>;
  toggleProjectExpanded(id: string): Promise<DesktopProject | null>;
};

const loadSharedApi = (): Promise<SharedProjectApi> => {
  const sharedModule = require('@forwardapp/shared-kmp') as {
    createDesktopProjectApi?: () => Promise<SharedProjectApi>;
    default?: () => Promise<SharedProjectApi>;
  };
  const factory = sharedModule.createDesktopProjectApi ?? sharedModule.default;
  if (!factory) {
    return Promise.reject(
      new Error('createDesktopProjectApi is missing from @forwardapp/shared-kmp'),
    );
  }
  return factory();
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
