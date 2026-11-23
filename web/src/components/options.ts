import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import type { Option, Page, RoleSummary } from '../api/types';

export function useCenterOptions(orgId: string) {
  return useQuery({ queryKey: ['center-options', orgId], queryFn: () => api.get<Option[]>(`/orgs/${orgId}/centers/options`) });
}

export function useBatchOptions(orgId: string, centerId: string | undefined) {
  return useQuery({
    queryKey: ['batch-options', orgId, centerId],
    queryFn: () => api.get<Option[]>(`/orgs/${orgId}/centers/${centerId}/batches/options`),
    enabled: !!centerId,
  });
}

export function useAssignableRoles(orgId: string) {
  return useQuery({ queryKey: ['roles-assignable', orgId], queryFn: () => api.get<Page<RoleSummary>>(`/orgs/${orgId}/roles/assignable`), select: (p) => p.items });
}

export const COUNTRY_CODES = ['+91', '+971', '+966', '+974', '+965', '+968', '+973', '+44', '+1', '+61', '+65'];
export const BLOOD_GROUPS = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
