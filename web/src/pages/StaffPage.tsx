import { useEffect, useState } from 'react';
import {
  Alert, Avatar, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Grid2 as Grid, IconButton, Menu,
  MenuItem, Stack, Tab, Table, TableBody, TableCell, TableHead, TablePagination, TableRow, Tabs, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage, fieldErrors } from '../api/client';
import type { Page, Staff } from '../api/types';
import { useOrg } from '../org/OrgContext';
import { ConfirmDialog, EmptyState, PageHeader, Panel, SearchBox, clean, useDebounced, useForm } from '../components/common';
import { COUNTRY_CODES, useAssignableRoles, useCenterOptions } from '../components/options';
import { roleTypeLabel } from './RolesPage';
import { Loading } from '../App';

const EMPTY = {
  roleType: 'HEAD_OFFICE', roleId: '', centerId: '', email: '', countryCode: '+91', mobile: '', firstName: '', lastName: '',
  dateOfJoining: '', qualifications: '', profile: '',
};

function StaffDialog({ open, staff, onClose }: { open: boolean; staff?: Staff; onClose: () => void }) {
  const { orgId, centerLabel, access } = useOrg();
  const qc = useQueryClient();
  const f = useForm({ ...EMPTY });
  const [error, setError] = useState<string | null>(null);
  const roles = useAssignableRoles(orgId);
  const centers = useCenterOptions(orgId);

  useEffect(() => {
    if (!open) return;
    setError(null);
    f.setErrors({});
    if (staff) {
      f.setValues({
        roleType: staff.roleType, roleId: staff.roleId, centerId: staff.centerId ?? '', email: staff.email ?? '',
        countryCode: staff.countryCode ?? '+91', mobile: staff.mobile ?? '', firstName: staff.firstName,
        lastName: staff.lastName ?? '', dateOfJoining: staff.dateOfJoining ?? '', qualifications: staff.qualifications ?? '',
        profile: staff.profile ?? '',
      });
    } else f.setValues({ ...EMPTY, roleType: access.centerId ? 'CENTER' : 'HEAD_OFFICE', centerId: access.centerId ?? '' });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, staff]);

  const typeRoles = (roles.data ?? []).filter((r) => r.roleType === f.values.roleType);
  const save = useMutation({
    mutationFn: () => {
      const { roleType, ...rest } = f.values;
      const body = clean({ ...rest, centerId: roleType === 'CENTER' ? rest.centerId : '' });
      return staff ? api.put(`/orgs/${orgId}/staff/${staff.membershipId}`, body) : api.post(`/orgs/${orgId}/staff`, body);
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['staff', orgId] }); onClose(); },
    onError: (e) => { f.setErrors(fieldErrors(e)); setError(errorMessage(e)); },
  });
  const valid = f.values.roleId && f.values.email.trim() && f.values.firstName.trim() && (f.values.roleType !== 'CENTER' || f.values.centerId);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{staff ? 'Edit Staff' : 'Add Staff'}</DialogTitle>
      <DialogContent dividers>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {!staff && <Alert severity="info" sx={{ mb: 2 }}>The staff member gets an email invite to set their own password.</Alert>}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField select label="Role Type" required value={f.values.roleType} disabled={!!access.centerId}
              onChange={(e) => f.setValues((s) => ({ ...s, roleType: e.target.value, roleId: '' }))}>
              <MenuItem value="HEAD_OFFICE">Head Office</MenuItem>
              <MenuItem value="CENTER">{centerLabel}</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField select label="Role" required {...f.field('roleId')}>
              {typeRoles.map((r) => <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>)}
            </TextField>
          </Grid>
          {f.values.roleType === 'CENTER' && (
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField select label={centerLabel} required {...f.field('centerId')} disabled={!!access.centerId}>
                {(centers.data ?? []).map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
              </TextField>
            </Grid>
          )}
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Email Address" type="email" required {...f.field('email')} inputProps={{ maxLength: 254 }} /></Grid>
          <Grid size={{ xs: 4, sm: 2 }}>
            <TextField select label="Code" {...f.field('countryCode')}>{COUNTRY_CODES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}</TextField>
          </Grid>
          <Grid size={{ xs: 8, sm: 4 }}><TextField label="Mobile" {...f.field('mobile')} inputProps={{ inputMode: 'numeric', maxLength: 15 }} /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="First Name" required {...f.field('firstName')} inputProps={{ maxLength: 60 }} /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Last Name" {...f.field('lastName')} inputProps={{ maxLength: 60 }} /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Date Of Joining" type="date" InputLabelProps={{ shrink: true }} {...f.field('dateOfJoining')}
              inputProps={{ max: new Date().toISOString().slice(0, 10) }} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Qualifications" {...f.field('qualifications')} inputProps={{ maxLength: 300 }} /></Grid>
          <Grid size={12}><TextField label="Profile" multiline minRows={3} {...f.field('profile')} inputProps={{ maxLength: 1000 }} /></Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!valid || save.isPending} onClick={() => save.mutate()}>Save</Button>
      </DialogActions>
    </Dialog>
  );
}

export function StaffPage() {
  const { orgId, can, centerLabel } = useOrg();
  const qc = useQueryClient();
  const manage = can('STAFF', 'MANAGE');
  const [status, setStatus] = useState<'ACTIVE' | 'DEREGISTERED'>('ACTIVE');
  const [search, setSearch] = useState('');
  const q = useDebounced(search);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [dialog, setDialog] = useState<{ open: boolean; staff?: Staff }>({ open: false });
  const [menu, setMenu] = useState<{ el: HTMLElement; row: Staff } | null>(null);
  const [dereg, setDereg] = useState<Staff | null>(null);
  const [error, setError] = useState<string | null>(null);
  const list = useQuery({
    queryKey: ['staff', orgId, status, q, page, size],
    queryFn: () => api.get<Page<Staff>>(`/orgs/${orgId}/staff`, { status, search: q, page, size }),
    placeholderData: keepPreviousData,
  });
  const deregister = useMutation({
    mutationFn: (s: Staff) => api.post(`/orgs/${orgId}/staff/${s.membershipId}/deregister`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['staff', orgId] }); setDereg(null); },
    onError: (e) => { setError(errorMessage(e)); setDereg(null); },
  });

  return (
    <>
      <PageHeader title="Staff" actions={<>
        <SearchBox value={search} onChange={(v) => { setSearch(v); setPage(0); }} />
        {manage && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ open: true })}>Add Staff</Button>}
      </>} />
      <Tabs value={status} onChange={(_, v) => { setStatus(v); setPage(0); }} sx={{ mb: 2 }}>
        <Tab value="ACTIVE" label="Active" /><Tab value="DEREGISTERED" label="Deregistered" />
      </Tabs>
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      <Panel>
        {list.isLoading ? <Loading /> : !list.data?.items.length ? <EmptyState text="No staff found." /> : (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead><TableRow><TableCell>Staff Member</TableCell><TableCell>Role</TableCell><TableCell>Contact</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead>
              <TableBody>
                {list.data.items.map((s) => (
                  <TableRow key={s.membershipId} hover>
                    <TableCell>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <Avatar sx={{ width: 32, height: 32, fontSize: 14 }}>{s.firstName[0]}</Avatar>
                        <Box>
                          <Typography>{s.firstName} {s.lastName ?? ''}</Typography>
                          {!s.passwordSet && <Chip size="small" label="Invite pending" variant="outlined" />}
                        </Box>
                      </Stack>
                    </TableCell>
                    <TableCell>{s.roleName} ({roleTypeLabel(s.roleType, centerLabel)})</TableCell>
                    <TableCell sx={{ wordBreak: 'break-all' }}>{s.email}{s.mobile ? ` · ${s.countryCode ?? ''} ${s.mobile}` : ''}</TableCell>
                    <TableCell align="right">
                      {manage && s.status === 'ACTIVE' && (
                        <IconButton size="small" aria-label="Actions" onClick={(e) => setMenu({ el: e.currentTarget, row: s })}><MoreVertIcon fontSize="small" /></IconButton>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        )}
        <TablePagination component="div" count={list.data?.total ?? 0} page={page} rowsPerPage={size} rowsPerPageOptions={[5, 10, 20]}
          onPageChange={(_, p) => setPage(p)} onRowsPerPageChange={(e) => { setSize(parseInt(e.target.value, 10)); setPage(0); }} />
      </Panel>
      <Menu anchorEl={menu?.el} open={!!menu} onClose={() => setMenu(null)}>
        <MenuItem onClick={() => { if (menu) setDialog({ open: true, staff: menu.row }); setMenu(null); }}>Edit</MenuItem>
        <MenuItem sx={{ color: 'error.main' }} onClick={() => { if (menu) setDereg(menu.row); setMenu(null); }}>Deregister</MenuItem>
      </Menu>
      <StaffDialog open={dialog.open} staff={dialog.staff} onClose={() => setDialog({ open: false })} />
      <ConfirmDialog open={!!dereg} title="Deregister staff" danger confirmLabel="Deregister"
        text={`${dereg?.firstName} will lose access to this academy and be signed out.`}
        onClose={() => setDereg(null)} onConfirm={() => dereg && deregister.mutateAsync(dereg)} />
    </>
  );
}

