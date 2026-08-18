import React from 'react';
import { AlertCircle } from 'lucide-react';

interface FeatureLimitBannerProps {
  limit: number;
  isUnlimited: boolean;
  currentCount: number;
  featureName: string;
}

export default function FeatureLimitBanner({
  limit,
  isUnlimited,
  currentCount,
  featureName,
}: FeatureLimitBannerProps) {
  if (isUnlimited) {
    return null;
  }

  const percentage = (currentCount / limit) * 100;
  const isNearLimit = percentage >= 80;

  return (
    <div
      style={{
        padding: '1rem',
        marginBottom: '1.5rem',
        background: isNearLimit ? 'rgba(245, 158, 11, 0.1)' : 'rgba(59, 130, 246, 0.1)',
        border: `1px solid ${isNearLimit ? '#f59e0b' : '#3b82f6'}`,
        borderRadius: '0.5rem',
        display: 'flex',
        alignItems: 'center',
        gap: '1rem',
        color: isNearLimit ? '#f59e0b' : '#3b82f6',
      }}
    >
      <AlertCircle size={20} />
      <div style={{ flex: 1 }}>
        <strong>{featureName} limit:</strong> {currentCount} of {limit}
        {isNearLimit && <span> - You are nearing your limit!</span>}
      </div>
      <div
        style={{
          width: '100px',
          height: '8px',
          background: 'rgba(0, 0, 0, 0.2)',
          borderRadius: '4px',
          overflow: 'hidden',
        }}
      >
        <div
          style={{
            width: `${Math.min(percentage, 100)}%`,
            height: '100%',
            background: isNearLimit ? '#f59e0b' : '#3b82f6',
            transition: 'width 0.3s ease',
          }}
        />
      </div>
    </div>
  );
}

