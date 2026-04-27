'use client';

import { useState } from 'react';
import { useSWRConfig } from 'swr';
import { createAsset, ApiError } from '@/lib/api';
import { CreateAssetRequest } from '@/lib/types';

interface CreateAssetModalProps {
  onClose: () => void;
}

export default function CreateAssetModal({ onClose }: CreateAssetModalProps) {
  const [name, setName] = useState('');
  const [type, setType] = useState<CreateAssetRequest['type']>('SERVER');
  const [ipAddress, setIpAddress] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { mutate } = useSWRConfig();

  const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    // Without this, the browser would reload the page on form submit.
    // In a SPA we never want that.
    e.preventDefault();

    setError(null);
    setIsSubmitting(true);

    try {
      await createAsset({ name, type, ipAddress, username, password });
      mutate('/api/v1/assets');
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setError('An asset with this IP address already exists.');
        } else {
          setError(err.message);
        }
      } else {
        setError('Unexpected error. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-50"
      style={{ backgroundColor: 'rgba(0, 0, 0, 0.6)' }}
      onClick={onClose}
    >
      <div
        className="bg-[#111621] border border-[#1a2535] rounded-lg p-6 w-full max-w-md"
        onClick={e => e.stopPropagation()}
        // stopPropagation prevents clicks inside the modal from bubbling
        // up to the backdrop and closing it accidentally.
      >
        <h2 className="text-[#e4e8ee] font-semibold text-base mb-5">
          New asset
        </h2>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">

          <div className="flex flex-col gap-1">
            <label className="text-[#4a5568] text-xs font-mono">NAME</label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              required
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-2 text-sm focus:outline-none focus:border-[#10b981]"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-[#4a5568] text-xs font-mono">TYPE</label>
            <select
              value={type}
              onChange={e => setType(e.target.value as CreateAssetRequest['type'])}
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-2 text-sm focus:outline-none focus:border-[#10b981]"
            >
              <option value="SERVER">SERVER</option>
              <option value="ROUTER">ROUTER</option>
              <option value="IOT_DEVICE">IOT_DEVICE</option>
            </select>
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-[#4a5568] text-xs font-mono">IP ADDRESS</label>
            <input
              type="text"
              value={ipAddress}
              onChange={e => setIpAddress(e.target.value)}
              required
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-2 text-sm font-mono focus:outline-none focus:border-[#10b981]"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-[#4a5568] text-xs font-mono">USERNAME</label>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-2 text-sm font-mono focus:outline-none focus:border-[#10b981]"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-[#4a5568] text-xs font-mono">PASSWORD</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              className="bg-[#0c0f14] border border-[#1a2535] text-[#e4e8ee] rounded px-3 py-2 text-sm font-mono focus:outline-none focus:border-[#10b981]"
            />
          </div>

          {error && (
            <p className="text-[#ef4444] text-xs font-mono">{error}</p>
          )}

          <div className="flex justify-end gap-3 mt-2">
            <button
              type="button"
              onClick={onClose}
              className="text-[#4a5568] text-sm hover:underline"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="bg-[#10b981] text-white text-sm px-4 py-2 rounded hover:bg-[#059669] disabled:opacity-50"
            >
              {isSubmitting ? 'Creating...' : 'Create asset'}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}
