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
  const [api] = useState<DesktopProjectApi | null>(window.__forwardapp?.projects ?? null);
  const [projects, setProjects] = useState<DesktopProject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState>({ type: 'hidden' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [refreshToken, setRefreshToken] = useState(0);

  useEffect(() => {
    if (!api) {
      setError('Не вдалося знайти API проєктів (перевір preload.ts)');
      setLoading(false);
      return;
    }
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
