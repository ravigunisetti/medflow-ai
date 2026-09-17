import React, { useEffect, useState } from 'react';
import {
  Building2,
  AlertTriangle,
  Flame,
  ArrowLeftRight,
  ShieldAlert,
  Activity,
  ChevronRight,
  CheckCircle2,
} from 'lucide-react';
import { KpiCard } from '../components/KpiCard';
import { PhcMap } from '../components/PhcMap';
import { PhcDetailModal } from '../components/PhcDetailModal';
import { DashboardSummary, Phc, Alert, RiskLevel } from '../types';
import { apiService } from '../services/api';

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [phcs, setPhcs] = useState<Phc[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [selectedPhc, setSelectedPhc] = useState<Phc | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const loadData = () => {
    setLoading(true);
    Promise.all([
      apiService.getDashboardSummary(),
      apiService.getAllActivePhcs(),
      apiService.getActiveAlerts(),
    ])
      .then(([summaryData, phcsData, alertsData]) => {
        setSummary(summaryData);
        setPhcs(phcsData);
        setAlerts(alertsData);
      })
      .catch((err) => console.error('Failed to load dashboard data', err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  // Map of PHC ID -> Risk level
  const riskMap: Record<number, RiskLevel> = {};
  alerts.forEach((alert) => {
    if (!riskMap[alert.phcId] || alert.riskLevel === 'CRITICAL') {
      riskMap[alert.phcId] = alert.riskLevel;
    }
  });

  return (
    <div className="space-y-6">
      {/* Top Section: Quick Emergency Banner if Criticals Exist */}
      {summary && summary.criticalStockouts > 0 && (
        <div className="p-4 rounded-xl bg-red-950/30 border border-red-500/30 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-red-500/20 text-red-400">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-semibold text-white">
                Urgent Attention Required: {summary.criticalStockouts} Medicine Stock-Outs Detected
              </div>
              <div className="text-xs text-red-300/80">
                Facilities in Pune and Nashik clusters are running critically below safety thresholds. Redistribution solver recommends inter-facility transfer.
              </div>
            </div>
          </div>
          <button
            onClick={() => window.location.assign('#/transfers')}
            className="px-3.5 py-1.5 rounded-lg bg-red-600 hover:bg-red-500 text-white text-xs font-semibold shadow-md transition flex items-center space-x-1"
          >
            <span>Review Redistribution</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          title="Monitored PHCs"
          value={summary ? summary.totalPhcsMonitored : 100}
          subtitle="Active network nodes in Maharashtra"
          icon={Building2}
          color="blue"
        />
        <KpiCard
          title="Critical Stock-outs"
          value={summary ? summary.criticalStockouts : 15}
          subtitle="Facilities with < 3 days stock"
          icon={AlertTriangle}
          color="red"
          trend="+3 this week"
          trendType="critical"
        />
        <KpiCard
          title="High-Risk Vulnerabilities"
          value={summary ? summary.highRiskPredictions : 24}
          subtitle="Projected shortage within 7 days"
          icon={Flame}
          color="amber"
          trend="Action required"
          trendType="negative"
        />
        <KpiCard
          title="Active Transfers"
          value={summary ? summary.pendingTransfers : 8}
          subtitle="Redistribution proposals pending approval"
          icon={ArrowLeftRight}
          color="purple"
          trend="Deterministic solver active"
          trendType="positive"
        />
      </div>

      {/* Main Command Center: Interactive Geospatial Map & District Summaries */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Geospatial Map View (Spans 2 columns) */}
        <div className="lg:col-span-2 bg-[#111827] border border-gray-800 rounded-xl p-4 flex flex-col h-[520px]">
          <div className="flex items-center justify-between mb-3 px-1">
            <div className="flex items-center space-x-2">
              <Activity className="w-4 h-4 text-blue-400" />
              <h2 className="text-sm font-semibold text-white">Geospatial Network Telemetry (100 PHCs)</h2>
            </div>
            <span className="text-xs text-gray-400">Click any facility node for live stock breakdown</span>
          </div>

          <div className="flex-1 w-full min-h-0">
            <PhcMap
              phcs={phcs}
              riskMap={riskMap}
              onSelectPhc={(phc) => setSelectedPhc(phc)}
              selectedPhcId={selectedPhc?.id}
            />
          </div>
        </div>

        {/* District Risk Table */}
        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4 flex flex-col h-[520px]">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-white">District Vulnerability Index</h2>
            <span className="text-[11px] text-gray-400">5 Districts</span>
          </div>

          <div className="overflow-y-auto space-y-2.5 flex-1 pr-1">
            {summary?.districtSummaries.map((dist) => {
              const isCrit = dist.criticalPhcs > 0;
              return (
                <div
                  key={dist.district}
                  className="p-3 rounded-lg bg-gray-900/70 border border-gray-800 hover:border-gray-700 transition"
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-semibold text-xs text-white">{dist.district}</span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        isCrit
                          ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                          : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      }`}
                    >
                      {dist.overallStatus}
                    </span>
                  </div>
                  <div className="grid grid-cols-3 gap-2 mt-2 pt-2 border-t border-gray-800 text-[11px]">
                    <div>
                      <div className="text-gray-500">Facilities</div>
                      <div className="font-semibold text-gray-200">{dist.totalPhcs}</div>
                    </div>
                    <div>
                      <div className="text-gray-500">Critical</div>
                      <div className={`font-semibold ${isCrit ? 'text-red-400' : 'text-gray-300'}`}>
                        {dist.criticalPhcs}
                      </div>
                    </div>
                    <div>
                      <div className="text-gray-500">Avg Stock</div>
                      <div className="font-semibold text-gray-200">{dist.averageDaysRemaining}d</div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Critical Stock-Out Alerts Table */}
      <div className="bg-[#111827] border border-gray-800 rounded-xl p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 text-red-400" />
            <h2 className="text-sm font-semibold text-white">Active Critical Alerts & Immediate Action Required</h2>
          </div>
          <span className="text-xs text-gray-400">Total active warnings: {alerts.length}</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-900 text-gray-400 font-semibold border-b border-gray-800">
              <tr>
                <th className="p-3">PHC Facility</th>
                <th className="p-3">District</th>
                <th className="p-3">Medicine</th>
                <th className="p-3 text-right">Stock</th>
                <th className="p-3 text-right">Daily Demand</th>
                <th className="p-3 text-right">Days Remaining</th>
                <th className="p-3 text-center">Severity</th>
                <th className="p-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-800">
              {alerts.slice(0, 8).map((a, idx) => (
                <tr key={idx} className="hover:bg-gray-800/40 transition">
                  <td className="p-3 font-medium text-white">{a.phcName}</td>
                  <td className="p-3 text-gray-400">{a.district}</td>
                  <td className="p-3 text-gray-300 font-medium">{a.medicineName}</td>
                  <td className="p-3 text-right font-mono font-bold text-white">{a.currentStock}</td>
                  <td className="p-3 text-right font-mono text-gray-400">{a.dailyDemand.toFixed(1)}</td>
                  <td className="p-3 text-right font-mono font-bold text-red-400">{a.daysRemaining.toFixed(1)} days</td>
                  <td className="p-3 text-center">
                    <span className="px-2 py-0.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 font-bold text-[10px]">
                      {a.riskLevel}
                    </span>
                  </td>
                  <td className="p-3 text-right">
                    <button
                      onClick={() => {
                        const target = phcs.find((p) => p.id === a.phcId);
                        if (target) setSelectedPhc(target);
                      }}
                      className="px-2.5 py-1 rounded bg-gray-800 hover:bg-gray-700 text-blue-400 font-medium text-[11px] transition"
                    >
                      Inspect
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Drilldown Slide-Over Modal */}
      <PhcDetailModal
        phc={selectedPhc}
        onClose={() => setSelectedPhc(null)}
      />
    </div>
  );
};
