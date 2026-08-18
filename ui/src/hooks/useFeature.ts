import { useFeatureContext } from '../context/FeatureContext';

/**
 * useFeature("COURSE_CREATE")
 * Returns { allowed: boolean, limit: number, isUnlimited: boolean, loading: boolean }
 *
 * limit === -1 means unlimited.
 * If the feature is not yet loaded, allowed defaults to false (safe default).
 */
export function useFeature(featureKey: string) {
  const { features, featuresLoading } = useFeatureContext();

  if (featuresLoading) {
    return {
      allowed: false,
      limit: 0,
      usage: 0,
      isUnlimited: false,
      atLimit: false,
      loading: true,
    };
  }

  const config = features[featureKey];
  if (!config) {
    return {
      allowed: false,
      limit: 0,
      usage: 0,
      isUnlimited: false,
      atLimit: false,
      loading: false,
    };
  }

  const isUnlimited = config.limit === -1;
  const usage = config.usage || 0;
  const atLimit = !isUnlimited && usage >= config.limit;

  return {
    allowed: config.allowed,
    limit: config.limit,
    usage,
    isUnlimited,
    atLimit,
    loading: false,
  };
}

