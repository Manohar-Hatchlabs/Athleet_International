import { useEffect, useState } from 'react';
import {
  Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Grid2 as Grid,
  IconButton, Radio, RadioGroup, Table, TableBody, TableCell, TableHead, TablePagination, TableRow, TextField,
  ToggleButton, ToggleButtonGroup, Tooltip, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage, fieldErrors } from '../api/client';
import { MODULES, type AccessLevel, type AppModule, type Page, type Role, type RoleSummary, type RoleType } from '../api/types';
import { useOrg } from '../org/OrgContext';
import { ConfirmDialog, EmptyState, PageHeader, Panel, SearchBox, useDebounced } from '../components/common';
import { Loading } from '../App';

export function roleTypeLabel(t: RoleType, centerLabel: string) {
  return t === 'HEAD_OFFICE' ? 'Head Office' : t === 'CENTER' ? centerLabel : 'Student';
}

function RoleDialog({ open, role, onClose }: { open: boolean; role?: Role; onClose: () => void }) {
  const { orgId, centerLabel, access } = useOrg();
  const qc = useQueryClient();
  const [name, setName] = useState('');
  const [roleType, setRoleType] = useState<RoleType>('HEAD_OFFICE');
  const [perms, setPerms] = useState<Partial<Record<AppModule, AccessLevel>>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const rank = { NONE: 0, VIEW: 1, MANAGE: 2 } as const;

  useEffect(() => {
    if (!open) return;
    setError(null);
    setErrors({});
    setName(role?.name ?? '');
    setRoleType(role?.roleType ?? 'HEAD_OFFICE');
    setPerms(role?.permissions ?? { DASHBOARD: 'VIEW' });
  }, [open, role]);

  const save = useMutation({
    mutationFn: () => {
      const body = { name: name.trim(), roleType, permissions: perms };
      return role ? api.put(`/orgs/${orgId}/roles/${role.id}`, body) : api.post(`/orgs/${orgId}/roles`, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['roles', orgId] });
      qc.invalidateQueries({ queryKey: ['roles-assignable', orgId] });
      onClose();
    },
    onError: (e) => { setErrors(fieldErrors(e)); setError(errorMessage(e)); },
  });

  const locked = role?.systemRole;
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{role ? 'Edit Role' : 'Add Role'}</DialogTitle>
      <DialogContent dividers>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {locked && <Alert severity="info" sx={{ mb: 2 }}>System roles keep their name and type; only permissions can change.</Alert>}
        <Typography variant="body2" color="text.secondary">Role Type</Typography>
        <RadioGroup row value={roleType} onChange={(e) => setRoleType(e.target.value as RoleType)} sx={{ mb: 1 }}>
          <FormControlLabel value="HEAD_OFFICE" control={<Radio size="small" />} disabled={!!role}
            label={<Box><Typography>Head Office</Typography><Typography variant="caption" color="text.secondary">Administrative roles</Typography></Box>} />
          <FormControlLabel value="CENTER" control={<Radio size="small" />} disabled={!!role}
            label={<Box><Typography>{centerLabel}</Typography><Typography variant="caption" color="text.secondary">Premise coaching roles</Typography></Box>} />
        </RadioGroup>
        <TextField label="Role Name" required value={name} disabled={locked} error={!!errors.name} helperText={errors.name}
          onChange={(e) => setName(e.target.value)} inputProps={{ maxLength: 60 }} sx={{ mb: 2 }} />
        <Typography fontWeight={600} gutterBottom>Permissions</Typography>
        <Grid container spacing={1}>
          {MODULES.map((m) => {
            const mine = access.permissions[m.key] ?? 'NONE';
            return (
              <Grid key={m.key} size={12} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Typography>{m.key === 'CENTER' ? centerLabel : m.label}</Typography>
                <ToggleButtonGroup size="small" exclusive value={perms[m.key] ?? 'NONE'}
                  onChange={(_, v: AccessLevel | null) => v && setPerms({ ...perms, [m.key]: v })}>
                  {(['NONE', 'VIEW', 'MANAGE'] as AccessLevel[]).map((l) => (
                    // You can't grant more than you have (the server enforces this too).
                    <ToggleButton key={l} value={l} disabled={rank[l] > rank[mine]} sx={{ px: 1.5 }}>
                      {l === 'NONE' ? 'None' : l === 'VIEW' ? 'View' : 'Manage'}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
              </Grid>
            );
          })}
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!name.trim() || save.isPending} onClick={() => save.mutate()}>Save</Button>
      </DialogActions>
    </Dialog>
  );
}

export function RolesPage() {
  const { orgId, can, centerLabel } = useOrg();
  const qc = useQueryClient();
  const manage = can('ROLE', 'MANAGE');
  const [search, setSearch] = useState('');
  const q = useDebounced(search);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [dialog, setDialog] = useState<{ open: boolean; role?: Role }>({ open: false });
  const [del, setDel] = useState<RoleSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const list = useQuery({
    queryKey: ['roles', orgId, q, page, size],
    queryFn: () => api.get<Page<RoleSummary>>(`/orgs/${orgId}/roles`, { search: q, page, size }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (r: RoleSummary) => api.del(`/orgs/${orgId}/roles/${r.id}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['roles', orgId] }); setDel(null); },
    onError: (e) => { setError(errorMessage(e)); setDel(null); },
  });
  const edit = async (r: RoleSummary) => {
    try { setDialog({ open: true, role: await api.get<Role>(`/orgs/${orgId}/roles/${r.id}`) }); }
    catch (e) { setError(errorMessage(e)); }
  };

  return (
    <>
      <PageHeader title="Roles" actions={<>
        <SearchBox value={search} onChange={(v) => { setSearch(v); setPage(0); }} />
        {manage && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ open: true })}>Add Role</Button>}
      </>} />
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      <Panel>
        {list.isLoading ? <Loading /> : !list.data?.items.length ? <EmptyState text="No roles found." /> : (
          <Table size="small">
            <TableHead><TableRow><TableCell>Role Name</TableCell><TableCell>Role Type</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
            <TableBody>
              {list.data.items.map((r) => (
                <TableRow key={r.id} hover>
                  <TableCell>{r.name} {r.systemRole && <Chip size="small" label="System" sx={{ ml: 1 }} />}</TableCell>
                  <TableCell>{roleTypeLabel(r.roleType, centerLabel)}</TableCell>
                  <TableCell align="right">
                    {manage && r.roleType !== 'STUDENT' && (
                      <Tooltip title="Edit"><IconButton size="small" onClick={() => edit(r)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                    )}
                    {manage && !r.systemRole && (
                      <Tooltip title="Delete"><IconButton size="small" onClick={() => setDel(r)}><DeleteIcon fontSize="small" /></IconButton></Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <TablePagination component="div" count={list.data?.total ?? 0} page={page} rowsPerPage={size} rowsPerPageOptions={[5, 10, 20]}
          onPageChange={(_, p) => setPage(p)} onRowsPerPageChange={(e) => { setSize(parseInt(e.target.value, 10)); setPage(0); }} />
      </Panel>
      <RoleDialog open={dialog.open} role={dialog.role} onClose={() => setDialog({ open: false })} />
      <ConfirmDialog open={!!del} title="Delete role" danger confirmLabel="Delete" text={`Delete the role "${del?.name}"?`}
        onClose={() => setDel(null)} onConfirm={() => del && remove.mutateAsync(del)} />
    </>
  );
}
