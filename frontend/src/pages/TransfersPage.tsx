import React, { useEffect, useState } from 'react';
import { ArrowLeftRight, Check, X, Truck, Plus, CheckCircle2, Clock, AlertCircle } from 'lucide-react';
import { Transfer, Phc, Medicine } from '../types';
import { apiService } from '../services/api';

export const TransfersPage: React.FC = () => {
  const [transfers, setTransfers] = useState<Transfer[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [loading, setLoading] = useState<boolean>(true);

  // New transfer modal state
  const [showModal, setShowModal] = useState<boolean>(false);
  const [phcs, setPhcs] = useState<Phc[]>([]);
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [sourceId, setSourceId] = useState<number>(16); // default surplus PHC
  const [destId, setDestId] = useState<number>(1); // default critical PHC
  const [medId, setMedId] = useState<number>(5); // Paracetamol
  const [quantity, setQuantity] = useState<number>(500);

  const loadData = () => {
    setLoading(true);
    apiService
      .getTransfers(statusFilter || undefined, page, 20)
      .then((data) => {
        setTransfers(data.content);
        setTotalPages(data.totalPages);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, [statusFilter, page]);

  useEffect(() => {
    Promise.all([apiService.getAllActivePhcs(), apiService.getAllMedicines()]).then(([p, m]) => {
      setPhcs(p);
      setMedicines(m);
    });
  }, []);

  const handleStatusUpdate = async (id: number, status: string) => {
    try {
      await apiService.updateTransferStatus(id, status);
      loadData();
    } catch (err) {
      console.error('Failed to update transfer status', err);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiService.createTransfer({
        sourcePhcId: sourceId,
        destinationPhcId: destId,
        medicineId: medId,
        quantity,
        recommendationReason: 'Manual redistribution initiated by District Health Officer',
      });
      setShowModal(false);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create transfer');
    }
  };

  const getStatusBadge = (status: Transfer['status']) => {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'bg-amber-500/15 text-amber-400 border-amber-500/30';
      case 'APPROVED':
        return 'bg-blue-500/15 text-blue-400 border-blue-500/30';
      case 'IN_TRANSIT':
        return 'bg-purple-500/15 text-purple-400 border-purple-500/30';
      case 'COMPLETED':
        return 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30';
      case 'REJECTED':
      default:
        return 'bg-rose-500/15 text-rose-400 border-rose-500/30';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Inter-PHC Stock Redistribution</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            Deterministic optimization solver and human-in-the-loop stock transfer governance
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="bg-gray-900 border border-gray-800 rounded-lg px-3 py-1.5 text-xs text-gray-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Statuses</option>
            <option value="PENDING_APPROVAL">Pending Review</option>
            <option value="APPROVED">Approved / Reserved</option>
            <option value="COMPLETED">Delivered & Restocked</option>
            <option value="REJECTED">Rejected</option>
          </select>

          <button
            onClick={() => setShowModal(true)}
            className="px-3.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium flex items-center space-x-1.5 transition"
          >
            <Plus className="w-4 h-4" />
            <span>New Transfer Proposal</span>
          </button>
        </div>
      </div>

      {/* Transfers Table */}
      <div className="bg-[#111827] border border-gray-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-900 text-gray-400 font-semibold border-b border-gray-800">
              <tr>
                <th className="p-3">ID</th>
                <th className="p-3">Donor Facility (Source)</th>
                <th className="p-3">Deficit Facility (Destination)</th>
                <th className="p-3">Medicine & Qty</th>
                <th className="p-3 text-right">Distance</th>
                <th className="p-3 text-right">Est. Transit Cost</th>
                <th className="p-3 text-center">Status</th>
                <th className="p-3 text-right">Officer Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {transfers.length > 0 ? (
                transfers.map((t) => (
                  <tr key={t.id} className="hover:bg-gray-800/40 transition">
                    <td className="p-3 font-mono text-gray-500">#{t.id}</td>
                    <td className="p-3">
                      <div className="font-semibold text-emerald-400">{t.sourcePhcName}</div>
                      <div className="text-[10px] text-gray-500">{t.sourceDistrict}</div>
                    </td>
                    <td className="p-3">
                      <div className="font-semibold text-red-400">{t.destinationPhcName}</div>
                      <div className="text-[10px] text-gray-500">{t.destinationDistrict}</div>
                    </td>
                    <td className="p-3">
                      <div className="font-medium text-white">{t.medicineName}</div>
                      <div className="text-xs font-bold text-blue-400 font-mono">
                        {t.quantity.toLocaleString()} {t.medicineUnit}
                      </div>
                    </td>
                    <td className="p-3 text-right font-mono text-gray-300 font-medium">{t.distanceKm} km</td>
                    <td className="p-3 text-right font-mono text-gray-300 font-bold">₹{t.estimatedCost.toFixed(2)}</td>
                    <td className="p-3 text-center">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border ${getStatusBadge(t.status)}`}
                      >
                        {t.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="p-3 text-right">
                      {t.status === 'PENDING_APPROVAL' && (
                        <div className="flex items-center justify-end space-x-1.5">
                          <button
                            onClick={() => handleStatusUpdate(t.id, 'APPROVED')}
                            className="p-1 rounded bg-blue-600/20 hover:bg-blue-600/40 text-blue-400 border border-blue-500/30 transition"
                            title="Approve & Reserve Stock"
                          >
                            <Check className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleStatusUpdate(t.id, 'REJECTED')}
                            className="p-1 rounded bg-rose-600/20 hover:bg-rose-600/40 text-rose-400 border border-rose-500/30 transition"
                            title="Reject Proposal"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      )}

                      {t.status === 'APPROVED' && (
                        <button
                          onClick={() => handleStatusUpdate(t.id, 'COMPLETED')}
                          className="px-2.5 py-1 rounded bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-[11px] transition"
                        >
                          Mark Delivered
                        </button>
                      )}

                      {t.status === 'COMPLETED' && (
                        <span className="text-[10px] text-emerald-400 font-semibold flex items-center justify-end space-x-1">
                          <CheckCircle2 className="w-3 h-3" />
                          <span>Restocked</span>
                        </span>
                      )}

                      {t.status === 'REJECTED' && (
                        <span className="text-[10px] text-rose-400 font-medium">Declined</span>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-gray-500">
                    {loading ? 'Loading transfers...' : 'No transfer records found'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
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

      {/* New Transfer Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <form onSubmit={handleCreate} className="bg-[#111827] border border-gray-800 rounded-xl p-6 w-full max-w-lg shadow-2xl space-y-4">
            <h3 className="font-bold text-white text-base">Propose Inter-PHC Stock Transfer</h3>
            <p className="text-xs text-gray-400">
              Deterministic constraints check that source PHC maintains mandatory safety buffer stock.
            </p>

            <div>
              <label className="block text-xs text-gray-400 mb-1">Donor Facility (Source with Surplus)</label>
              <select
                value={sourceId}
                onChange={(e) => setSourceId(parseInt(e.target.value))}
                className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-white text-xs focus:outline-none focus:border-blue-500"
              >
                {phcs.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.district})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs text-gray-400 mb-1">Deficit Facility (Destination Shortage)</label>
              <select
                value={destId}
                onChange={(e) => setDestId(parseInt(e.target.value))}
                className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-white text-xs focus:outline-none focus:border-blue-500"
              >
                {phcs.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.district})
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-gray-400 mb-1">Medicine Catalog</label>
                <select
                  value={medId}
                  onChange={(e) => setMedId(parseInt(e.target.value))}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-white text-xs focus:outline-none focus:border-blue-500"
                >
                  {medicines.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-gray-400 mb-1">Transfer Units</label>
                <input
                  type="number"
                  min="1"
                  value={quantity}
                  onChange={(e) => setQuantity(parseInt(e.target.value) || 1)}
                  className="w-full bg-gray-900 border border-gray-700 rounded-lg px-3 py-2 text-white font-mono text-xs focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>

            <div className="flex justify-end space-x-3 pt-2">
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="px-4 py-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-xs font-medium transition"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium transition"
              >
                Submit Proposal
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
