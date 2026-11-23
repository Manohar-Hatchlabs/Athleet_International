import { useState } from 'react';
import {
  Alert, Avatar, Box, Button, Chip, IconButton, Menu, MenuItem, Table, TableBody, TableCell, TableHead,
  TablePagination, TableRow,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage } from '../api/client';
import type { Center, CenterSummary, Page } from '../api/types';
import { useOrg } from '../org/OrgContext';
import { ConfirmDialog, EmptyState, PageHeader, Panel, SearchBox, useDebounced } from '../components/common';
import { BatchDialog, BatchesDialog, ClassDialog } from './ClassDialogs';
import { Loading } from '../App';

export function ClassesPage() {
  const { orgId, can, centerLabel, access } = useOrg();
  const qc = useQueryClient();
  const manage = can('CENTER', 'MANAGE');
  const [search, setSearch] = useState('');
  const q = useDebounced(search);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [menu, setMenu] = useState<{ el: HTMLElement; row: CenterSummary } | null>(null);
  const [dialog, setDialog] = useState<{ open: boolean; center?: Center }>({ open: false });
  const [addBatchFor, setAddBatchFor] = useState<string | null>(null);
  const [batchesFor, setBatchesFor] = useState<CenterSummary | null>(null);
  const [del, setDel] = useState<CenterSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const list = useQuery({
    queryKey: ['centers', orgId, q, page, size],
    queryFn: () => api.get<Page<CenterSummary>>(`/orgs/${orgId}/centers`, { search: q, page, size }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (c: CenterSummary) => api.del(`/orgs/${orgId}/centers/${c.id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['centers', orgId] });
      qc.invalidateQueries({ queryKey: ['center-options', orgId] });
      setDel(null);
    },
    onError: (e) => { setError(errorMessage(e)); setDel(null); },
  });

  async function act(action: string, row: CenterSummary) {
    setMenu(null);
    if (action === 'edit') {
      try { setDialog({ open: true, center: await api.get<Center>(`/orgs/${orgId}/centers/${row.id}`) }); }
      catch (e) { setError(errorMessage(e)); }
    }
    if (action === 'add-batch') setAddBatchFor(row.id);
    if (action === 'batches') setBatchesFor(row);
    if (action === 'delete') setDel(row);
  }

  return (
    <>
      <PageHeader title={centerLabel} actions={<>
        <SearchBox value={search} onChange={(v) => { setSearch(v); setPage(0); }} />
        {manage && !access.centerId && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialog({ open: true })}>Add {centerLabel}</Button>
        )}
      </>} />
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      <Panel>
        {list.isLoading ? <Loading /> : !list.data?.items.length ? <EmptyState text={`No ${centerLabel.toLowerCase()} found.`} /> : (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead><TableRow>
                <TableCell /><TableCell>{centerLabel} name</TableCell><TableCell>Status</TableCell><TableCell>Location</TableCell>
                <TableCell align="right">Batches</TableCell><TableCell align="right">Student</TableCell>
                <TableCell align="right">Capacity</TableCell><TableCell align="right">Action</TableCell>
              </TableRow></TableHead>
              <TableBody>
                {list.data.items.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell><Avatar variant="rounded" sx={{ width: 32, height: 32, fontSize: 14 }}>{c.name[0]}</Avatar></TableCell>
                    <TableCell>{c.name}</TableCell>
                    <TableCell><Chip size="small" label={c.status === 'ACTIVE' ? 'Active' : 'Inactive'} color={c.status === 'ACTIVE' ? 'success' : 'default'} variant="outlined" /></TableCell>
                    <TableCell>{[c.city, c.country].filter(Boolean).join(', ') || '—'}</TableCell>
                    <TableCell align="right">{c.batches}</TableCell>
                    <TableCell align="right">{c.students}</TableCell>
                    <TableCell align="right">{c.capacity}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" aria-label="Actions" onClick={(e) => setMenu({ el: e.currentTarget, row: c })}><MoreVertIcon fontSize="small" /></IconButton>
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
        <MenuItem onClick={() => menu && act('batches', menu.row)}>View Batches</MenuItem>
        {manage && <MenuItem onClick={() => menu && act('add-batch', menu.row)}>Add Batches</MenuItem>}
        {manage && <MenuItem onClick={() => menu && act('edit', menu.row)}>Edit {centerLabel}</MenuItem>}
        {manage && !access.centerId && <MenuItem onClick={() => menu && act('delete', menu.row)} sx={{ color: 'error.main' }}>Delete {centerLabel}</MenuItem>}
      </Menu>

      <ClassDialog open={dialog.open} center={dialog.center} onClose={() => setDialog({ open: false })} />
      {addBatchFor && <BatchDialog open centerId={addBatchFor} onClose={() => setAddBatchFor(null)} />}
      <BatchesDialog center={batchesFor} onClose={() => setBatchesFor(null)} />
      <ConfirmDialog open={!!del} title={`Delete ${centerLabel}`} danger confirmLabel="Delete"
        text={`Delete "${del?.name}"? This cannot be undone.`} onClose={() => setDel(null)} onConfirm={() => del && remove.mutateAsync(del)} />
    </>
  );
}
