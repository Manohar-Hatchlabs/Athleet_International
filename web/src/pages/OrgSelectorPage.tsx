import { useMemo, useState } from 'react';
import {
  AppBar, Autocomplete, Avatar, Box, Button, Card, CardActionArea, CardContent, Container, Dialog, DialogContent,
  DialogTitle, Grid2 as Grid, IconButton, InputAdornment, List, ListItemButton, ListItemText, Stack, TextField,
  Toolbar, Tooltip, Typography,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import SearchIcon from '@mui/icons-material/Search';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { AccessibleOrganization } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Loading } from '../App';

function initials(name: string) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]!.toUpperCase()).join('');
}

function OrgCard({ org, onClick }: { org: AccessibleOrganization; onClick: () => void }) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardActionArea onClick={onClick} sx={{ height: '100%' }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center">
            <Avatar sx={{ bgcolor: 'primary.main' }}>{initials(org.name)}</Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography fontWeight={600} noWrap title={org.name}>{org.name}</Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {[org.city, org.country].filter(Boolean).join(', ') || '—'}
              </Typography>
            </Box>
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}

/** Acadware flow: "Select Academy" dropdown → GO, then primary card + affiliated academies grid. */
export function OrgSelectorPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['organizations'],
    queryFn: () => api.get<AccessibleOrganization[]>('/organizations'),
  });
  const [picked, setPicked] = useState<AccessibleOrganization | null>(null);
  const [selected, setSelected] = useState<AccessibleOrganization | null>(null);
  const [search, setSearch] = useState('');
  const [modeFor, setModeFor] = useState<AccessibleOrganization | null>(null);

  const affiliates = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (selected?.affiliates ?? []).filter((a) => !q || a.name.toLowerCase().includes(q) || a.city?.toLowerCase().includes(q));
  }, [selected, search]);

  const open = (org: AccessibleOrganization, view: 'primary' | 'academy' = 'academy') => {
    sessionStorage.setItem('athleet.view', view);
    navigate(`/org/${org.id}/dashboard`);
  };

  if (isLoading) return <Loading />;
  const orgs = data ?? [];

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography variant="h6" fontWeight={700} sx={{ flexGrow: 1 }}>Athleet</Typography>
          <Tooltip title="Logout">
            <IconButton color="inherit" onClick={() => logout().then(() => navigate('/login'))} aria-label="Logout">
              <LogoutIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        {!selected ? (
          <Card sx={{ maxWidth: 520, mx: 'auto', p: 3 }}>
            <Typography variant="h6" gutterBottom>Select Academy</Typography>
            {orgs.length === 0 ? (
              <Typography color="text.secondary">You do not have access to any academy yet.</Typography>
            ) : (
              <Stack direction="row" spacing={1}>
                <Autocomplete
                  sx={{ flexGrow: 1 }}
                  options={orgs}
                  getOptionLabel={(o) => o.name}
                  value={picked}
                  onChange={(_, v) => setPicked(v)}
                  renderInput={(p) => <TextField {...p} label="Academy" />}
                />
                <Button variant="contained" disabled={!picked}
                  onClick={() => (picked!.primary && (picked!.affiliates?.length ?? 0) > 0 ? setSelected(picked) : open(picked!))}>
                  GO
                </Button>
              </Stack>
            )}
          </Card>
        ) : (
          <Stack spacing={3}>
            <Box>
              <Typography variant="overline" color="text.secondary">Primary Organization</Typography>
              <Box sx={{ maxWidth: 360 }}>
                <OrgCard org={selected} onClick={() => setModeFor(selected)} />
              </Box>
            </Box>
            <Box>
              <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={1} sx={{ mb: 1 }}>
                <Typography variant="overline" color="text.secondary">
                  Affiliated Organizations ({selected.affiliates?.length ?? 0})
                </Typography>
                <TextField sx={{ maxWidth: 280 }} placeholder="Search" value={search} onChange={(e) => setSearch(e.target.value)}
                  InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }} />
              </Stack>
              <Grid container spacing={2}>
                {affiliates.map((a) => (
                  <Grid key={a.id} size={{ xs: 12, sm: 6, md: 4 }}>
                    <OrgCard org={a} onClick={() => open(a)} />
                  </Grid>
                ))}
              </Grid>
            </Box>
            <Button onClick={() => setSelected(null)} sx={{ alignSelf: 'flex-start' }}>Back</Button>
          </Stack>
        )}
      </Container>

      <Dialog open={!!modeFor} onClose={() => setModeFor(null)}>
        <DialogTitle>{modeFor?.name}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Every Primary Organization can also operate as a self contained academy.
          </Typography>
          <List>
            <ListItemButton onClick={() => modeFor && open(modeFor, 'primary')}>
              <ListItemText primary="Primary Organization" secondary="Primary" />
            </ListItemButton>
            <ListItemButton onClick={() => modeFor && open(modeFor, 'academy')}>
              <ListItemText primary="Academy Admin" secondary="Affiliate" />
            </ListItemButton>
          </List>
        </DialogContent>
      </Dialog>
    </Box>
  );
}
