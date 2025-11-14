import { contextBridge } from 'electron';
import type { DesktopProjectApi } from '../src/types/forward-shared';

const sharedApiPromise = import('@forwardapp/shared-kmp').then(async (mod) => {
  const apiFactory = (mod as any).createDesktopProjectApi;
  if (!apiFactory) {
    throw new Error('createDesktopProjectApi is missing from @forwardapp/shared-kmp');
  }
  return apiFactory();
});

const projectsBridge: DesktopProjectApi = {
  list: () => sharedApiPromise.then((api: any) => api.listProjects()),
  create: (name, description, parentId) =>
    sharedApiPromise.then((api: any) => api.createProject(name, description, parentId)),
  update: (id, name, description) =>
    sharedApiPromise.then((api: any) => api.updateProject(id, name, description)),
  remove: (id) => sharedApiPromise.then((api: any) => api.deleteProject(id)),
  toggle: (id) => sharedApiPromise.then((api: any) => api.toggleProjectExpanded(id))
};

contextBridge.exposeInMainWorld('__forwardapp', {
  projects: projectsBridge
});
