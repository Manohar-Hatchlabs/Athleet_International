import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, refreshSession, setAccessToken, setSessionExpiredHandler } from '../api/client';
import type { Me, TokenResponse } from '../api/types';

interface AuthState {
  user: Me | null;
  ready: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Me | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setSessionExpiredHandler(() => {
      setAccessToken(null);
      setUser(null);
    });
    // Restore the session from the refresh cookie (the access token is never persisted).
    (async () => {
      if (await refreshSession()) {
        try {
          setUser(await api.get<Me>('/auth/me'));
        } catch {
          setUser(null);
        }
      }
      setReady(true);
    })();
  }, []);

  const login = useCallback(async (identifier: string, password: string) => {
    const res = await api.post<TokenResponse>('/auth/login', { identifier, password });
    setAccessToken(res.accessToken);
    setUser(res.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      setAccessToken(null);
      setUser(null);
      sessionStorage.removeItem('athleet.org');
    }
  }, []);

  const value = useMemo(() => ({ user, ready, login, logout }), [user, ready, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside AuthProvider');
  return ctx;
}
