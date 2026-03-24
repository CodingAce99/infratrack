"use client";

import Header from "./Header";
import AssetCard from "./AssetCard";
import { useAssets } from "@/hooks/useAssets";

export default function Dashboard() {
  const { assets, isLoading, isError } = useAssets();

  return (
    <div className="min-h-screen">
      <Header assetCount={assets.length} isConnected={!isError} />
      <main className="max-w-6xl mx-auto px-6 py-8">
        {isLoading && (
          <p className="text-[#4a5568] font-mono text-sm">
            Connecting to backend...
          </p>
        )}

        {isError && (
          <p className="text-[#ef4444] font-mono text-sm">
            Unable to reach backend. Retrying automatically.
          </p>
        )}

        <div className="grid grid-cols-1 gap-6">
          {assets.map((asset) => (
            <AssetCard key={asset.id} asset={asset} />
          ))}
        </div>
      </main>
    </div>
  );
}
