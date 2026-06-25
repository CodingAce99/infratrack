/**
 * Domain models mirroring the Infratrack REST API contract.
 * The backend API remains unchanged; these types document that contract in the frontend.
 */

export type AssetType = 'SERVER' | 'ROUTER' | 'IOT_DEVICE';
export type AssetStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
export type UserRole = 'ADMIN' | 'VIEWER';

export interface Asset {
  id: string;
  name: string;
  type: AssetType;
  ipAddress: string;
  status: AssetStatus;
  username: string;
}

export interface MetricSnapshot {
  assetId: string;
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  collectedAt: string;
}

export interface CreateAssetRequest {
  name: string;
  type: AssetType;
  ipAddress: string;
  username: string;
  password: string;
}

export interface UpdateStatusRequest {
  status: AssetStatus;
}

export interface UpdateIpRequest {
  ipAddress: string;
}

export interface UpdateCredentialsRequest {
  username: string;
  password: string;
}

/** Asset type options for form selects. */
export const ASSET_TYPES: readonly AssetType[] = ['SERVER', 'ROUTER', 'IOT_DEVICE'];

/** Asset status options for form selects. */
export const ASSET_STATUSES: readonly AssetStatus[] = ['ACTIVE', 'INACTIVE', 'MAINTENANCE'];