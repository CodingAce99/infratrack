export interface Asset {
    id: string;
    name: string;
    type: 'SERVER' | 'ROUTER' | 'IOT_DEVICE';
    ipAddress: string;
    status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
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
    type: 'SERVER' | 'ROUTER' | 'IOT_DEVICE';
    ipAddress: string;
    username: string;
    password: string;
}

export interface UpdateStatusRequest {
    status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
}

export interface UpdateIpRequest {
    ipAddress: string;
}

export interface UpdateCredentialsRequest {
    username: string;
    password: string;
}