# CURRENT PROBLEM – Electron preload still fails to require shared KMP bundle

## Relevant Files

### apps/desktop/electron/main.ts
````ts
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

````

### apps/desktop/electron/preload.ts
````ts
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

````

### apps/desktop/src/App.tsx
````tsx
import React, { useEffect, useMemo, useState } from 'react';
import type { DesktopProject, DesktopProjectApi } from './types/forward-shared';

type DialogState =
  | { type: 'hidden' }
  | { type: 'create'; parentId: string | null }
  | { type: 'edit'; project: DesktopProject }
  | { type: 'delete'; project: DesktopProject };

interface ProjectListItem {
  project: DesktopProject;
  depth: number;
  hasChildren: boolean;
}

interface EditorValues {
  name: string;
  description: string;
  parentId: string | null;
}

const initialEditorValues: EditorValues = {
  name: '',
  description: '',
  parentId: null
};

const App = () => {
  const [api, setApi] = useState<DesktopProjectApi | null>(window.__forwardapp?.projects ?? null);
  const [projects, setProjects] = useState<DesktopProject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState>({ type: 'hidden' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [refreshToken, setRefreshToken] = useState(0);

  useEffect(() => {
    if (api) return;
    let cancelled = false;
    const probe = () => {
      if (cancelled) return;
      const bridge = window.__forwardapp?.projects ?? null;
      if (bridge) {
        setApi(bridge);
        setError(null);
      }
    };
    const interval = setInterval(probe, 300);
    probe();
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [api]);

  useEffect(() => {
    if (!api) {
      const diagnostics = window.__forwardappDiagnostics?.getSharedLoadError?.();
      const message = diagnostics?.message
        ? `Не вдалося знайти API проєктів: ${diagnostics.message}`
        : 'Не вдалося знайти API проєктів (перевір preload.ts)';
      console.error('[renderer] shared API not ready', diagnostics);
      setError(message);
      setLoading(false);
      return;
    }
    console.info('[renderer] shared API detected, start fetching projects');
    let cancelled = false;

    setLoading(true);
    setError(null);
    api
      .list()
      .then((items) => {
        if (cancelled) return;
        setProjects(items);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(`Не вдалося отримати проєкти: ${(err as Error).message}`);
      })
      .finally(() => {
        if (cancelled) return;
        setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [api, refreshToken]);

  const hierarchyItems = useMemo(() => buildHierarchy(projects), [projects]);

  const blockedParentIds = useMemo(() => {
    if (dialog.type !== 'edit') {
      return new Set<string>();
    }
    return collectDescendants(dialog.project.id, projects);
  }, [dialog, projects]);

  const handleCreate = async (values: EditorValues) => {
    if (!api) return;
    setIsSubmitting(true);
    try {
      await api.create(
        values.name.trim(),
        values.description.trim() || null,
        values.parentId
      );
      setDialog({ type: 'hidden' });
      setToast('Проєкт збережено');
      triggerRefresh();
    } catch (err) {
      setError(`Помилка створення: ${(err as Error).message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = async (project: DesktopProject, values: EditorValues) => {
    if (!api) return;
    setIsSubmitting(true);
    try {
      await api.update(project.id, values.name.trim(), values.description.trim() || null);
      setDialog({ type: 'hidden' });
      setToast('Проєкт оновлено');
      triggerRefresh();
    } catch (err) {
      setError(`Помилка збереження: ${(err as Error).message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (project: DesktopProject) => {
    if (!api) return;
    setIsSubmitting(true);
    try {
      await api.remove(project.id);
      setDialog({ type: 'hidden' });
      setToast('Проєкт видалено');
      triggerRefresh();
    } catch (err) {
      setError(`Помилка видалення: ${(err as Error).message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleExpanded = async (project: DesktopProject) => {
    if (!api) return;
    try {
      await api.toggle(project.id);
      triggerRefresh();
    } catch (err) {
      setError(`Не вдалося оновити стан: ${(err as Error).message}`);
    }
  };

  const openCreateDialog = (parentId: string | null = null) => {
    setDialog({ type: 'create', parentId });
    setToast(null);
  };

  const openEditDialog = (project: DesktopProject) => {
    setDialog({ type: 'edit', project });
    setToast(null);
  };

  const openDeleteDialog = (project: DesktopProject) => {
    setDialog({ type: 'delete', project });
    setToast(null);
  };

  const triggerRefresh = () => setRefreshToken((token) => token + 1);

  const editorDefaults = useMemo(() => {
    if (dialog.type === 'create') {
      return { ...initialEditorValues, parentId: dialog.parentId };
    }
    if (dialog.type === 'edit') {
      return {
        name: dialog.project.name ?? '',
        description: dialog.project.description ?? '',
        parentId: dialog.project.parentId ?? null
      };
    }
    return initialEditorValues;
  }, [dialog]);

  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">ForwardApp</p>
          <h1>Головний екран</h1>
          <p className="subtitle">
            Синхронізована з Android версією ієрархія проєктів на базі KMP-шару даних.
          </p>
        </div>
        <div className="header-actions">
          <button className="primary" onClick={() => openCreateDialog()}>
            + Новий проєкт
          </button>
        </div>
      </header>

      {toast && (
        <div className="banner success">
          <span>{toast}</span>
          <button onClick={() => setToast(null)}>×</button>
        </div>
      )}

      {error && (
        <div className="banner error">
          <span>{error}</span>
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}

      <main className="app-content">
        {loading ? (
          <div className="card muted">Завантаження даних...</div>
        ) : projects.length === 0 ? (
          <div className="card muted">
            <h2>Ще немає жодного проєкту</h2>
            <p>Створіть перший запис, щоб побачити роботу спільного KMP шару в дія.</p>
            <button className="primary ghost" onClick={() => openCreateDialog()}>
              Додати проєкт
            </button>
          </div>
        ) : (
          <section className="card">
            <div className="list-head">
              <h2>Мої проєкти</h2>
              <span>{projects.length} запис(ів)</span>
            </div>
            <ProjectList
              items={hierarchyItems}
              onAddChild={(id) => openCreateDialog(id)}
              onEdit={openEditDialog}
              onDelete={openDeleteDialog}
              onToggle={handleToggleExpanded}
            />
          </section>
        )}
      </main>

      {dialog.type === 'create' && (
        <ProjectEditorModal
          title={dialog.parentId ? 'Новий дочірній проєкт' : 'Новий проєкт'}
          isSubmitting={isSubmitting}
          defaultValues={editorDefaults}
          parentOptions={projects}
          onClose={() => setDialog({ type: 'hidden' })}
          onSubmit={(values) => handleCreate(values)}
        />
      )}

      {dialog.type === 'edit' && (
      <ProjectEditorModal
        title="Редагувати проєкт"
        isSubmitting={isSubmitting}
        defaultValues={editorDefaults}
        parentOptions={projects.filter(
          (project) => project.id !== dialog.project.id && !blockedParentIds.has(project.id)
        )}
        onClose={() => setDialog({ type: 'hidden' })}
        onSubmit={(values) => handleEdit(dialog.project, values)}
      />
      )}

      {dialog.type === 'delete' && (
        <ConfirmDialog
          title="Видалити проєкт?"
          description={`Дія безповоротна. Ви впевнені, що хочете видалити «${dialog.project.name}»?`}
          confirmLabel="Видалити"
          isSubmitting={isSubmitting}
          onConfirm={() => handleDelete(dialog.project)}
          onCancel={() => setDialog({ type: 'hidden' })}
        />
      )}
    </div>
  );
};

const ProjectList = ({
  items,
  onAddChild,
  onEdit,
  onDelete,
  onToggle
}: {
  items: ProjectListItem[];
  onAddChild: (projectId: string) => void;
  onEdit: (project: DesktopProject) => void;
  onDelete: (project: DesktopProject) => void;
  onToggle: (project: DesktopProject) => void;
}) => {
  if (items.length === 0) {
    return <div className="muted">Немає проєктів для відображення.</div>;
  }

  return (
    <ul className="project-list">
      {items.map((item) => (
        <li key={item.project.id} className="project-row" style={{ paddingLeft: `${item.depth * 24}px` }}>
          <div className="row-main">
            <div className="row-title">
              {item.hasChildren ? (
                <button className="icon-button" onClick={() => onToggle(item.project)}>
                  {item.project.isExpanded ? '▾' : '▸'}
                </button>
              ) : (
                <span className="icon-placeholder" />
              )}
              <div>
                <p className="project-name">{item.project.name || 'Без назви'}</p>
                {item.project.description && (
                  <p className="project-description">{item.project.description}</p>
                )}
              </div>
            </div>
            <div className="row-actions">
              <button onClick={() => onAddChild(item.project.id)}>Підпроєкт</button>
              <button onClick={() => onEdit(item.project)}>Редагувати</button>
              <button className="danger" onClick={() => onDelete(item.project)}>
                Видалити
              </button>
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
};

const ProjectEditorModal = ({
  title,
  isSubmitting,
  defaultValues,
  parentOptions,
  onClose,
  onSubmit
}: {
  title: string;
  isSubmitting: boolean;
  defaultValues: EditorValues;
  parentOptions: DesktopProject[];
  onClose: () => void;
  onSubmit: (values: EditorValues) => void;
}) => {
  const [values, setValues] = useState<EditorValues>(defaultValues);

  useEffect(() => {
    setValues(defaultValues);
  }, [defaultValues]);

  const handleChange = (field: keyof EditorValues, next: string) => {
    setValues((current) => ({
      ...current,
      [field]: field === 'parentId' ? (next || null) : next
    }));
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!values.name.trim()) {
      return;
    }
    onSubmit(values);
  };

  return (
    <Modal onClose={onClose}>
      <form className="modal" onSubmit={handleSubmit}>
        <h3>{title}</h3>
        <label>
          Назва
          <input
            type="text"
            value={values.name}
            disabled={isSubmitting}
            onChange={(event) => handleChange('name', event.target.value)}
            required
          />
        </label>
        <label>
          Опис
          <textarea
            value={values.description}
            disabled={isSubmitting}
            onChange={(event) => handleChange('description', event.target.value)}
            rows={3}
          />
        </label>
        <label>
          Батьківський проєкт
          <select
            value={values.parentId ?? ''}
            disabled={isSubmitting}
            onChange={(event) => handleChange('parentId', event.target.value)}
          >
            <option value="">(Кореневий рівень)</option>
            {parentOptions.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name || 'Без назви'}
              </option>
            ))}
          </select>
        </label>
        <div className="modal-actions">
          <button type="button" onClick={onClose} disabled={isSubmitting}>
            Скасувати
          </button>
          <button className="primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Збереження…' : 'Зберегти'}
          </button>
        </div>
      </form>
    </Modal>
  );
};

const ConfirmDialog = ({
  title,
  description,
  confirmLabel,
  isSubmitting,
  onConfirm,
  onCancel
}: {
  title: string;
  description: string;
  confirmLabel: string;
  isSubmitting: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) => (
  <Modal onClose={onCancel}>
    <div className="modal">
      <h3>{title}</h3>
      <p>{description}</p>
      <div className="modal-actions">
        <button onClick={onCancel} disabled={isSubmitting}>
          Скасувати
        </button>
        <button className="danger" onClick={onConfirm} disabled={isSubmitting}>
          {isSubmitting ? 'Видалення…' : confirmLabel}
        </button>
      </div>
    </div>
  </Modal>
);

const Modal = ({ children, onClose }: { children: React.ReactNode; onClose: () => void }) => {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div
      className="backdrop"
      role="dialog"
      aria-modal="true"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div className="modal-wrapper">{children}</div>
    </div>
  );
};

function buildHierarchy(projects: DesktopProject[]): ProjectListItem[] {
  const groups = new Map<string | null, DesktopProject[]>();
  for (const project of projects) {
    const key = project.parentId ?? null;
    const bucket = groups.get(key);
    if (bucket) {
      bucket.push(project);
    } else {
      groups.set(key, [project]);
    }
  }

  for (const bucket of groups.values()) {
    bucket.sort((a, b) => {
      if (a.goalOrder === b.goalOrder) {
        return a.createdAt - b.createdAt;
      }
      return a.goalOrder - b.goalOrder;
    });
  }

  const result: ProjectListItem[] = [];
  const visit = (parentId: string | null, depth: number) => {
    const items = groups.get(parentId);
    if (!items) return;
    for (const project of items) {
      const hasChildren = (groups.get(project.id)?.length ?? 0) > 0;
      result.push({ project, depth, hasChildren });
      if (project.isExpanded !== false) {
        visit(project.id, depth + 1);
      }
    }
  };

  visit(null, 0);
  return result;
}

function collectDescendants(rootId: string, projects: DesktopProject[]): Set<string> {
  const map = new Map<string, DesktopProject[]>();
  for (const project of projects) {
    const key = project.parentId ?? null;
    if (!map.has(key)) {
      map.set(key, []);
    }
    map.get(key)!.push(project);
  }

  const visited = new Set<string>();
  const walk = (id: string) => {
    const children = map.get(id);
    if (!children) return;
    for (const child of children) {
      if (visited.has(child.id)) continue;
      visited.add(child.id);
      walk(child.id);
    }
  };

  walk(rootId);
  visited.add(rootId);
  return visited;
}

export default App;

````

### apps/desktop/src/types/forward-shared.ts
````ts
export interface DesktopProject {
  id: string;
  name: string;
  description: string | null;
  parentId: string | null;
  goalOrder: number;
  createdAt: number;
  updatedAt: number;
  isExpanded: boolean;
}

export interface DesktopProjectApi {
  list(): Promise<DesktopProject[]>;
  create(name: string, description: string | null, parentId: string | null): Promise<DesktopProject>;
  update(id: string, name: string, description: string | null): Promise<DesktopProject>;
  remove(id: string): Promise<void>;
  toggle(id: string): Promise<DesktopProject | null>;
}

declare global {
  interface Window {
    __forwardapp?: {
      projects: DesktopProjectApi;
    };
    __forwardappDiagnostics?: {
      getSharedLoadError?: () => { message: string; stack?: string } | null;
    };
  }
}

export {};

````

### apps/desktop/tsconfig.electron.json
````json
{
  "compilerOptions": {
    "outDir": "./dist-electron",
    "module": "Node16",
    "moduleResolution": "Node16",
    "target": "ES2020",
    "lib": ["ES2020", "DOM"],
    "esModuleInterop": true,
    "strict": true,
    "skipLibCheck": true,
    "types": ["node", "electron"]
  },
  "include": ["electron/**/*.ts"]
}

````

### apps/desktop/package.json
````json
{
  "name": "@forwardapp/desktop",
  "version": "0.1.0",
  "private": true,
  "main": "dist-electron/electron/main.js",
  "scripts": {
    "dev": "concurrently -k \"pnpm dev:renderer\" \"pnpm dev:electron\"",
    "dev:renderer": "vite",
    "dev:electron": "concurrently -k \"pnpm dev:electron:tsc\" \"pnpm dev:electron:run\"",
    "dev:electron:tsc": "tsc -p tsconfig.electron.json --watch",
    "dev:electron:run": "wait-on dist-electron/electron/main.js && nodemon --watch dist-electron --ext js --exec \"cross-env VITE_DEV_SERVER_URL=http://localhost:5173 electron --no-sandbox dist-electron/electron/main.js\"",
    "build": "pnpm build:renderer && pnpm build:electron",
    "build:renderer": "vite build",
    "build:electron": "tsc -p tsconfig.electron.json",
    "preview": "vite preview"
  },
  "dependencies": {
    "@forwardapp/shared-kmp": "file:../../packages/shared-kmp/forwardapp-shared-kmp-0.1.2.tgz",
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  },
  "devDependencies": {
    "@types/node": "^20.14.12",
    "@types/react": "^18.3.3",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.2",
    "concurrently": "^8.2.2",
    "cross-env": "^7.0.3",
    "electron": "^32.1.2",
    "nodemon": "^3.1.7",
    "ts-node": "^10.9.2",
    "tslib": "^2.6.2",
    "wait-on": "^7.2.0",
    "typescript": "^5.5.4",
    "vite": "^5.4.1"
  },
  "packageManager": "pnpm@9.12.0"
}

````

### packages/shared-kmp/package.json
````json
{
  "name": "@forwardapp/shared-kmp",
  "version": "0.1.2",
  "private": false,
  "main": "dist/index.cjs",
  "module": "dist/index.mjs",
  "types": "dist/index.d.ts",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs",
      "types": "./dist/index.d.ts"
    }
  }
}

````

## Observed Errors
````text
Latest renderer console:
- Unable to load preload script …/dist-electron/electron/preload.js
- TypeError: require.resolve is not a function (when preload executed in sandbox)
Previous failures:
- __dirname is not defined
- process.cwd is not a function
- module not found: node:module / module not found: module
Renderer also shows 'Не вдалося отримати проєкти: …' and diagnostics `window.__forwardappDiagnostics` reporting the exception message.
````

## Reproduction / Demo
1. From repo root: `make shared-npm` ensures packages/shared-kmp/forwardapp-shared-kmp-0.1.2.tgz.
2. In apps/desktop: `pnpm install --no-frozen-lockfile` (blocked earlier by registry, must retry when possible).
3. Run `pnpm dev` (starts Vite + Electron). Electron window currently fails to fetch projects because preload cannot require shared package.
4. Open DevTools (View → Toggle Developer Tools) to observe `[preload]` logs and renderer errors.

## Background & Attempts
- Desktop Electron app consumes local npm tarball @forwardapp/shared-kmp built from Kotlin/JS module via make shared-npm.
- Preload needs to instantiate createDesktopProjectApi from that package to expose CRUD bridge to renderer (React main screen mirrors Android projects list).
- Electron is started through pnpm dev with contextIsolation true; sandbox restrictions prevented access to Node built-ins like module, __dirname, process.cwd, so bridge never initialised.
- We iteratively switched from createRequire→plain require, added logging, and now disable sandbox in BrowserWindow (sandbox: false) to regain Node APIs.
- Because pnpm install hit EAI_AGAIN earlier, node_modules may still reference forwardapp-shared-kmp-0.1.1; once network is available we must reinstall to ensure 0.1.2 which ships both CJS/ESM entries.

## Next Steps / Plan
1. Re-run pnpm install --no-frozen-lockfile so apps/desktop/node_modules picks up forwardapp-shared-kmp-0.1.2.tgz.
2. Restart pnpm dev; with sandbox disabled the preload should now access Node APIs. Check DevTools console for '[preload] Shared API is ready'.
3. If shared module still fails to load, inspect node_modules/.pnpm/@forwardapp+shared-kmp…/dist contents and compare with packages/shared-kmp/dist to ensure tarball matches.
4. Once preload exposes bridge, verify renderer CRUD (list/create/update/delete/toggle) works and document CLI steps in README/TESTING_MANUAL as needed.

## Additional Notes
- Можу надати більше логів або тимчасово змінити конфіг для додаткових перевірок.
- Renderer (`App.tsx`) уже містить діагностику через `window.__forwardappDiagnostics`; після виправлення preload вона має повернути null.
