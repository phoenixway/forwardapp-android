import { contextBridge } from 'electron';

type SharedProjectApi = {
  listProjects(): Promise<unknown[]>;
  createProject(name: string, description: string | null, parentId: string | null): Promise<unknown>;
  updateProject(id: string, name: string, description: string | null): Promise<unknown>;
  deleteProject(id: string): Promise<void>;
  toggleProjectExpanded(id: string): Promise<unknown>;
};

type RendererProjectBridge = {
  list: () => Promise<unknown[]>;
  create: (name: string, description: string | null, parentId: string | null) => Promise<unknown>;
  update: (id: string, name: string, description: string | null) => Promise<unknown>;
  remove: (id: string) => Promise<void>;
  toggle: (id: string) => Promise<unknown>;
};

const sharedApiPromise: Promise<SharedProjectApi> = import('@forwardapp/shared-kmp').then(
  async (moduleLike) => {
    const candidate = moduleLike as { createDesktopProjectApi?: () => Promise<SharedProjectApi> };
    const factory = candidate.createDesktopProjectApi;
    if (!factory) {
      throw new Error('createDesktopProjectApi is missing from @forwardapp/shared-kmp');
    }
    return factory();
  },
);

const projectsBridge: RendererProjectBridge = {
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
