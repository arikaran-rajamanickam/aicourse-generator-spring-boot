import React from 'react';
import { AlertCircle } from 'lucide-react';

interface FeatureRestrictedButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  allowed: boolean;
  atLimit: boolean;
  loading: boolean;
}

export default function FeatureRestrictedButton({
  allowed,
  atLimit,
  loading,
  disabled,
  children,
  title,
  ...props
}: FeatureRestrictedButtonProps) {
  const isDisabled = disabled || loading || !allowed || atLimit;
  const buttonTitle = !allowed
    ? 'This feature is not available'
    : atLimit
      ? 'You have reached your limit for this feature'
      : title;

  return (
    <button
      {...props}
      disabled={isDisabled}
      title={buttonTitle}
      style={{
        opacity: isDisabled ? 0.6 : 1,
        cursor: isDisabled ? 'not-allowed' : 'pointer',
        ...props.style,
      }}
    >
      {loading && <AlertCircle size={16} style={{ marginRight: '0.5rem' }} />}
      {children}
    </button>
  );
}

