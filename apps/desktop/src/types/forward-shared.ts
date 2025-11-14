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
  }
}

export {};
