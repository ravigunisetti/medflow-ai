import React, { useEffect, useState } from 'react';
import { AlertOctagon, RefreshCw, Filter, CheckCircle2, Flame, ShieldAlert } from 'lucide-react';
import { Prediction, RiskLevel } from '../types';
import { apiService } from '../services/api';

export const PredictionsPage: React.FC = () => {
  const [predictions, setPredictions] = useState<Prediction[]>([]);
  const [riskFilter, setRiskFilter] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [evaluating, setEvaluating] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  const loadData = () => {
    setLoading(true);
    apiService
      .getPredictions(riskFilter || undefined, page, 25)
      .then((data) => {
        setPredictions(data.content);
        setTotalPages(data.totalPages);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, [riskFilter, page]);

  const handleEvaluateAll = async () => {
    setEvaluating(true);
    try {
      await apiService.evaluateAllRisks();
      loadData();
    } catch (err) {
      console.error('Failed to run network evaluation', err);
    } finally {
      setEvaluating(false);
    }
  };

  const getRiskBadge = (level: RiskLevel) => {
    switch (level) {
      case 'CRITICAL':
        return 'bg-red-500/15 text-red-400 border border-red-500/30';
      case 'HIGH':
        return 'bg-orange-500/15 text-orange-400 border border-orange-500/30';
      case 'MEDIUM':
        return 'bg-amber-500/15 text-amber-400 border border-amber-500/30';
      case 'LOW':
      default:
        return 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Demand Forecasts & Stock-Out Engine</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            Deterministic risk engine computing days-to-stockout (Stock / Average Daily Demand) across all nodes
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <select
            value={riskFilter}
            onChange={(e) => {
              setRiskFilter(e.target.value);
              setPage(0);
            }}
            className="bg-gray-900 border border-gray-800 rounded-lg px-3 py-1.5 text-xs text-gray-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Risk Levels</option>
            <option value="CRITICAL">Critical (&lt; 3 days)</option>
            <option value="HIGH">High (3–7 days)</option>
            <option value="MEDIUM">Medium (7–14 days)</option>
            <option value="LOW">Low (&gt; 14 days)</option>
          </select>

          <button
            onClick={handleEvaluateAll}
            disabled={evaluating}
            className="px-3.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium flex items-center space-x-2 transition disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${evaluating ? 'animate-spin' : ''}`} />
            <span>{evaluating ? 'Evaluating Network...' : 'Recalculate All Risks'}</span>
          </button>
        </div>
      </div>

      {/* Predictions Table */}
      <div className="bg-[#111827] border border-gray-800 rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-900 text-gray-400 font-semibold border-b border-gray-800">
              <tr>
                <th className="p-3">PHC Facility</th>
                <th className="p-3">District</th>
                <th className="p-3">Medicine</th>
                <th className="p-3 text-right">Projected Daily Demand</th>
                <th className="p-3 text-right">Days to Stock-Out</th>
                <th className="p-3 text-center">Risk Classification</th>
                <th className="p-3 text-right">Model Engine</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {predictions.map((p) => (
                <tr key={p.id} className="hover:bg-gray-800/40 transition">
                  <td className="p-3 font-medium text-white">{p.phcName}</td>
                  <td className="p-3 text-gray-400">{p.district}</td>
                  <td className="p-3">
                    <span className="font-semibold text-gray-200">{p.medicineName}</span>
                    <span className="text-[10px] text-gray-500 block font-mono">{p.medicineCode}</span>
                  </td>
                  <td className="p-3 text-right font-mono text-gray-300 font-medium">
                    {p.predictedDailyDemand.toFixed(1)} units/day
                  </td>
                  <td className="p-3 text-right font-mono font-bold">
                    <span
                      className={
                        p.riskLevel === 'CRITICAL'
                          ? 'text-red-400'
                          : p.riskLevel === 'HIGH'
                          ? 'text-orange-400'
                          : p.riskLevel === 'MEDIUM'
                          ? 'text-amber-400'
                          : 'text-emerald-400'
                      }
                    >
                      {p.predictedDaysToStockout.toFixed(1)} days
                    </span>
                  </td>
                  <td className="p-3 text-center">
                    <span className={`px-2.5 py-0.5 rounded text-[10px] font-bold ${getRiskBadge(p.riskLevel)}`}>
                      {p.riskLevel}
                    </span>
                  </td>
                  <td className="p-3 text-right">
                    <span className="text-[10px] font-mono text-gray-400 bg-gray-900 px-2 py-0.5 rounded border border-gray-800">
                      {p.modelVersion}
                    </span>
                  </td>
                </tr>
              ))}
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
    </div>
  );
};
