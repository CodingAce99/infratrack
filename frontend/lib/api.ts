import { Asset, CreateAssetRequest, UpdateCredentialsRequest, UpdateIpRequest, UpdateStatusRequest } from "./types";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "";

export async function fetcher<T>(url: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${url}`);

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function createAsset(data: CreateAssetRequest): Promise<Asset> {
  const res= await fetch(`${API_BASE_URL}/api/v1/assets`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const error = await res.json();
    throw new ApiError(res.status, error.error || 'Failed to create asset');
  }
  return res.json();
}

export async function updateStatus(id: string, data: UpdateStatusRequest): Promise<Asset> {
  const res = await fetch(`${API_BASE_URL}/api/v1/assets/${id}/status`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json'},
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const error = await res.json();
    throw new ApiError(res.status, error.error || 'Failed to update status')
  }
  return res.json();
}

export async function updateIp(id: string, data: UpdateIpRequest): Promise<Asset> {
  const res = await fetch(`${API_BASE_URL}/api/v1/assets/${id}/ip`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const error = await res.json();
    throw new ApiError(res.status, error.error || 'Failed to update ip')
  }
  return res.json();
}

export async function updateCredentials(id: string, data: UpdateCredentialsRequest): Promise<Asset> {
  const res = await fetch(`${API_BASE_URL}/api/v1/assets/${id}/credentials`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const error = await res.json();
    throw new ApiError(res.status, error.error || 'Failed to update credentials')
  }
  return res.json();
}

export async function deleteAsset(id: string): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/api/v1/assets/${id}`, {
    method: 'DELETE',
  });
  if (!res.ok) {
    const error = await res.json();
    throw new ApiError(res.status, error.error || 'Failed to delete asset')
  }
}