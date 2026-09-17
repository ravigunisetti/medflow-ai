import React, { useEffect, useState } from 'react';
import { X, AlertCircle, TrendingUp, Package, MapPin, Users, Calendar, ArrowRight } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { Phc, InventoryItem, FootfallRecord } from '../types';
import { apiService } from '../services/api';

interface PhcDetailModalProps {
  phc: Phc | null;
  onClose: () => void;
  onRequestTransfer?: (sourcePhc: Phc, item: InventoryItem) => void;
}

export const PhcDetailModal: React.FC<PhcDetailModalProps> = ({ phc, onClose }) => {
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [footfall, setFootfall] = useState<FootfallRecord[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!phc) return;
    setLoading(true);

    Promise.all([
      apiService.getInventoryByPhc(phc.id),
      apiService.getFootfallHistory(phc.id, 30),
    ])
      .then(([invData, ffData]) => {
        setInventory(invData);
        setFootfall(ffData);
      })
      .catch((err) => console.error('Failed to load PHC details', err))
      .finally(() => setLoading(false));
  }, [phc]);

  if (!phc) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#111827] border border-gray-800 rounded-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col shadow-2xl">
        {/* Modal Header */}
        <div className="p-6 border-b border-gray-800 flex items-start justify-between bg-gray-900/50">
          <div>
            <div className="flex items-center space-x-3">
              <h2 className="text-xl font-bold text-white tracking-tight">{phc.name}</h2>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 font-medium">
                Facility ID #{phc.id}
              </span>
            </div>
            <div className="flex items-center space-x-4 mt-2 text-xs text-gray-400">
              <span className="flex items-center space-x-1">
                <MapPin className="w-3.5 h-3.5 text-gray-500" />
                <span>{phc.district}, {phc.state}</span>
              </span>
              <span className="flex items-center space-x-1">
                <Users className="w-3.5 h-3.5 text-gray-500" />
                <span>Pop: {phc.populationServed.toLocaleString()}</span>
              </span>
              <span>Coords: {phc.latitude}, {phc.longitude}</span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg bg-gray-800 text-gray-400 hover:text-white hover:bg-gray-700 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {/* Historical Footfall Chart */}
          <div className="bg-gray-900/60 border border-gray-800/80 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <TrendingUp className="w-4 h-4 text-blue-400" />
                <h3 className="text-sm font-semibold text-gray-200">Patient OPD Footfall (Last 30 Days)</h3>
              </div>
              <span className="text-xs text-gray-400">Aggregated daily patient headcount</span>
            </div>

            <div className="h-44 w-full">
              {footfall.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={footfall}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1F2937" />
                    <XAxis dataKey="recordDate" tick={{ fontSize: 10, fill: '#9CA3AF' }} />
                    <YAxis tick={{ fontSize: 10, fill: '#9CA3AF' }} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#111827', borderColor: '#374151', fontSize: '12px' }}
                    />
                    <Line
                      type="monotone"
                      dataKey="patientCount"
                      stroke="#3B82F6"
                      strokeWidth={2}
                      dot={false}
                      name="Patients"
                    />
                    <Line
                      type="monotone"
                      dataKey="emergencyCount"
                      stroke="#EF4444"
                      strokeWidth={1.5}
                      dot={false}
                      name="Emergency"
                    />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-full flex items-center justify-center text-xs text-gray-500">
                  {loading ? 'Loading historical trends...' : 'No footfall data logged'}
                </div>
              )}
            </div>
          </div>

          {/* Current Stock Balances Table */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <Package className="w-4 h-4 text-indigo-400" />
                <h3 className="text-sm font-semibold text-gray-200">Essential Medicine Stock Balances</h3>
              </div>
              <span className="text-xs text-gray-400">Total items: {inventory.length}</span>
            </div>

            <div className="border border-gray-800 rounded-xl overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-900 text-gray-400 font-semibold border-b border-gray-800">
                  <tr>
                    <th className="p-3">Medicine</th>
                    <th className="p-3">Category</th>
                    <th className="p-3 text-right">In Stock</th>
                    <th className="p-3 text-right">Safety Stock</th>
                    <th className="p-3">Batch & Expiry</th>
                    <th className="p-3 text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800">
                  {inventory.map((item) => {
                    const isDeficit = item.quantity < item.safetyStock;
                    const isCritical = item.quantity < item.safetyStock * 0.35;

                    return (
                      <tr key={item.id} className="hover:bg-gray-800/40 transition">
                        <td className="p-3">
                          <div className="font-medium text-white">{item.medicineName}</div>
                          <div className="text-[10px] text-gray-500">{item.medicineCode}</div>
                        </td>
                        <td className="p-3 text-gray-400">{item.category}</td>
                        <td className="p-3 text-right font-mono font-semibold text-white">
                          {item.quantity.toLocaleString()} {item.unit}
                        </td>
                        <td className="p-3 text-right font-mono text-gray-400">
                          {item.safetyStock.toLocaleString()}
                        </td>
                        <td className="p-3">
                          <div className="text-gray-300 font-mono text-[11px]">{item.batchNumber}</div>
                          <div className="text-gray-500 text-[10px]">Exp: {item.expiryDate}</div>
                        </td>
                        <td className="p-3 text-center">
                          {isCritical ? (
                            <span className="px-2 py-0.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 font-bold text-[10px]">
                              CRITICAL
                            </span>
                          ) : isDeficit ? (
                            <span className="px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 font-medium text-[10px]">
                              DEFICIT
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-medium text-[10px]">
                              ADEQUATE
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-gray-800 bg-gray-900/50 flex justify-end space-x-3">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-xs font-medium transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
