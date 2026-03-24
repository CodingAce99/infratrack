import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Asset } from "@/lib/types";

export interface UseAssetsResult {
  assets: Asset[];
  isLoading: boolean;
  isError: boolean;
}

export function useAssets(): UseAssetsResult {
  const {
    data: assets,
    error,
    isLoading,
  } = useSWR<Asset[]>("/api/v1/assets", fetcher, {
    refreshInterval: 60000,
  });

  if (isLoading) {
    return { assets: [], isLoading: true, isError: false };
  }

  if (error) {
    return { assets: [], isLoading: false, isError: true };
  }

  return { assets: assets ?? [], isLoading: false, isError: false };
}
