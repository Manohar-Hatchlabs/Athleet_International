import { createContext, useContext, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { AccessLevel, AppModule, MyAccess } from '../api/types';

interface OrgState {
  orgId: string;
  access: MyAccess;
  can: (module: AppModule, level?: AccessLevel) => boolean;
  centerLabel: string;
}

const OrgContext = createContext<OrgState | null>(null);

const RANK: Record<AccessLevel, number> = { NONE: 0, VIEW: 1, MANAGE: 2 };

export function useMyAccess(orgId: string | undefined) {
  return useQuery({
    queryKey: ['my-access', orgId],
    queryFn: () => api.get<MyAccess>(`/organizations/${orgId}/my-access`),
    enabled: !!orgId,
  });
}

export function OrgProvider({ access, children }: { access: MyAccess; children: ReactNode }) {
  const can = (module: AppModule, level: AccessLevel = 'VIEW') =>
    RANK[access.permissions[module] ?? 'NONE'] >= RANK[level];
  return (
    <OrgContext.Provider
      value={{ orgId: access.organization.id, access, can, centerLabel: access.organization.centerLabel || 'Center' }}
    >
      {children}
    </OrgContext.Provider>
  );
}

export function useOrg() {
  const ctx = useContext(OrgContext);
  if (!ctx) throw new Error('useOrg outside OrgProvider');
  return ctx;
}

export function useOrgIdParam() {
  return useParams<{ orgId: string }>().orgId;
}
