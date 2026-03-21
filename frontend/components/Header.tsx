'use client';

interface HeaderProps {
  assetCount: number;
  isConnected: boolean;
}

export default function Header({ assetCount, isConnected }: HeaderProps) {
  return (
    <header className="flex items-center justify-between px-6 py-4 border-b border-[#1a2535]">

      {/* Logo + título */}
      <div className="flex items-center gap-3">
        <span className="text-[#10b981] font-mono text-xl font-bold">▶</span>
        <span className="text-[#e4e8ee] font-semibold text-lg tracking-wide">
          INFRATRACK
        </span>
      </div>

      {/* Centro: contador de activos */}
      <div className="text-[#4a5568] text-sm font-mono">
        {assetCount} {assetCount === 1 ? 'asset' : 'assets'} monitored
      </div>

      {/* Derecha: indicador de conexión */}
      <div className="flex items-center gap-2">
        <span
          className={`w-2 h-2 rounded-full ${
            isConnected ? 'bg-[#10b981]' : 'bg-[#ef4444]'
          }`}
        />
        <span className="text-sm font-mono text-[#4a5568]">
          {isConnected ? 'Connected' : 'Connection lost'}
        </span>
      </div>

    </header>
  );
}
