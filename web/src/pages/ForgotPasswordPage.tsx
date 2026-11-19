import { useState, type FormEvent } from 'react';
import { Alert, Button, Link, Stack, TextField } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { api, errorMessage } from '../api/client';

export function ForgotPasswordPage() {
  const [identifier, setIdentifier] = useState('');
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/auth/forgot-password', { identifier: identifier.trim() });
      setDone(true);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <AuthLayout title="Reset your password">
      {done ? (
        // Same message whether or not the account exists, so accounts cannot be discovered.
        <Alert severity="success">If an account exists for this email or mobile, a reset link has been sent.</Alert>
      ) : (
        <form onSubmit={submit}>
          <Stack spacing={2}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Email / Mobile" required autoFocus value={identifier}
              onChange={(e) => setIdentifier(e.target.value)} inputProps={{ maxLength: 254 }} />
            <Button type="submit" variant="contained" disabled={!identifier.trim()}>Send reset link</Button>
          </Stack>
        </form>
      )}
      <Link component={RouterLink} to="/login" sx={{ display: 'block', mt: 2 }}>Back to sign in</Link>
    </AuthLayout>
  );
}
