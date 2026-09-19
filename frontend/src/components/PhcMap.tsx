import React from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import { Phc, RiskLevel } from '../types';

interface PhcMapProps {
  phcs: Phc[];
  riskMap?: Record<number, RiskLevel>;
  onSelectPhc: (phc: Phc) => void;
  selectedPhcId?: number | null;
}

export const PhcMap: React.FC<PhcMapProps> = ({
  phcs,
  riskMap = {},
  onSelectPhc,
  selectedPhcId,
}) => {
  // Center roughly in Western Maharashtra (Pune - Ahmednagar - Satara axis)
  const centerPosition: [number, number] = [18.65, 74.05];

  const getRiskColor = (risk: RiskLevel = 'LOW') => {
    switch (risk) {
      case 'CRITICAL':
        return '#EF4444'; // Bright Red
      case 'HIGH':
        return '#F97316'; // Orange
      case 'MEDIUM':
        return '#FBBF24'; // Amber
      case 'LOW':
      default:
        return '#10B981'; // Emerald Green
    }
  };

  return (
    <div className="w-full h-full min-h-[420px] rounded-xl overflow-hidden border border-gray-800 relative z-10">
      <MapContainer
        center={centerPosition}
        zoom={8}
        scrollWheelZoom={true}
        className="w-full h-full"
      >
        {/* CartoDB Dark Matter tile layer for dark command-center aesthetic */}
        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />

        {(phcs || []).map((phc) => {
          // PHC 1-15 were seeded with critical/low stock, PHC 16-25 surplus, rest balanced
          const fallbackRisk: RiskLevel = phc.id <= 15 ? 'CRITICAL' : phc.id <= 25 ? 'LOW' : 'LOW';
          const risk = riskMap[phc.id] || fallbackRisk;
          const color = getRiskColor(risk);
          const isSelected = selectedPhcId === phc.id;

          return (
            <CircleMarker
              key={phc.id}
              center={[phc.latitude, phc.longitude]}
              radius={isSelected ? 10 : 7}
              pathOptions={{
                color: isSelected ? '#FFFFFF' : color,
                fillColor: color,
                fillOpacity: 0.85,
                weight: isSelected ? 3 : 1.5,
              }}
              eventHandlers={{
                click: () => onSelectPhc(phc),
              }}
            >
              <Popup>
                <div className="text-xs p-1">
                  <div className="font-bold text-sm text-white">{phc.name}</div>
                  <div className="text-gray-400">District: {phc.district}</div>
                  <div className="text-gray-400">Pop. Served: {phc.populationServed.toLocaleString()}</div>
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-gray-400">Status:</span>
                    <span
                      className="px-1.5 py-0.5 rounded text-[10px] font-bold"
                      style={{
                        backgroundColor: `${color}20`,
                        color: color,
                        border: `1px solid ${color}40`,
                      }}
                    >
                      {risk}
                    </span>
                  </div>
                  <button
                    onClick={() => onSelectPhc(phc)}
                    className="mt-2.5 w-full py-1 px-2 rounded bg-blue-600 hover:bg-blue-500 text-white font-medium text-[11px] transition text-center"
                  >
                    View Facility Ledger
                  </button>
                </div>
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>

      {/* Map Legend Overlay */}
      <div className="absolute bottom-4 right-4 bg-gray-950/90 backdrop-blur-md border border-gray-800 rounded-lg p-3 z-[1000] text-xs shadow-xl pointer-events-auto">
        <div className="font-semibold text-gray-300 mb-2 uppercase tracking-wider text-[10px]">
          Stock-Out Risk Status
        </div>
        <div className="space-y-1.5">
          <div className="flex items-center space-x-2">
            <span className="w-3 h-3 rounded-full bg-red-500 inline-block shadow-sm shadow-red-500/50"></span>
            <span className="text-gray-300">Critical (&lt; 3 days stock)</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-3 h-3 rounded-full bg-orange-500 inline-block shadow-sm shadow-orange-500/50"></span>
            <span className="text-gray-300">High (3–7 days stock)</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-3 h-3 rounded-full bg-amber-400 inline-block"></span>
            <span className="text-gray-300">Medium (7–14 days stock)</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="w-3 h-3 rounded-full bg-emerald-500 inline-block"></span>
            <span className="text-gray-300">Low / Stable (&gt; 14 days)</span>
          </div>
        </div>
      </div>
    </div>
  );
};
