"use client";

import { useState } from "react";
import CreateAssetModal from "./CreateAssetModal";

interface HeaderProps {
  assetCount: number;
  isConnected: boolean;
}

export default function Header({ assetCount, isConnected }: HeaderProps) {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
      <header className="flex items-center justify-between px-6 py-4 border-b border-[#1a2535]">
        {/* Logo + title */}
        <div className="flex items-center gap-3">
          <span className="text-[#10b981] font-mono text-xl font-bold">▶</span>
          <span className="text-[#e4e8ee] font-semibold text-lg tracking-wide">
            INFRATRACK
          </span>
        </div>

        {/* Asset counter */}
        <div className="text-[#4a5568] text-sm font-mono">
          {assetCount} {assetCount === 1 ? "asset" : "assets"} monitored
        </div>

        {/* Right side: add button + connection indicator */}
        <div className="flex items-center gap-4">
          <button
            onClick={() => setIsModalOpen(true)}
            className="bg-[#10b981] text-white text-sm px-3 py-1.5 rounded hover:bg-[#059669] font-mono"
          >
            + Add Asset
          </button>

          <div className="flex items-center gap-2">
            <span
              className={`w-2 h-2 rounded-full ${isConnected ? "bg-[#10b981]" : "bg-[#ef4444]"}`}
            />
            <span className="text-sm font-mono text-[#4a5568]">
              {isConnected ? "Connected" : "Connection lost"}
            </span>
          </div>
        </div>
      </header>

      {isModalOpen && (
        <CreateAssetModal onClose={() => setIsModalOpen(false)} />
      )}
    </>
  );
}
