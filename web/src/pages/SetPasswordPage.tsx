import { useEffect, useState, type FormEvent } from 'react';
import { Alert, Button, Link, Stack, TextField } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { api, errorMessage } from '../api/client';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

export function SetPasswordPage() {
  const [token, setToken] = useState<string | null>(null);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  useEffect(() => {
    // The token arrives in the URL fragment (never sent to servers or written to access logs).
    // Read it once, then strip it from the address bar and history.
    const match = window.location.hash.match(/token=([A-Za-z0-9_-]{20,100})/);
    if (match) setToken(match[1]);
    window.history.replaceState(null, '', window.location.pathname);
  }, []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!PASSWORD_RULE.test(password)) return setError('Use 8-72 characters with at least one letter and one number.');
    if (password !== confirm) return setError('Passwords do not match.');
    try {
      await api.post('/auth/set-password', { token, newPassword: password });
      setDone(true);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <AuthLayout title="Set your password">
      {done ? (
        <Alert severity="success">Password saved. You can now sign in.</Alert>
      ) : !token ? (
        <Alert severity="warning">This link is invalid or has expired. Ask your academy admin for a new invite.</Alert>
      ) : (
        <form onSubmit={submit}>
          <Stack spacing={2}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="New password" type="password" autoComplete="new-password" value={password}
              onChange={(e) => setPassword(e.target.value)} helperText="At least 8 characters, with a letter and a number" />
            <TextField label="Confirm password" type="password" autoComplete="new-password" value={confirm}
              onChange={(e) => setConfirm(e.target.value)} />
            <Button type="submit" variant="contained">Save password</Button>
          </Stack>
        </form>
      )}
      <Link component={RouterLink} to="/login" sx={{ display: 'block', mt: 2 }}>Go to sign in</Link>
    </AuthLayout>
  );
}
