import { queryOptions } from "@tanstack/react-query";
import { queryKeys } from "../../lib/queries/keys";
import { getLandingContent, getLoginContent } from "../../services/marketingContentApi";

export const landingContentQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.marketing.page("landing"),
    queryFn: getLandingContent,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
  });

export const loginContentQueryOptions = () =>
  queryOptions({
    queryKey: queryKeys.marketing.page("login"),
    queryFn: getLoginContent,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
  });

