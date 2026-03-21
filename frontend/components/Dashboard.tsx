"use client";

import { Asset, MetricSnapshot } from "@/lib/types";
import Header from "./Header";
import AssetCard from "./AssetCard";

// Datos hardcodeados para Sprint 5.1
// En Sprint 5.2 estos vendrán del hook useAssets()
const MOCK_ASSETS: Asset[] = [
  {
    id: "1",
    name: "web-server-01",
    type: "SERVER",
    ipAddress: "192.168.1.10",
    status: "ACTIVE",
    username: "admin",
  },
  {
    id: "2",
    name: "router-core",
    type: "ROUTER",
    ipAddress: "192.168.1.1",
    status: "MAINTENANCE",
    username: "admin",
  },
];

const MOCK_METRICS: Record<string, MetricSnapshot[]> = {
  "1": [
    {
      assetId: "1",
      cpuUsage: 67,
      memoryUsage: 54,
      diskUsage: 41,
      collectedAt: "2026-03-21T10:00:00Z",
    },
    {
      assetId: "1",
      cpuUsage: 71,
      memoryUsage: 58,
      diskUsage: 41,
      collectedAt: "2026-03-21T09:59:00Z",
    },
    {
      assetId: "1",
      cpuUsage: 45,
      memoryUsage: 50,
      diskUsage: 40,
      collectedAt: "2026-03-21T09:58:00Z",
    },
  ],
  "2": [],
};

export default function Dashboard() {
  return (
    <div className="min-h-screen">
      <Header assetCount={MOCK_ASSETS.length} isConnected={true} />
      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="grid grid-cols-1 gap-6">
          {MOCK_ASSETS.map((asset) => (
            <AssetCard
              key={asset.id}
              asset={asset}
              metrics={MOCK_METRICS[asset.id] ?? []}
            />
          ))}
        </div>
      </main>
    </div>
  );
}
