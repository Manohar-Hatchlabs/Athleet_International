import { useEffect, useState } from 'react';
import { Alert, Button, Card, CardContent, Grid2 as Grid, List, ListItemButton, ListItemText, MenuItem, TextField, Typography } from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage, fieldErrors } from '../api/client';
import { useOrg } from '../org/OrgContext';
import { PageHeader } from '../components/common';

const SECTIONS = [
  'Branding Preference', 'Mobile Apps', 'Notifications', 'Social Media Handles', 'Terms & Conditions',
  'Dashboard Configuration', 'Attendance Configuration', 'Fee Settings', 'Cascade Activities', 'Activity Categories',
  'Competition / Rank Master', 'Player Unique ID Auto-generation', 'Student Rating Categories',
];

export function SettingsPage() {
  const { orgId, access, can } = useOrg();
  const qc = useQueryClient();
  const org = access.organization;
  const manage = can('SETTINGS', 'MANAGE');
  const [section, setSection] = useState(SECTIONS[0]);
  const [v, setV] = useState({ centerLabel: org.centerLabel, currency: org.currency, dateFormat: org.dateFormat });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);
  useEffect(() => setV({ centerLabel: org.centerLabel, currency: org.currency, dateFormat: org.dateFormat }), [org]);

  const save = useMutation({
    mutationFn: () => api.put(`/organizations/${orgId}/branding`, { ...v, centerLabel: v.centerLabel.trim(), currency: v.currency.trim().toUpperCase() }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['my-access', orgId] }); setErrors({}); setMsg({ ok: true, text: 'Saved.' }); },
    onError: (e) => { setErrors(fieldErrors(e)); setMsg({ ok: false, text: errorMessage(e) }); },
  });

  return (
    <>
      <PageHeader title="Settings" />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined">
            <List dense>
              {SECTIONS.map((s) => (
                <ListItemButton key={s} selected={s === section} onClick={() => setSection(s)}>
                  <ListItemText primary={s} />
                </ListItemButton>
              ))}
            </List>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>{section}</Typography>
              {section !== 'Branding Preference' ? (
                <Alert severity="info">
                  {section === 'Fee Settings' ? 'Fees are scheduled for the final phase.' : 'This setting will be added in a later phase.'}
                </Alert>
              ) : (
                <Grid container spacing={2}>
                  {msg && <Grid size={12}><Alert severity={msg.ok ? 'success' : 'error'} onClose={() => setMsg(null)}>{msg.text}</Alert></Grid>}
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField select label="Currency" value={v.currency} disabled={!manage} onChange={(e) => setV({ ...v, currency: e.target.value })}>
                      {['INR', 'AED', 'SAR', 'QAR', 'KWD', 'OMR', 'BHD', 'GBP', 'USD'].map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
                    </TextField>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField select label="Date format" value={v.dateFormat} disabled={!manage} onChange={(e) => setV({ ...v, dateFormat: e.target.value })}>
                      {['d/m/Y', 'm/d/Y', 'Y-m-d'].map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
                    </TextField>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField label="Center Name Title" value={v.centerLabel} disabled={!manage} error={!!errors.centerLabel}
                      helperText={errors.centerLabel ?? 'e.g. "Class" — the label used for a center'}
                      onChange={(e) => setV({ ...v, centerLabel: e.target.value })} inputProps={{ maxLength: 30 }} />
                  </Grid>
                  {manage && <Grid size={12}><Button variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>Save</Button></Grid>}
                </Grid>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
