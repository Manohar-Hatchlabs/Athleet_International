import { useEffect, useState } from 'react';
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, Grid2 as Grid, InputLabel, MenuItem,
  Select, Step, StepLabel, Stepper, TextField,
} from '@mui/material';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, errorMessage, fieldErrors } from '../api/client';
import type { Student } from '../api/types';
import { useOrg } from '../org/OrgContext';
import { clean, useForm } from '../components/common';
import { BLOOD_GROUPS, COUNTRY_CODES, useBatchOptions, useCenterOptions } from '../components/options';

const EMPTY = {
  centerId: '', batchIds: [] as string[], firstName: '', lastName: '', email: '', gender: '', dateOfBirth: '',
  countryCode: '+91', mobile: '', uniqueId: '', bloodGroup: '', country: '', state: '', city: '', address: '',
  pinCode: '', schoolName: '', schoolAddress: '', grade: '', dominantSide: '', medicalCondition: '',
};

const STEP1 = ['centerId', 'batchIds', 'firstName', 'lastName', 'email', 'gender', 'dateOfBirth', 'countryCode', 'mobile'];

export function StudentDialog({ open, student, onClose }: { open: boolean; student?: Student; onClose: () => void }) {
  const { orgId, centerLabel } = useOrg();
  const qc = useQueryClient();
  const f = useForm({ ...EMPTY });
  const [step, setStep] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const centers = useCenterOptions(orgId);
  const batches = useBatchOptions(orgId, f.values.centerId || undefined);

  useEffect(() => {
    if (!open) return;
    setStep(0);
    setError(null);
    f.setErrors({});
    if (student) {
      const s = student as unknown as Record<string, unknown>;
      const v: Record<string, unknown> = { ...EMPTY };
      for (const k of Object.keys(EMPTY)) if (s[k] !== undefined && s[k] !== null) v[k] = s[k];
      v.centerId = student.center.id;
      v.batchIds = student.batches.map((b) => b.id);
      f.setValues(v as typeof EMPTY);
    } else {
      f.setValues({ ...EMPTY });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, student]);

  const save = useMutation({
    mutationFn: () => {
      const body = clean({ ...f.values, gender: f.values.gender || undefined, dominantSide: f.values.dominantSide || undefined });
      return student ? api.put(`/orgs/${orgId}/students/${student.id}`, body) : api.post(`/orgs/${orgId}/students`, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['students', orgId] });
      qc.invalidateQueries({ queryKey: ['dashboard', orgId] });
      onClose();
    },
    onError: (e) => {
      const fe = fieldErrors(e);
      f.setErrors(fe);
      setError(errorMessage(e));
      if (Object.keys(fe).some((k) => STEP1.includes(k))) setStep(0);
    },
  });

  const step1Valid = f.values.centerId && f.values.batchIds.length > 0 && f.values.firstName.trim() && f.values.gender && f.values.dateOfBirth;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{student ? 'Edit Student' : 'Add Student'}</DialogTitle>
      <DialogContent dividers>
        <Stepper activeStep={step} sx={{ mb: 3 }}>
          <Step><StepLabel>Student details</StepLabel></Step>
          <Step><StepLabel>Address & health</StepLabel></Step>
        </Stepper>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {step === 0 ? (
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <FormControl required error={!!f.errors.centerId}>
                <InputLabel>{centerLabel}</InputLabel>
                <Select label={centerLabel} value={f.values.centerId}
                  onChange={(e) => f.setValues((s) => ({ ...s, centerId: e.target.value, batchIds: [] }))}>
                  {(centers.data ?? []).map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <FormControl required error={!!f.errors.batchIds} disabled={!f.values.centerId}>
                <InputLabel>Batch</InputLabel>
                <Select multiple label="Batch" value={f.values.batchIds}
                  onChange={(e) => f.set('batchIds')(typeof e.target.value === 'string' ? e.target.value.split(',') : e.target.value)}>
                  {(batches.data ?? []).map((b) => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="First Name" required {...f.field('firstName')} inputProps={{ maxLength: 60 }} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Last Name" {...f.field('lastName')} inputProps={{ maxLength: 60 }} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Email Id" type="email" {...f.field('email')} inputProps={{ maxLength: 254 }} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select label="Gender" required {...f.field('gender')}>
                <MenuItem value="MALE">Male</MenuItem><MenuItem value="FEMALE">Female</MenuItem><MenuItem value="OTHER">Other</MenuItem>
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField label="Date of Birth" type="date" required {...f.field('dateOfBirth')} InputLabelProps={{ shrink: true }}
                inputProps={{ max: new Date().toISOString().slice(0, 10) }} />
            </Grid>
            <Grid size={{ xs: 4, sm: 2 }}>
              <TextField select label="Code" {...f.field('countryCode')}>
                {COUNTRY_CODES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 8, sm: 4 }}>
              <TextField label="Mobile" {...f.field('mobile')} inputProps={{ inputMode: 'numeric', maxLength: 15 }} />
            </Grid>
          </Grid>
        ) : (
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Unique ID" {...f.field('uniqueId')} inputProps={{ maxLength: 40 }} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select label="Blood Group" {...f.field('bloodGroup')}>
                <MenuItem value="">—</MenuItem>
                {BLOOD_GROUPS.map((b) => <MenuItem key={b} value={b}>{b}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}><TextField label="Country" {...f.field('country')} /></Grid>
            <Grid size={{ xs: 12, sm: 4 }}><TextField label="State" {...f.field('state')} /></Grid>
            <Grid size={{ xs: 12, sm: 4 }}><TextField label="City" {...f.field('city')} /></Grid>
            <Grid size={{ xs: 12, sm: 8 }}><TextField label="Address" {...f.field('address')} inputProps={{ maxLength: 300 }} /></Grid>
            <Grid size={{ xs: 12, sm: 4 }}><TextField label="Pin Code" {...f.field('pinCode')} inputProps={{ maxLength: 12 }} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="School Name" {...f.field('schoolName')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="School Address" {...f.field('schoolAddress')} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}><TextField label="Grade" {...f.field('grade')} inputProps={{ maxLength: 20 }} /></Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField select label="Dominant Side" {...f.field('dominantSide')}>
                <MenuItem value="">—</MenuItem><MenuItem value="LEFT">Left</MenuItem><MenuItem value="RIGHT">Right</MenuItem><MenuItem value="BOTH">Both</MenuItem>
              </TextField>
            </Grid>
            <Grid size={12}><TextField label="Medical Condition" multiline minRows={2} {...f.field('medicalCondition')} inputProps={{ maxLength: 1000 }} /></Grid>
          </Grid>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        {step === 1 && <Button onClick={() => setStep(0)}>Back</Button>}
        {step === 0
          ? <Button variant="contained" disabled={!step1Valid} onClick={() => setStep(1)}>Next</Button>
          : <Button variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>Save</Button>}
      </DialogActions>
    </Dialog>
  );
}
