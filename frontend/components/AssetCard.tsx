"use client";

import { Asset, MetricSnapshot } from "@/lib/types";
import MetricGauge from "./MetricGauge";
import StatusBadge from "./StatusBadge";
import useSWR from "swr";
import { fetcher } from "@/lib/api";

interface AssetCardProps {
  asset: Asset;
}

export default function AssetCard({ asset }: AssetCardProps) {
  const { data: metrics = [] } = useSWR<MetricSnapshot[]>(
    `/api/v1/assets/${asset.id}/metrics/history?limit=20`,
    fetcher,
    { refreshInterval: 60000 },
  );

  const latest = metrics[0] ?? null;

  const borderColor = {
    ACTIVE: "#10b981",
    MAINTENANCE: "#f59e0b",
    INACTIVE: "#6b7280",
  }[asset.status];

  return (
    <div
      style={{ borderLeft: `3px solid ${borderColor}` }}
      className="bg-[#111621] rounded-lg p-5 border border-[#1a2535]"
    >
      <div className="flex items-center justify-between mb-4">
        <div>
          <span className="text-[#e4e8ee] font-semibold">{asset.name}</span>
          <p className="text-[#4a5568] text-xs font-mono mt-0.5">
            {asset.type} · {asset.ipAddress}
          </p>
        </div>
        <StatusBadge assetStatus={asset.status} />
      </div>
      {latest ? (
        <div className="grid grid-cols-3 gap-4">
          <MetricGauge
            label="CPU Usage"
            value={latest.cpuUsage}
            history={metrics.map((m) => m.cpuUsage)}
          />
          <MetricGauge
            label="Memory Usage"
            value={latest.memoryUsage}
            history={metrics.map((m) => m.memoryUsage)}
          />
          <MetricGauge
            label="Disk Usage"
            value={latest.diskUsage}
            history={metrics.map((m) => m.diskUsage)}
          />
        </div>
      ) : (
        <p className="text-[#4a5568] text-sm font-mono">No data available</p>
      )}
    </div>
  );
}
