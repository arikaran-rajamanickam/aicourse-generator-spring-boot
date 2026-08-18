import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { apiFetch } from '../services/apiClient';
import { useAuth } from '@/auth/AuthContext';

export interface FeatureConfig {
  allowed: boolean;
  limit: number;
  usage: number;
}

export interface FeatureContextType {
  features: Record<string, FeatureConfig>;
  featuresLoading: boolean;
  refreshFeatures: () => Promise<void>;
}

const FeatureContext = createContext<FeatureContextType | undefined>(undefined);

export function FeatureProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const [features, setFeatures] = useState<Record<string, FeatureConfig>>({});
  const [featuresLoading, setFeaturesLoading] = useState(true);

  // Load features when auth token is present.
  useEffect(() => {
    if (!token) {
      setFeatures({});
      setFeaturesLoading(false);
      return;
    }
    loadFeatures();
  }, [token]);

  const loadFeatures = async () => {
    try {
      setFeaturesLoading(true);
      const response = await apiFetch('/api/features/me');
      if (response?.data) {
        setFeatures(response.data);
      } else if (response && typeof response === 'object') {
        setFeatures(response as Record<string, FeatureConfig>);
      }
    } catch (error) {
      console.error('Failed to load features:', error);
      // Set default features if loading fails
      setFeatures({
        COURSE_CREATE: { allowed: true, limit: 5, usage: 0 },
        PROJECT_CREATE: { allowed: true, limit: 3, usage: 0 },
        COURSE_SHARE: { allowed: true, limit: -1, usage: 0 },
      });
    } finally {
      setFeaturesLoading(false);
    }
  };

  const value: FeatureContextType = {
    features,
    featuresLoading,
    refreshFeatures: loadFeatures,
  };

  return (
    <FeatureContext.Provider value={value}>{children}</FeatureContext.Provider>
  );
}

export function useFeatureContext(): FeatureContextType {
  const context = useContext(FeatureContext);
  if (!context) {
    throw new Error('useFeatureContext must be used within a FeatureProvider');
  }
  return context;
}

