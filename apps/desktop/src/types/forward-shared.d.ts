declare module '@forwardapp/shared-kmp' {
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
    listProjects(): Promise<DesktopProject[]>;
    createProject(name: string, description: string | null, parentId: string | null): Promise<DesktopProject>;
    updateProject(id: string, name: string, description: string | null): Promise<DesktopProject>;
    deleteProject(id: string): Promise<void>;
    toggleProjectExpanded(id: string): Promise<DesktopProject | null>;
  }

  export function createDesktopProjectApi(): Promise<DesktopProjectApi>;
}
