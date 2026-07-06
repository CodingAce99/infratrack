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

/** Credentials submitted to the login API. */
export interface LoginRequest {
  username: string;
  password: string;
}

/** Authenticated principal exposed to the frontend. */
export interface AuthUser {
  username: string;
  role: UserRole;
}

/**
 * Login API response contract.
 * `type` is always `'Bearer'`; it is kept on the payload to mirror the backend
 * `AuthenticationResult` and the `Authorization` header value prefix.
 */
export interface LoginResponse {
  token: string;
  type: 'Bearer';
  username: string;
  role: UserRole;
}

/** Asset type options for form selects. */
export const ASSET_TYPES: readonly AssetType[] = ['SERVER', 'ROUTER', 'IOT_DEVICE'];

/** Asset status options for form selects. */
export const ASSET_STATUSES: readonly AssetStatus[] = ['ACTIVE', 'INACTIVE', 'MAINTENANCE'];