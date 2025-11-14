import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { DesktopProject, DesktopProjectApi } from './types/forward-shared';

type DialogState =
  | { type: 'hidden' }
  | { type: 'create'; parentId: string | null }
  | { type: 'edit'; project: DesktopProject }
  | { type: 'delete'; project: DesktopProject };

interface EditorValues {
  name: string;
  description: string;
  parentId: string | null;
}

const initialEditorValues: EditorValues = {
  name: '',
  description: '',
  parentId: null,
};

interface WorkspaceTabBase {
  id: string;
  title: string;
  isClosable: boolean;
}

interface HomeWorkspaceTab extends WorkspaceTabBase {
  type: 'home';
  isClosable: false;
}

interface SettingsWorkspaceTab extends WorkspaceTabBase {
  type: 'settings';
}

interface ProjectWorkspaceTab extends WorkspaceTabBase {
  type: 'project';
  projectId: string;
}

type WorkspaceTab = HomeWorkspaceTab | SettingsWorkspaceTab | ProjectWorkspaceTab;

const homeTab = (): HomeWorkspaceTab => ({
  id: 'home',
  title: 'Головний екран',
  type: 'home',
  isClosable: false,
});

const App = () => {
  const [api, setApi] = useState<DesktopProjectApi | null>(
    window.__forwardapp?.projects ?? null,
  );
  const [projects, setProjects] = useState<DesktopProject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState>({ type: 'hidden' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [refreshToken, setRefreshToken] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [tabs, setTabs] = useState<WorkspaceTab[]>([homeTab()]);
  const [activeTabId, setActiveTabId] = useState<string>('home');

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

  const groupedProjects = useMemo(() => groupProjects(projects), [projects]);
  const isFiltering = searchTerm.trim().length > 0;
  const visibleProjectIds = useMemo(
    () => getVisibleProjectIds(projects, groupedProjects, searchTerm),
    [projects, groupedProjects, searchTerm],
  );

  const openProjectTab = useCallback((project: DesktopProject) => {
    setTabs((prev) => {
      const tabId = `project-${project.id}`;
      const exists = prev.some((tab) => tab.id === tabId);
      if (exists) {
        return prev.map((tab) =>
          tab.id === tabId && tab.type === 'project'
            ? { ...tab, title: project.name || 'Без назви' }
            : tab,
        );
      }
      return [
        ...prev,
        {
          id: tabId,
          title: project.name || 'Без назви',
          type: 'project',
          projectId: project.id,
          isClosable: true,
        },
      ];
    });
    setActiveTabId(`project-${project.id}`);
  }, []);

  const openSettingsTab = useCallback(() => {
    setTabs((prev) => {
      const exists = prev.some((tab) => tab.id === 'settings');
      if (exists) return prev;
      return [
        ...prev,
        { id: 'settings', title: 'Налаштування', type: 'settings', isClosable: true },
      ];
    });
    setActiveTabId('settings');
  }, []);

  useEffect(() => {
    setTabs((prev) =>
      prev.map((tab) => {
        if (tab.type !== 'project') return tab;
        const linkedProject = projects.find((project) => project.id === tab.projectId);
        return linkedProject ? { ...tab, title: linkedProject.name || 'Без назви' } : tab;
      }),
    );
  }, [projects]);

  const handleTabClose = useCallback(
    (tabId: string) => {
      setTabs((prev) => {
        const closing = prev.find((tab) => tab.id === tabId);
        if (!closing || !closing.isClosable) {
          return prev;
        }

        const nextTabs = prev.filter((tab) => tab.id !== tabId);
        if (activeTabId === tabId) {
          const closingIndex = prev.findIndex((tab) => tab.id === tabId);
          const fallback = nextTabs[closingIndex - 1] || nextTabs[0] || homeTab();
          setActiveTabId(fallback.id);
        }

        return nextTabs.length > 0 ? nextTabs : [homeTab()];
      });
    },
    [activeTabId],
  );

  const activeTab = tabs.find((tab) => tab.id === activeTabId) || tabs[0] || homeTab();
  const activeProjectId = activeTab?.type === 'project' ? activeTab.projectId : null;

  const handleCreate = async (values: EditorValues) => {
    if (!api) return;
    setIsSubmitting(true);
    try {
      await api.create(values.name.trim(), values.description.trim() || null, values.parentId);
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
        parentId: dialog.project.parentId ?? null,
      };
    }
    return initialEditorValues;
  }, [dialog]);

  const blockedParentIds = useMemo(() => {
    if (dialog.type !== 'edit') {
      return new Set<string>();
    }
    return collectDescendants(dialog.project.id, projects);
  }, [dialog, projects]);

  const handleMenuCommand = (command: 'home' | 'settings' | 'refresh') => {
    if (command === 'home') {
      setActiveTabId('home');
      return;
    }
    if (command === 'settings') {
      openSettingsTab();
      return;
    }
    triggerRefresh();
  };

  const renderActiveTabContent = () => {
    if (loading) {
      return <div className="card muted">Завантаження робочого простору…</div>;
    }

    if (activeTab.type === 'home') {
      return (
        <HomeSummary
          projects={projects}
          onCreate={() => openCreateDialog()}
          onOpenProject={(project) => openProjectTab(project)}
        />
      );
    }

    if (activeTab.type === 'settings') {
      return <SettingsPlaceholder />;
    }

    const project = projects.find((item) => item.id === activeTab.projectId);
    return <ProjectPlaceholder project={project} onEdit={openEditDialog} />;
  };

  return (
    <div className="desktop-frame">
      <Sidebar
        projects={projects}
        groupedProjects={groupedProjects}
        visibleIds={visibleProjectIds}
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        loading={loading}
        isFiltering={isFiltering}
        activeProjectId={activeProjectId}
        onCreateRoot={() => openCreateDialog(null)}
        onCreateChild={(parentId) => openCreateDialog(parentId)}
        onOpenProject={openProjectTab}
        onToggleProject={handleToggleExpanded}
        onEditProject={openEditDialog}
        onDeleteProject={openDeleteDialog}
        onShowHome={() => handleMenuCommand('home')}
        onShowSettings={() => handleMenuCommand('settings')}
      />

      <section className="workspace">
        <MenuBar
          onCommand={handleMenuCommand}
          onCreate={() => openCreateDialog(null)}
          totalProjects={projects.length}
        />

        <section className="hero-card">
          <div>
            <p className="eyebrow">ForwardApp Desktop 2</p>
            <h1>Головний екран</h1>
            <p className="subtitle">
              Такий самий макет, як у десктоп1: таби, сайдбар і головне меню, але з даними KMP
              шару. Відкриття проєкту показує тимчасову заглушку.
            </p>
            <div className="hero-actions">
              <button className="primary" onClick={() => openCreateDialog()}>
                + Новий проєкт
              </button>
              <button className="ghost" onClick={() => handleMenuCommand('refresh')}>
                Оновити дані
              </button>
            </div>
          </div>
          <div className="hero-stats">
            <StatCard label="Всього проєктів" value={projects.length} />
            <StatCard label="Кореневі" value={groupedProjects.get(null)?.length ?? 0} />
            <StatCard
              label="Оновлені за добу"
              value={projects.filter((p) => Date.now() - p.updatedAt < 86_400_000).length}
            />
          </div>
        </section>

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

        <WorkspaceTabs
          tabs={tabs}
          activeTabId={activeTabId}
          onSelect={setActiveTabId}
          onClose={handleTabClose}
        />

        <div className="tab-panel">{renderActiveTabContent()}</div>
      </section>

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
            (project) => project.id !== dialog.project.id && !blockedParentIds.has(project.id),
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

interface SidebarProps {
  projects: DesktopProject[];
  groupedProjects: Map<string | null, DesktopProject[]>;
  visibleIds: Set<string>;
  searchTerm: string;
  onSearchChange: (value: string) => void;
  loading: boolean;
  isFiltering: boolean;
  activeProjectId: string | null;
  onCreateRoot: () => void;
  onCreateChild: (parentId: string) => void;
  onOpenProject: (project: DesktopProject) => void;
  onToggleProject: (project: DesktopProject) => void;
  onEditProject: (project: DesktopProject) => void;
  onDeleteProject: (project: DesktopProject) => void;
  onShowHome: () => void;
  onShowSettings: () => void;
}

const Sidebar = ({
  projects,
  groupedProjects,
  visibleIds,
  searchTerm,
  onSearchChange,
  loading,
  isFiltering,
  activeProjectId,
  onCreateRoot,
  onCreateChild,
  onOpenProject,
  onToggleProject,
  onEditProject,
  onDeleteProject,
  onShowHome,
  onShowSettings,
}: SidebarProps) => {
  const rootProjects = groupedProjects.get(null) ?? [];

  const treeContent = useMemo(() => {
    const renderBranch = (parentId: string | null, depth: number): React.ReactNode => {
      const items = groupedProjects.get(parentId) ?? [];
      return items.map((project) => {
        const shouldShow = !isFiltering || visibleIds.has(project.id);
        if (!shouldShow) return null;

        const children = groupedProjects.get(project.id) ?? [];
        const hasChildren = children.length > 0;
        const hasVisibleChildren = children.some((child) => visibleIds.has(child.id));
        const isExpanded = isFiltering ? hasVisibleChildren : project.isExpanded !== false;

        return (
          <React.Fragment key={project.id}>
            <ProjectTreeItem
              project={project}
              depth={depth}
              hasChildren={hasChildren}
              isExpanded={isExpanded}
              isActive={activeProjectId === project.id}
              filterTerm={searchTerm}
              onToggle={() => onToggleProject(project)}
              onOpen={() => onOpenProject(project)}
              onAddChild={() => onCreateChild(project.id)}
              onEdit={() => onEditProject(project)}
              onDelete={() => onDeleteProject(project)}
            />
            {hasChildren && isExpanded && renderBranch(project.id, depth + 1)}
          </React.Fragment>
        );
      });
    };

    return renderBranch(null, 0);
  }, [
    groupedProjects,
    isFiltering,
    visibleIds,
    activeProjectId,
    searchTerm,
    onToggleProject,
    onOpenProject,
    onCreateChild,
    onEditProject,
    onDeleteProject,
  ]);

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div>
          <p className="eyebrow">ForwardApp</p>
          <strong>Проєкти</strong>
        </div>
        <button className="primary" onClick={onCreateRoot}>
          +
        </button>
      </div>

      <div className="sidebar-search">
        <input
          type="search"
          value={searchTerm}
          placeholder="Пошук проєкту"
          onChange={(event) => onSearchChange(event.target.value)}
        />
      </div>

      <div className="sidebar-menu">
        <button onClick={onShowHome}>Головна</button>
        <button onClick={onShowSettings}>Налаштування</button>
        <button onClick={() => onSearchChange('')}>Очистити</button>
      </div>

      <div className="sidebar-section">
        <p className="sidebar-subtitle">Мої проєкти</p>
        {loading ? (
          <div className="muted">Завантаження…</div>
        ) : projects.length === 0 ? (
          <div className="muted">Ще немає жодного проєкту.</div>
        ) : rootProjects.length === 0 ? (
          <div className="muted">Створіть перший кореневий проєкт.</div>
        ) : (
          <div className="project-tree">{treeContent}</div>
        )}

        {isFiltering && visibleIds.size === 0 && <div className="muted">Нічого не знайдено</div>}
      </div>
    </aside>
  );
};

interface ProjectTreeItemProps {
  project: DesktopProject;
  depth: number;
  hasChildren: boolean;
  isExpanded: boolean;
  isActive: boolean;
  filterTerm: string;
  onToggle: () => void;
  onOpen: () => void;
  onAddChild: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

const ProjectTreeItem = ({
  project,
  depth,
  hasChildren,
  isExpanded,
  isActive,
  filterTerm,
  onToggle,
  onOpen,
  onAddChild,
  onEdit,
  onDelete,
}: ProjectTreeItemProps) => {
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (!(event.target instanceof HTMLElement)) return;
      if (!event.target.closest(`[data-menu="${project.id}"]`)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [project.id]);

  return (
    <div className={`tree-row ${isActive ? 'active' : ''}`} style={{ paddingLeft: `${depth * 16}px` }}>
      <div className="tree-main" onClick={onOpen}>
        {hasChildren ? (
          <button
            className="tree-toggle"
            onClick={(event) => {
              event.stopPropagation();
              onToggle();
            }}
          >
            {isExpanded ? '▾' : '▸'}
          </button>
        ) : (
          <span className="tree-toggle spacer" />
        )}
        <div className="tree-label">
          <span>{highlightMatch(project.name || 'Без назви', filterTerm)}</span>
          {project.description && <small>{project.description}</small>}
        </div>
      </div>
      <div className="tree-actions">
        <button
          title="Підпроєкт"
          onClick={(event) => {
            event.stopPropagation();
            onAddChild();
          }}
        >
          +
        </button>
        <div className="tree-menu" data-menu={project.id}>
          <button
            onClick={(event) => {
              event.stopPropagation();
              setMenuOpen((value) => !value);
            }}
          >
            ⋯
          </button>
          {menuOpen && (
            <div className="tree-menu-popover">
              <button
                onClick={() => {
                  setMenuOpen(false);
                  onOpen();
                }}
              >
                Відкрити
              </button>
              <button
                onClick={() => {
                  setMenuOpen(false);
                  onEdit();
                }}
              >
                Редагувати
              </button>
              <button
                className="danger"
                onClick={() => {
                  setMenuOpen(false);
                  onDelete();
                }}
              >
                Видалити
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

interface MenuBarProps {
  onCommand: (command: 'home' | 'settings' | 'refresh') => void;
  onCreate: () => void;
  totalProjects: number;
}

const MenuBar = ({ onCommand, onCreate, totalProjects }: MenuBarProps) => (
  <nav className="menu-bar">
    <button onClick={() => onCommand('home')}>Головна</button>
    <button onClick={() => onCommand('settings')}>Налаштування</button>
    <button onClick={() => onCommand('refresh')}>Синхронізація</button>
    <div className="menu-spacer" />
    <span className="menu-indicator">{totalProjects} проєкт(ів)</span>
    <button className="primary" onClick={onCreate}>
      + Створити
    </button>
  </nav>
);

interface TabsProps {
  tabs: WorkspaceTab[];
  activeTabId: string;
  onSelect: (tabId: string) => void;
  onClose: (tabId: string) => void;
}

const WorkspaceTabs = ({ tabs, activeTabId, onSelect, onClose }: TabsProps) => (
  <div className="tab-strip">
    {tabs.map((tab) => (
      <button
        key={tab.id}
        className={`tab-button ${tab.id === activeTabId ? 'active' : ''}`}
        onClick={() => onSelect(tab.id)}
      >
        <span>{tab.title}</span>
        {tab.isClosable && (
          <span
            className="tab-close"
            onClick={(event) => {
              event.stopPropagation();
              onClose(tab.id);
            }}
          >
            ×
          </span>
        )}
      </button>
    ))}
  </div>
);

const StatCard = ({ label, value }: { label: string; value: number }) => (
  <div className="stat-card">
    <p>{label}</p>
    <strong>{value}</strong>
  </div>
);

const HomeSummary = ({
  projects,
  onCreate,
  onOpenProject,
}: {
  projects: DesktopProject[];
  onCreate: () => void;
  onOpenProject: (project: DesktopProject) => void;
}) => {
  const recent = [...projects]
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .slice(0, 3);

  const inactive = projects.filter((project) => project.isExpanded === false);

  return (
    <div className="home-grid">
      <div className="placeholder-card">
        <h2>Останні оновлення</h2>
        {recent.length === 0 ? (
          <p className="muted">Ще немає активності.</p>
        ) : (
          <ul>
            {recent.map((project) => (
              <li key={project.id}>
                <button onClick={() => onOpenProject(project)}>{project.name || 'Без назви'}</button>
                <small>
                  Оновлено {new Date(project.updatedAt).toLocaleDateString()} ·{' '}
                  {project.description || 'Без опису'}
                </small>
              </li>
            ))}
          </ul>
        )}
        <button className="primary" onClick={onCreate}>
          + Новий проєкт
        </button>
      </div>

      <div className="placeholder-card">
        <h2>Структура</h2>
        <p>Активних гілок: {projects.filter((project) => project.isExpanded !== false).length}</p>
        <p>Згорнуті гілки: {inactive.length}</p>
        <p>Середня глибина: {calculateAverageDepth(projects).toFixed(1)}</p>
      </div>
    </div>
  );
};

const ProjectPlaceholder = ({
  project,
  onEdit,
}: {
  project?: DesktopProject;
  onEdit: (project: DesktopProject) => void;
}) => {
  if (!project) {
    return <div className="card muted">Проєкт не знайдено.</div>;
  }

  return (
    <div className="placeholder-card">
      <p className="eyebrow">Тимчасовий перегляд</p>
      <h2>{project.name || 'Без назви'}</h2>
      <p>
        Тут буде реальний контент проєкту. Поки що показуємо заглушку, щоб можна було перевірити
        новий UI, вкладки та взаємодію із сайдбаром.
      </p>
      {project.description && <p className="muted">Опис: {project.description}</p>}
      <div className="hero-actions">
        <button className="ghost" onClick={() => onEdit(project)}>
          Редагувати
        </button>
      </div>
    </div>
  );
};

const SettingsPlaceholder = () => (
  <div className="placeholder-card">
    <h2>Налаштування</h2>
    <p>
      У десктоп2 цей розділ повторює поведінку десктоп1: вкладка відкривається через меню або
      гарячі дії. Реальний вміст буде додано після готовності KMP-шару.
    </p>
    <ul>
      <li>Глобальна тема</li>
      <li>Wi-Fi синхронізація</li>
      <li>Підключення Obsidian</li>
    </ul>
  </div>
);

const highlightMatch = (text: string, query: string): React.ReactNode => {
  if (!query.trim()) return text;
  const lowerText = text.toLowerCase();
  const lowerQuery = query.trim().toLowerCase();
  const index = lowerText.indexOf(lowerQuery);
  if (index === -1) return text;
  return (
    <>
      {text.slice(0, index)}
      <mark>{text.slice(index, index + lowerQuery.length)}</mark>
      {text.slice(index + lowerQuery.length)}
    </>
  );
};

const groupProjects = (projects: DesktopProject[]) => {
  const map = new Map<string | null, DesktopProject[]>();
  for (const project of projects) {
    const key = project.parentId ?? null;
    const bucket = map.get(key);
    if (bucket) {
      bucket.push(project);
    } else {
      map.set(key, [project]);
    }
  }
  for (const bucket of map.values()) {
    bucket.sort((a, b) => {
      if (a.goalOrder === b.goalOrder) {
        return a.createdAt - b.createdAt;
      }
      return a.goalOrder - b.goalOrder;
    });
  }
  return map;
};

const getVisibleProjectIds = (
  projects: DesktopProject[],
  groupedProjects: Map<string | null, DesktopProject[]>,
  searchTerm: string,
) => {
  const normalized = searchTerm.trim().toLowerCase();
  if (!normalized) {
    return new Set(projects.map((project) => project.id));
  }

  const parentMap = new Map(projects.map((project) => [project.id, project.parentId ?? null]));
  const result = new Set<string>();

  const markAncestors = (projectId: string | null) => {
    let current: string | null = projectId;
    while (current) {
      if (result.has(current)) break;
      result.add(current);
      current = parentMap.get(current) ?? null;
    }
  };

  const includeChildren = (projectId: string) => {
    const children = groupedProjects.get(projectId) ?? [];
    for (const child of children) {
      if (!result.has(child.id)) {
        result.add(child.id);
        includeChildren(child.id);
      }
    }
  };

  for (const project of projects) {
    const haystack = `${project.name ?? ''} ${project.description ?? ''}`.toLowerCase();
    if (haystack.includes(normalized)) {
      result.add(project.id);
      markAncestors(parentMap.get(project.id) ?? null);
      includeChildren(project.id);
    }
  }

  return result;
};

function collectDescendants(rootId: string, projects: DesktopProject[]): Set<string> {
  const map = new Map<string, DesktopProject[]>();
  for (const project of projects) {
    if (!project.parentId) continue;
    if (!map.has(project.parentId)) {
      map.set(project.parentId, []);
    }
    map.get(project.parentId)!.push(project);
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

function calculateAverageDepth(projects: DesktopProject[]): number {
  if (projects.length === 0) return 0;
  const depthMap = new Map<string, number>();
  const findDepth = (project: DesktopProject): number => {
    if (!project.parentId) return 0;
    if (depthMap.has(project.parentId)) {
      return (depthMap.get(project.parentId) ?? 0) + 1;
    }
    const parent = projects.find((item) => item.id === project.parentId);
    if (!parent) return 0;
    const depth = findDepth(parent) + 1;
    depthMap.set(project.parentId, depth - 1);
    return depth;
  };

  const total = projects.reduce((sum, project) => sum + findDepth(project), 0);
  return total / projects.length;
}

interface ProjectEditorModalProps {
  title: string;
  isSubmitting: boolean;
  defaultValues: EditorValues;
  parentOptions: DesktopProject[];
  onClose: () => void;
  onSubmit: (values: EditorValues) => void;
}

const ProjectEditorModal = ({
  title,
  isSubmitting,
  defaultValues,
  parentOptions,
  onClose,
  onSubmit,
}: ProjectEditorModalProps) => {
  const [values, setValues] = useState<EditorValues>(defaultValues);

  useEffect(() => {
    setValues(defaultValues);
  }, [defaultValues]);

  const handleChange = (field: keyof EditorValues, next: string) => {
    setValues((current) => ({
      ...current,
      [field]: field === 'parentId' ? (next || null) : next,
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

interface ConfirmDialogProps {
  title: string;
  description: string;
  confirmLabel: string;
  isSubmitting: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

const ConfirmDialog = ({
  title,
  description,
  confirmLabel,
  isSubmitting,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) => (
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

export default App;
