import React, { useEffect, useState } from 'react';
import { Package, Search, Edit3, CheckCircle2, AlertTriangle, Filter } from 'lucide-react';
import { InventoryItem } from '../types';
import { apiService } from '../services/api';

export const InventoryPage: React.FC = () => {
  const [inventories, setInventories] = useState<InventoryItem[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [editingItem, setEditingItem] = useState<InventoryItem | null>(null);
  const [editQty, setEditQty] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  const loadData = () => {
    setLoading(true);
    apiService
      .getInventories(undefined, undefined, page, 25)
      .then((data) => {
        setInventories(data.content);
        setTotalPages(data.totalPages);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, [page]);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingItem) return;

    try {
      await apiService.updateInventory(editingItem.id, editQty);
      setEditingItem(null);
      loadData();
    } catch (err) {
      console.error('Failed to update inventory', err);
    }
  };

  const filtered = inventories.filter(
    (i) =>
      i.medicineName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.phcName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      i.category.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Medicine Inventory Ledger</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            Real-time multi-facility stock ledger across 20 essential drug categories (2,000 active balances)
          </p>
        </div>

        <div className="relative">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search facility or drug..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="bg-gray-900 border border-gray-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-blue-500 w-64"
          />
        </div>
      </div>

      <div className="bg-[#111827] border border-gray-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-900 text-gray-400 font-semibold border-b border-gray-800">
              <tr>
                <th className="p-3">PHC Facility</th>
                <th className="p-3">Medicine & Code</th>
                <th className="p-3">Category</th>
                <th className="p-3 text-right">In Stock</th>
                <th className="p-3 text-right">Safety Stock</th>
                <th className="p-3 text-center">Buffer Ratio</th>
                <th className="p-3">Batch / Expiry</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {filtered.map((item) => {
                const ratio = item.safetyStock > 0 ? (item.quantity / item.safetyStock) * 100 : 100;
                const isCrit = item.quantity < item.safetyStock * 0.35;
                const isDef = item.quantity < item.safetyStock;

                return (
                  <tr key={item.id} className="hover:bg-gray-800/40 transition">
                    <td className="p-3">
                      <div className="font-medium text-white">{item.phcName}</div>
                      <div className="text-[10px] text-gray-500">{item.district}</div>
                    </td>
                    <td className="p-3">
                      <div className="font-semibold text-gray-200">{item.medicineName}</div>
                      <div className="text-[10px] text-gray-500 font-mono">{item.medicineCode}</div>
                    </td>
                    <td className="p-3 text-gray-400">{item.category}</td>
                    <td className="p-3 text-right font-mono font-bold text-white">
                      {item.quantity.toLocaleString()} {item.unit}
                    </td>
                    <td className="p-3 text-right font-mono text-gray-400">{item.safetyStock.toLocaleString()}</td>
                    <td className="p-3 text-center">
                      <div className="w-24 mx-auto">
                        <div className="flex justify-between text-[10px] mb-1 font-mono">
                          <span className={isCrit ? 'text-red-400 font-bold' : isDef ? 'text-amber-400' : 'text-emerald-400'}>
                            {Math.round(ratio)}%
                          </span>
                        </div>
                        <div className="h-1.5 w-full bg-gray-800 rounded-full overflow-hidden">
                          <div
                            className={`h-full ${
                              isCrit ? 'bg-red-500' : isDef ? 'bg-amber-500' : 'bg-emerald-500'
                            }`}
                            style={{ width: `${Math.min(100, Math.max(5, ratio))}%` }}
                          />
                        </div>
                      </div>
                    </td>
                    <td className="p-3">
                      <div className="text-gray-300 font-mono text-[11px]">{item.batchNumber}</div>
                      <div className="text-gray-500 text-[10px]">Exp: {item.expiryDate}</div>
                    </td>
                    <td className="p-3 text-right">
                      <button
                        onClick={() => {
                          setEditingItem(item);
                          setEditQty(item.quantity);
                        }}
                        className="p-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 hover:text-white transition"
                        title="Update Quantity"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="p-4 border-t border-gray-800 flex items-center justify-between text-xs text-gray-400">
          <span>Page {page + 1} of {totalPages}</span>
          <div className="space-x-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded bg-gray-900 border border-gray-800 disabled:opacity-40 hover:bg-gray-800 transition"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded bg-gray-900 border border-gray-800 disabled:opacity-40 hover:bg-gray-800 transition"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Edit Quantity Modal */}
      {editingItem && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleUpdate} className="bg-[#111827] border border-gray-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
            <h3 className="font-bold text-white text-base mb-1">Update Stock Count</h3>
            <p className="text-xs text-gray-400 mb-4">
              {editingItem.medicineName} at {editingItem.phcName}
            </p>

            <div className="space-y-3 mb-6">
              <div>
                <label className="block text-xs text-gray-400 mb-1">Physical On-Hand Quantity ({editingItem.unit})</label>
                <input
                  type="number"
                  min="0"
                  value={editQty}
                  onChange={(e) => setEditQty(parseInt(e.target.value) || 0)}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-white font-mono text-sm focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>

            <div className="flex justify-end space-x-3">
              <button
                type="button"
                onClick={() => setEditingItem(null)}
                className="px-4 py-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-xs font-medium transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium transition"
              >
                Save Changes
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
