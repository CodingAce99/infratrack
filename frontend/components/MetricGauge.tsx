"use client";

import Sparkline from "./Sparkline";

interface MetricGaugeProps {
  label: string;
  value: number;
  history: number[];
}

function getColor(label: string, value: number): string {
  if (label === "Disk") {
    if (value > 85) return "#ef4444";
    if (value > 70) return "#f59e0b";
    return "#6b7280";
  }
  if (value > 80) return "#ef4444";
  if (value > 60) return "#f59e0b";
  return "#10b981";
}

export default function MetricGauge({
  label,
  value,
  history,
}: MetricGaugeProps) {
  const color = getColor(label, value);

  return (
    <div>
      <div className="flex justify-between items-center mb-1">
        <span className="text-[#4a5568] text-xs font-mono">{label}</span>
        <span className="text-[#e4e8ee] text-sm font-mono font-bold">{value}%</span>
      </div>
      <div className="w-full bg-[#1a2535] rounded-full h-1 mt-2">
        <div
          className="h-1 rounded-full transition-all"
          style={{ width: `${value}%`, backgroundColor: color }}
        />
      </div>
      <Sparkline data={history} color={color} />
    </div>
  );
}
