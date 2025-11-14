import { contextBridge } from 'electron';
import { createDesktopProjectApi } from '@forwardapp/shared-kmp';

const apiPromise = createDesktopProjectApi();

const projectsBridge = {
  list: () => apiPromise.then((api) => api.listProjects()),
  create: (name: string, description: string | null, parentId: string | null) =>
    apiPromise.then((api) => api.createProject(name, description, parentId)),
  update: (id: string, name: string, description: string | null) =>
    apiPromise.then((api) => api.updateProject(id, name, description)),
  remove: (id: string) => apiPromise.then((api) => api.deleteProject(id)),
  toggle: (id: string) => apiPromise.then((api) => api.toggleProjectExpanded(id))
};

contextBridge.exposeInMainWorld('__forwardapp', {
  projects: projectsBridge
});
