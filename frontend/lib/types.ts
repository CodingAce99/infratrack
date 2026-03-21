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
