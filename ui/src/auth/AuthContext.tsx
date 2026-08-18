import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { apiFetch } from '../services/apiClient';

const TOKEN_STORAGE_KEY = 'token';
const USER_STORAGE_KEY = 'auth_user';

export interface User {
  id?: string;
  handle?: string;
  displayName?: string;
  username?: string;
  email?: string;
  roles?: string[];
  [key: string]: any;
}

export interface AuthContextType {
  token: string | null;
  user: User | null;
  loading: boolean;
  login: (token: string, user?: User) => void;
  logout: () => void;
  setUser: (user: User | null) => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const normalizeUser = (raw: unknown): User | null => {
    if (!raw || typeof raw !== 'object') return null;
    const value = raw as User;
    const displayName = value.displayName ?? value.username ?? value.handle;
    const handle = value.handle ?? value.username;
    return { ...value, displayName, handle, username: handle };
  };

  const readStoredUser = (): User | null => {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    if (!raw) return null;
    try {
      return normalizeUser(JSON.parse(raw));
    } catch {
      localStorage.removeItem(USER_STORAGE_KEY);
      return null;
    }
  };

  const shouldInvalidateSession = (error: unknown): boolean => {
    const message = error instanceof Error ? error.message.toLowerCase() : String(error).toLowerCase();
    return (
      message.includes('401') ||
      message.includes('403') ||
      message.includes('unauthorized') ||
      message.includes('forbidden') ||
      message.includes('jwt') ||
      message.includes('token')
    );
  };

  // Initialize from localStorage on mount
  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (storedToken) {
      setToken(storedToken);

      const storedUser = readStoredUser();
      if (storedUser) {
        setUser(storedUser);
      }

      setLoading(false);
      void fetchUserProfile(true);
    } else {
      setLoading(false);
    }
  }, []);

  const fetchUserProfile = async (clearSessionOnAuthError = false) => {
    try {
      const response = await apiFetch('/api/auth/me');
      const data = response?.data ?? response;
      const normalizedUser = normalizeUser(data);
      if (normalizedUser) {
        setUser(normalizedUser);
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(normalizedUser));
      }
    } catch (error) {
      console.error('Failed to fetch user profile:', error);
      if (clearSessionOnAuthError && shouldInvalidateSession(error)) {
        logout();
      }
    } finally {
      setLoading(false);
    }
  };

  const login = (newToken: string, userData?: User) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, newToken);
    setToken(newToken);

    const normalizedUser = normalizeUser(userData);
    if (normalizedUser) {
      setUser(normalizedUser);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(normalizedUser));
    } else {
      // Fetch user profile if not provided
      setLoading(true);
      void fetchUserProfile(true);
    }
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
    setLoading(false);
  };

  const value: AuthContextType = {
    token,
    user,
    loading,
    login,
    logout,
    setUser,
    isAuthenticated: token !== null,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
