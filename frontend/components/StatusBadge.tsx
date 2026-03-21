'use client'

import { Asset } from "@/lib/types";

interface StatusBadgeProps {
    assetStatus: Asset['status'];
}

export default function StatusBadge ({ assetStatus }: StatusBadgeProps) {
    
    const config= {
        ACTIVE: { classes: 'bg-[#10b981]/10 text-[#10b981]', label: 'Active'},
        MAINTENANCE: { classes: 'bg-[#f59e0b]/10 text-[#f59e0b]', label: 'Maintenance'},
        INACTIVE: { classes: 'bg-[#6b7280]/10 text-[#6b7280]', label: 'Inactive'}

    }

    const { classes, label} = config[assetStatus];

    return (
    <span className={`text-xs font-mono font-medium px-2 py-0.5 rounded-full ${classes}`}>
        ● {label}
    </span>
    );
}