import { useState, type FormEvent } from 'react';
import { Alert, Button, Link, Stack, TextField } from '@mui/material';
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/client';

/** Only same-app paths are allowed as a post-login redirect (no open redirects). */
function safeReturnPath(p: unknown): string {
  return typeof p === 'string' && /^\/(?![\\/])/.test(p) ? p : '/organizations';
}

export function LoginPage() {
  const { user, ready, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [step, setStep] = useState<1 | 2>(1);
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (ready && user) return <Navigate to="/organizations" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (step === 1) {
      if (identifier.trim()) setStep(2);
      return;
    }
    setBusy(true);
    try {
      await login(identifier.trim(), password);
      navigate(safeReturnPath((location.state as { from?: string } | null)?.from), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setPassword('');
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout title="Sign in to your academy">
      <form onSubmit={submit} noValidate>
        <Stack spacing={2}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Email / Mobile"
            required
            autoFocus={step === 1}
            autoComplete="username"
            value={identifier}
            disabled={step === 2}
            onChange={(e) => setIdentifier(e.target.value)}
            inputProps={{ maxLength: 254 }}
          />
          {step === 2 && (
            <TextField
              label="Password"
              type="password"
              required
              autoFocus
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              inputProps={{ maxLength: 72 }}
            />
          )}
          <Button type="submit" variant="contained" size="large" disabled={busy}>
            {step === 1 ? 'Submit' : 'Sign in'}
          </Button>
          {step === 2 && (
            <Stack direction="row" justifyContent="space-between">
              <Link component="button" type="button" onClick={() => { setStep(1); setPassword(''); }}>
                Change Email / Mobile
              </Link>
              <Link component={RouterLink} to="/forgot">Forgot password?</Link>
            </Stack>
          )}
        </Stack>
      </form>
    </AuthLayout>
  );
}
