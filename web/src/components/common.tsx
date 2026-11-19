import { useEffect, useState, type ReactNode } from 'react';
import {
  Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, InputAdornment, Paper, Stack,
  TextField, Typography,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';

export function PageHeader({ title, actions }: { title: string; actions?: ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
      <Typography variant="h5" fontWeight={700}>{title}</Typography>
      <Stack direction="row" spacing={1} alignItems="center">{actions}</Stack>
    </Stack>
  );
}

export function SearchBox({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <TextField
      placeholder="Search"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      sx={{ width: { xs: '100%', sm: 260 } }}
      inputProps={{ maxLength: 100 }}
      InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
    />
  );
}

export function Panel({ children }: { children: ReactNode }) {
  return <Paper variant="outlined" sx={{ overflow: 'hidden' }}>{children}</Paper>;
}

export function EmptyState({ text }: { text: string }) {
  return <Box sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>{text}</Box>;
}

export function ConfirmDialog(props: {
  open: boolean; title: string; text: string; confirmLabel?: string; danger?: boolean;
  onClose: () => void; onConfirm: () => Promise<unknown> | void | null;
}) {
  const [busy, setBusy] = useState(false);
  return (
    <Dialog open={props.open} onClose={props.onClose}>
      <DialogTitle>{props.title}</DialogTitle>
      <DialogContent><DialogContentText>{props.text}</DialogContentText></DialogContent>
      <DialogActions>
        <Button onClick={props.onClose}>Cancel</Button>
        <Button
          variant="contained"
          color={props.danger ? 'error' : 'primary'}
          disabled={busy}
          onClick={async () => {
            setBusy(true);
            try { await props.onConfirm(); } finally { setBusy(false); }
          }}
        >
          {props.confirmLabel ?? 'Confirm'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Debounced value for search boxes, so typing doesn't send a request per keystroke. */
export function useDebounced<T>(value: T, ms = 300) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}

/** Form state helper: values, per-field errors (from the API's 400 field list), setters. */
export function useForm<T extends Record<string, unknown>>(initial: T) {
  const [values, setValues] = useState<T>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = <K extends keyof T>(k: K) => (v: T[K]) => setValues((s) => ({ ...s, [k]: v }));
  const field = (k: keyof T & string) => ({
    value: (values[k] ?? '') as string,
    onChange: (e: { target: { value: string } }) => setValues((s) => ({ ...s, [k]: e.target.value })),
    error: !!errors[k],
    helperText: errors[k],
  });
  return { values, setValues, errors, setErrors, set, field };
}

/** Converts '' to undefined so optional fields are omitted rather than sent as empty strings. */
export function clean<T extends Record<string, unknown>>(o: T): T {
  const out: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(o)) out[k] = typeof v === 'string' ? (v.trim() === '' ? undefined : v.trim()) : v;
  return out as T;
}
