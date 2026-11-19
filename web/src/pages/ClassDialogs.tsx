import { useEffect, useState } from 'react';
import {
  Alert, Box, Button, Checkbox, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, Grid2 as Grid,
  IconButton, InputLabel, Link, ListItemText, MenuItem, OutlinedInput, Select, Stack, Table, TableBody, TableCell,
  TableHead, TableRow, TextField, ToggleButton, ToggleButtonGroup, Typography,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage, fieldErrors } from '../api/client';
import type { Batch, Center, DayOfWeek, Page, Staff } from '../api/types';
import { useOrg } from '../org/OrgContext';
import { ConfirmDialog, clean, useForm } from '../components/common';
import { COUNTRY_CODES } from '../components/options';

const CENTER_EMPTY = {
  name: '', code: '', contactName: '', contactCountryCode: '+91', contactPhone: '', contactEmail: '', sport: '',
  facilityType: '', facilityCount: '', status: 'ACTIVE', country: '', state: '', city: '', address1: '', address2: '',
  zipcode: '', latitude: '', longitude: '',
};

export function ClassDialog({ open, center, onClose }: { open: boolean; center?: Center; onClose: () => void }) {
  const { orgId, centerLabel } = useOrg();
  const qc = useQueryClient();
  const f = useForm({ ...CENTER_EMPTY });
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    f.setErrors({});
    const v: Record<string, string> = { ...CENTER_EMPTY };
    if (center) for (const k of Object.keys(CENTER_EMPTY)) {
      const val = (center as unknown as Record<string, unknown>)[k];
      if (val !== undefined && val !== null) v[k] = String(val);
    }
    f.setValues(v as typeof CENTER_EMPTY);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, center]);

  const num = (s: string) => (s.trim() === '' ? undefined : Number(s));
  const save = useMutation({
    mutationFn: () => {
      const v = clean(f.values);
      const body = { ...v, facilityCount: num(f.values.facilityCount), latitude: num(f.values.latitude), longitude: num(f.values.longitude) };
      return center ? api.put(`/orgs/${orgId}/centers/${center.id}`, body) : api.post(`/orgs/${orgId}/centers`, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['centers', orgId] });
      qc.invalidateQueries({ queryKey: ['center-options', orgId] });
      onClose();
    },
    onError: (e) => { f.setErrors(fieldErrors(e)); setError(errorMessage(e)); },
  });

  const mapUrl = f.values.latitude && f.values.longitude
    ? `https://www.google.com/maps?q=${encodeURIComponent(f.values.latitude)},${encodeURIComponent(f.values.longitude)}` : null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{center ? `Edit ${centerLabel}` : `Add ${centerLabel}`}</DialogTitle>
      <DialogContent dividers>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label={`${centerLabel} Name`} required {...f.field('name')} inputProps={{ maxLength: 100 }} /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Point Contact Name" {...f.field('contactName')} /></Grid>
          <Grid size={{ xs: 4, sm: 2 }}>
            <TextField select label="Code" {...f.field('contactCountryCode')}>
              {COUNTRY_CODES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid size={{ xs: 8, sm: 4 }}><TextField label="Point Contact Number" {...f.field('contactPhone')} inputProps={{ inputMode: 'numeric', maxLength: 15 }} /></Grid>
          <Grid size={{ xs: 12, sm: 6 }}><TextField label="Point Contact Email" type="email" {...f.field('contactEmail')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="Sport" {...f.field('sport')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="Court/Ground/Pitch Type" {...f.field('facilityType')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="Number of Courts/Grounds/Pitches" type="number" {...f.field('facilityCount')} inputProps={{ min: 0, max: 1000 }} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField select label="Status" required {...f.field('status')}>
              <MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem>
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="Country" {...f.field('country')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="State" {...f.field('state')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="City" {...f.field('city')} /></Grid>
          <Grid size={{ xs: 12, sm: 8 }}><TextField label="Address 1" {...f.field('address1')} /></Grid>
          <Grid size={{ xs: 12, sm: 8 }}><TextField label="Address 2" {...f.field('address2')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }}><TextField label="Zipcode" {...f.field('zipcode')} /></Grid>
          <Grid size={{ xs: 6, sm: 4 }}><TextField label="Latitude" {...f.field('latitude')} /></Grid>
          <Grid size={{ xs: 6, sm: 4 }}><TextField label="Longitude" {...f.field('longitude')} /></Grid>
          <Grid size={{ xs: 12, sm: 4 }} sx={{ display: 'flex', alignItems: 'center' }}>
            {mapUrl ? <Link href={mapUrl} target="_blank" rel="noopener noreferrer">View In Map</Link>
              : <Typography variant="body2" color="text.secondary">Enter latitude/longitude to view in map</Typography>}
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!f.values.name.trim() || save.isPending} onClick={() => save.mutate()}>Save</Button>
      </DialogActions>
    </Dialog>
  );
}

const DAYS: { key: DayOfWeek; short: string }[] = [
  { key: 'MONDAY', short: 'M' }, { key: 'TUESDAY', short: 'T' }, { key: 'WEDNESDAY', short: 'W' },
  { key: 'THURSDAY', short: 'T' }, { key: 'FRIDAY', short: 'F' }, { key: 'SATURDAY', short: 'S' }, { key: 'SUNDAY', short: 'S' },
];
const titleCase = (d: string) => d[0] + d.slice(1).toLowerCase();

interface BatchForm {
  name: string; startDate: string; endDate: string; capacity: string; status: string; description: string;
  coachUserIds: string[]; times: Partial<Record<DayOfWeek, { start: string; end: string }>>;
}

const BATCH_EMPTY: BatchForm = { name: '', startDate: '', endDate: '', capacity: '', status: 'ACTIVE', description: '', coachUserIds: [], times: {} };

export function BatchDialog({ open, centerId, batch, onClose }: { open: boolean; centerId: string; batch?: Batch; onClose: () => void }) {
  const { orgId } = useOrg();
  const qc = useQueryClient();
  const [v, setV] = useState<BatchForm>(BATCH_EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const staff = useQuery({
    queryKey: ['staff', orgId, 'coaches'],
    queryFn: () => api.get<Page<Staff>>(`/orgs/${orgId}/staff`, { size: 100 }),
    enabled: open,
  });

  useEffect(() => {
    if (!open) return;
    setError(null);
    setErrors({});
    if (batch) {
      const times: BatchForm['times'] = {};
      batch.slots.forEach((s) => (times[s.dayOfWeek] = { start: s.startTime.slice(0, 5), end: s.endTime.slice(0, 5) }));
      setV({ name: batch.name, startDate: batch.startDate ?? '', endDate: batch.endDate ?? '', capacity: String(batch.capacity),
        status: batch.status, description: batch.description ?? '', coachUserIds: batch.coaches.map((c) => c.userId), times });
    } else setV(BATCH_EMPTY);
  }, [open, batch]);

  const days = DAYS.filter((d) => v.times[d.key]).map((d) => d.key);
  const toggleDays = (_: unknown, next: DayOfWeek[]) => {
    const times: BatchForm['times'] = {};
    next.forEach((d) => (times[d] = v.times[d] ?? { start: '16:00', end: '17:00' }));
    setV({ ...v, times });
  };

  const save = useMutation({
    mutationFn: () => {
      const body = {
        name: v.name.trim(), startDate: v.startDate || undefined, endDate: v.endDate || undefined,
        capacity: Number(v.capacity), status: v.status, description: v.description.trim() || undefined,
        coachUserIds: v.coachUserIds,
        slots: days.map((d) => ({ dayOfWeek: d, startTime: v.times[d]!.start, endTime: v.times[d]!.end })),
      };
      return batch ? api.put(`/orgs/${orgId}/batches/${batch.id}`, body) : api.post(`/orgs/${orgId}/centers/${centerId}/batches`, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['batches', orgId, centerId] });
      qc.invalidateQueries({ queryKey: ['batch-options', orgId, centerId] });
      qc.invalidateQueries({ queryKey: ['centers', orgId] });
      onClose();
    },
    onError: (e) => { setErrors(fieldErrors(e)); setError(errorMessage(e)); },
  });

  const coaches = (staff.data?.items ?? []).filter((s) => s.status === 'ACTIVE');
  const valid = v.name.trim() && Number(v.capacity) > 0;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{batch ? 'Edit Batch' : 'Add Batch'}</DialogTitle>
      <DialogContent dividers>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Grid container spacing={2}>
          <Grid size={12}><TextField label="Batch Name" required value={v.name} error={!!errors.name} helperText={errors.name}
            onChange={(e) => setV({ ...v, name: e.target.value })} inputProps={{ maxLength: 100 }} /></Grid>
          <Grid size={6}><TextField label="Start Date" type="date" InputLabelProps={{ shrink: true }} value={v.startDate}
            onChange={(e) => setV({ ...v, startDate: e.target.value })} /></Grid>
          <Grid size={6}><TextField label="End Date" type="date" InputLabelProps={{ shrink: true }} value={v.endDate}
            error={!!errors.dateRangeValid} helperText={errors.dateRangeValid}
            onChange={(e) => setV({ ...v, endDate: e.target.value })} /></Grid>
          <Grid size={12}>
            <Typography variant="body2" color="text.secondary" gutterBottom>Training Days</Typography>
            <ToggleButtonGroup value={days} onChange={toggleDays} size="small" color="primary">
              {DAYS.map((d) => <ToggleButton key={d.key} value={d.key} aria-label={titleCase(d.key)} sx={{ width: 40 }}>{d.short}</ToggleButton>)}
            </ToggleButtonGroup>
          </Grid>
          {days.length > 0 && (
            <Grid size={12}>
              <Typography variant="body2" color="text.secondary" gutterBottom>Timings</Typography>
              <Stack spacing={1}>
                {days.map((d) => (
                  <Stack key={d} direction="row" spacing={1} alignItems="center">
                    <Typography sx={{ width: 100 }}>{titleCase(d)}</Typography>
                    <TextField type="time" label="From" InputLabelProps={{ shrink: true }} value={v.times[d]!.start}
                      onChange={(e) => setV({ ...v, times: { ...v.times, [d]: { ...v.times[d]!, start: e.target.value } } })} />
                    <Typography>to</Typography>
                    <TextField type="time" label="To" InputLabelProps={{ shrink: true }} value={v.times[d]!.end}
                      onChange={(e) => setV({ ...v, times: { ...v.times, [d]: { ...v.times[d]!, end: e.target.value } } })} />
                  </Stack>
                ))}
              </Stack>
            </Grid>
          )}
          <Grid size={12}>
            <FormControl>
              <InputLabel>Coach(s)</InputLabel>
              <Select multiple value={v.coachUserIds} input={<OutlinedInput label="Coach(s)" />}
                onChange={(e) => setV({ ...v, coachUserIds: typeof e.target.value === 'string' ? e.target.value.split(',') : e.target.value })}
                renderValue={(ids) => (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {ids.map((id) => {
                      const c = coaches.find((s) => s.userId === id);
                      return <Chip key={id} size="small" label={c ? `${c.firstName} ${c.lastName ?? ''}` : '…'} />;
                    })}
                  </Box>
                )}>
                {coaches.map((s) => (
                  <MenuItem key={s.userId} value={s.userId}>
                    <Checkbox size="small" checked={v.coachUserIds.includes(s.userId)} />
                    <ListItemText primary={`${s.firstName} ${s.lastName ?? ''}`} secondary={s.roleName} />
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid size={6}><TextField label="Batch Capacity" required type="number" value={v.capacity} error={!!errors.capacity}
            helperText={errors.capacity} onChange={(e) => setV({ ...v, capacity: e.target.value })} inputProps={{ min: 1, max: 10000 }} /></Grid>
          <Grid size={6}>
            <TextField select label="Status" value={v.status} onChange={(e) => setV({ ...v, status: e.target.value })}>
              <MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem>
            </TextField>
          </Grid>
          <Grid size={12}><TextField label="Batch Description" multiline minRows={2} value={v.description}
            onChange={(e) => setV({ ...v, description: e.target.value })} inputProps={{ maxLength: 500 }} /></Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!valid || save.isPending} onClick={() => save.mutate()}>Save</Button>
      </DialogActions>
    </Dialog>
  );
}

export function BatchesDialog({ center, onClose }: { center: { id: string; name: string } | null; onClose: () => void }) {
  const { orgId, can } = useOrg();
  const qc = useQueryClient();
  const manage = can('CENTER', 'MANAGE');
  const [edit, setEdit] = useState<{ open: boolean; batch?: Batch }>({ open: false });
  const [del, setDel] = useState<Batch | null>(null);
  const [error, setError] = useState<string | null>(null);
  const list = useQuery({
    queryKey: ['batches', orgId, center?.id],
    queryFn: () => api.get<Batch[]>(`/orgs/${orgId}/centers/${center!.id}/batches`),
    enabled: !!center,
  });
  const remove = useMutation({
    mutationFn: (b: Batch) => api.del(`/orgs/${orgId}/batches/${b.id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['batches', orgId, center?.id] });
      qc.invalidateQueries({ queryKey: ['centers', orgId] });
      setDel(null);
    },
    onError: (e) => { setError(errorMessage(e)); setDel(null); },
  });

  return (
    <Dialog open={!!center} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Batches · {center?.name}</DialogTitle>
      <DialogContent dividers>
        {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small">
            <TableHead><TableRow>
              <TableCell>Batch Name</TableCell><TableCell>Training Days</TableCell><TableCell>Coach(s)</TableCell>
              <TableCell align="right">Student</TableCell><TableCell align="right">Capacity</TableCell><TableCell>Status</TableCell><TableCell />
            </TableRow></TableHead>
            <TableBody>
              {(list.data ?? []).map((b) => (
                <TableRow key={b.id}>
                  <TableCell>{b.name}</TableCell>
                  <TableCell>{b.slots.map((s) => `${titleCase(s.dayOfWeek).slice(0, 3)} ${s.startTime.slice(0, 5)}–${s.endTime.slice(0, 5)}`).join(', ') || '—'}</TableCell>
                  <TableCell>{b.coaches.map((c) => c.name).join(', ') || '—'}</TableCell>
                  <TableCell align="right">{b.students}</TableCell>
                  <TableCell align="right">{b.capacity}</TableCell>
                  <TableCell>{titleCase(b.status)}</TableCell>
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    {manage && <>
                      <IconButton size="small" aria-label="Edit batch" onClick={() => setEdit({ open: true, batch: b })}><EditIcon fontSize="small" /></IconButton>
                      <IconButton size="small" aria-label="Delete batch" onClick={() => setDel(b)}><DeleteIcon fontSize="small" /></IconButton>
                    </>}
                  </TableCell>
                </TableRow>
              ))}
              {list.data?.length === 0 && <TableRow><TableCell colSpan={7} align="center">No batches yet.</TableCell></TableRow>}
            </TableBody>
          </Table>
        </Box>
      </DialogContent>
      <DialogActions>
        {manage && <Button onClick={() => setEdit({ open: true })}>Add Batch</Button>}
        <Button variant="contained" onClick={onClose}>Close</Button>
      </DialogActions>
      {center && <BatchDialog open={edit.open} centerId={center.id} batch={edit.batch} onClose={() => setEdit({ open: false })} />}
      <ConfirmDialog open={!!del} title="Delete batch" danger confirmLabel="Delete"
        text={`Delete "${del?.name}"? This cannot be undone.`} onClose={() => setDel(null)} onConfirm={() => del && remove.mutateAsync(del)} />
    </Dialog>
  );
}
