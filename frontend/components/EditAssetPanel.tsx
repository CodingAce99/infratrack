"use client";

import { useState } from "react";
import { useSWRConfig } from "swr";
import {
  updateStatus,
  updateIp,
  updateCredentials,
  deleteAsset,
  ApiError,
} from "@/lib/api";
import { Asset, UpdateStatusRequest } from "@/lib/types";

import ConfirmDialog from "./ConfirmDialog";

interface EditAssetPanelProps {
  asset: Asset;
  onClose: () => void;
}

export default function EditAssetPanel({
  asset,
  onClose,
}: EditAssetPanelProps) {
  const [status, setStatus] = useState<UpdateStatusRequest["status"]>(
    asset.status,
  );
  const [ipAddress, setIpAddress] = useState(asset.ipAddress);
  const [username, setUsername] = useState(asset.username);
  const [password, setPassword] = useState("");
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { mutate } = useSWRConfig();

  const handleStatusSave = async () => {
    setError(null);

    try {
      await updateStatus(asset.id, { status });
      mutate("/api/v1/assets");
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Unexpected error. Please try again.");
      }
    }
  };

  const handleIpSave = async () => {
    setError(null);

    try {
      await updateIp(asset.id, { ipAddress });
      mutate("/api/v1/assets");
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setError("An asset with this IP address already exists.");
        } else {
          setError(err.message);
        }
      } else {
        setError("Unexpected error. Please try again.");
      }
    }
  };

  const handleCredentialsSave = async () => {
    setError(null);

    try {
      await updateCredentials(asset.id, { username, password });
      mutate("/api/v1/assets");
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Unexpected error. Please try again.");
      }
    }
  };

  const handleDelete = async () => {
    setError(null);

    try {
      await deleteAsset(asset.id);
      mutate("/api/v1/assets");
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Unexpected error. Please try again.");
      }
    }
  };

  return (
    <>
      <div className="border-t border-[#1a2535] p-5 flex flex-col gap-4">
        {/* STATUS */}
        <div className="flex items-center gap-3">
          <label className="text-[#4a5568] text-xs font-mono w-28">
            STATUS
          </label>
          <select
            value={status}
            onChange={(e) =>
              setStatus(e.target.value as UpdateStatusRequest["status"])
            }
            className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-1.5 text-sm flex-1"
          >
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="MAINTENANCE">MAINTENANCE</option>
          </select>
          <button
            type="button"
            onClick={handleStatusSave}
            className="bg-[#1a2535] text-[#e4e8ee] text-xs px-3 py-1.5 rounded hover:bg-[#2a3545]"
          >
            Save
          </button>
        </div>

        {/* IP ADDRESS */}
        <div className="flex items-center gap-3">
          <label className="text-[#4a5568] text-xs font-mono w-28">
            IP ADDRESS
          </label>
          <input
            type="text"
            value={ipAddress}
            onChange={(e) => setIpAddress(e.target.value)}
            className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-1.5 text-sm font-mono flex-1"
          />
          <button
            type="button"
            onClick={handleIpSave}
            className="bg-[#1a2535] text-[#e4e8ee] text-xs px-3 py-1.5 rounded hover:bg-[#2a3545]"
          >
            Save
          </button>
        </div>

        {/* CREDENTIALS */}
        <div className="flex flex-col gap-2">
          <label className="text-[#4a5568] text-xs font-mono">
            CREDENTIALS
          </label>
          <div className="flex items-center gap-3">
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="username"
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-1.5 text-sm font-mono flex-1"
            />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="password"
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-1.5 text-sm font-mono flex-1"
            />
            <button
              type="button"
              onClick={handleCredentialsSave}
              className="bg-[#1a2535] text-[#e4e8ee] text-xs px-3 py-1.5 rounded hover:bg-[#2a3545]"
            >
              Save
            </button>
          </div>
        </div>

        {/* ERROR */}
        {error && <p className="text-[#ef4444] text-xs font-mono">{error}</p>}

        {/* DELETE */}
        <button
          type="button"
          onClick={() => setShowConfirm(true)}
          className="w-full border border-[#ef4444] text-[#ef4444] text-sm py-2 rounded hover:bg-[#ef4444]/10 mt-2"
        >
          Delete asset
        </button>
      </div>

      {showConfirm && (
        <ConfirmDialog
          message={`Delete ${asset.name}? This action cannot be undone.`}
          onConfirm={handleDelete}
          onCancel={() => setShowConfirm(false)}
        />
      )}
    </>
  );
}
