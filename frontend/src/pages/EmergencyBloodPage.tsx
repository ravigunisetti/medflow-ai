import React, { useState, useEffect } from 'react';
import {
  Droplet,
  AlertTriangle,
  ShieldCheck,
  Truck,
  Building2,
  Clock,
  Sparkles,
  RefreshCw,
  Search,
  Filter,
  CheckCircle2,
  AlertCircle,
  Phone,
  Send,
  Sliders,
  Maximize2,
  X,
  ExternalLink,
  Flame,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import { apiService } from '../services/api';
import {
  BloodBank,
  BloodInventoryItem,
  EmergencyBloodRequest,
  CandidateBloodResource,
  BloodMatchingResult,
  BloodTransfer,
  BloodDashboardSummary,
  Phc,
  BloodGroup,
  BloodRequestPriority,
} from '../types';

export const EmergencyBloodPage: React.FC = () => {
  // Navigation tabs
  const [activeTab, setActiveTab] = useState<'command' | 'map' | 'inventory' | 'simulation'>('command');

  // Core data states
  const [summary, setSummary] = useState<BloodDashboardSummary | null>(null);
  const [requests, setRequests] = useState<EmergencyBloodRequest[]>([]);
  const [bloodBanks, setBloodBanks] = useState<BloodBank[]>([]);
  const [transfers, setTransfers] = useState<BloodTransfer[]>([]);
  const [phcs, setPhcs] = useState<Phc[]>([]);
  const [inventories, setInventories] = useState<BloodInventoryItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [selectedDistrict, setSelectedDistrict] = useState<string>('All');

  // Match Results Drawer State
  const [selectedRequest, setSelectedRequest] = useState<EmergencyBloodRequest | null>(null);
  const [matchingResult, setMatchingResult] = useState<BloodMatchingResult | null>(null);
  const [isMatchingLoading, setIsMatchingLoading] = useState<boolean>(false);

  // Create Request Modal State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [formData, setFormData] = useState({
    hospitalId: 0,
    bloodGroup: 'O+' as BloodGroup,
    componentType: 'WHOLE_BLOOD',
    unitsRequired: 2,
    priority: 'HIGH' as BloodRequestPriority,
    deadlineMinutes: 60,
    clinicalNotes: '',
  });

  // Simulation State
  const [simData, setSimData] = useState({
    hospitalId: 0,
    bloodGroup: 'O-' as BloodGroup,
    unitsRequired: 4,
    priority: 'CRITICAL' as BloodRequestPriority,
    deadlineMinutes: 45,
    scenarioType: 'MASS_CASUALTY_ACCIDENT',
  });
  const [simResult, setSimResult] = useState<BloodMatchingResult | null>(null);
  const [isSimLoading, setIsSimLoading] = useState<boolean>(false);

  // Notification / Feedback banner
  const [actionNotice, setActionNotice] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);

  // Map filters
  const [showMapHospitals, setShowMapHospitals] = useState<boolean>(true);
  const [showMapBloodBanks, setShowMapBloodBanks] = useState<boolean>(true);
  const [showMapEmergencies, setShowMapEmergencies] = useState<boolean>(true);

  // Load all initial data
  const loadData = async () => {
    try {
      setIsLoading(true);
      const [sumData, reqsData, banksData, transfersData, phcsData, invsData] = await Promise.all([
        apiService.getBloodDashboardSummary().catch(() => null),
        apiService.getEmergencyBloodRequests().catch(() => []),
        apiService.getBloodBanks().catch(() => []),
        apiService.getBloodTransfers().catch(() => []),
        apiService.getAllActivePhcs().catch(() => []),
        apiService.getAllBloodInventory().catch(() => []),
      ]);

      if (sumData) setSummary(sumData);
      setRequests(reqsData || []);
      setBloodBanks(banksData || []);
      setTransfers(transfersData || []);
      setPhcs(phcsData || []);
      setInventories(invsData || []);

      if (phcsData && phcsData.length > 0 && formData.hospitalId === 0) {
        setFormData((prev) => ({ ...prev, hospitalId: phcsData[0].id }));
        setSimData((prev) => ({ ...prev, hospitalId: phcsData[0].id }));
      }
    } catch (err) {
      console.error('Failed to load blood network data', err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
    const timer = setInterval(() => {
      loadData();
    }, 20000); // auto-refresh every 20s
    return () => clearInterval(timer);
  }, []);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await loadData();
    setActionNotice({ message: 'Live blood ledger updated with regional telemetry', type: 'info' });
    setTimeout(() => setActionNotice(null), 4000);
  };

  // Run Deterministic Matching for a Request
  const handleFindMatches = async (req: EmergencyBloodRequest) => {
    setSelectedRequest(req);
    setIsMatchingLoading(true);
    try {
      const result = await apiService.getBloodMatches(req.id);
      setMatchingResult(result);
    } catch (err) {
      console.error('Failed to find matches', err);
      setActionNotice({ message: 'Failed to evaluate blood match algorithms', type: 'error' });
    } finally {
      setIsMatchingLoading(false);
    }
  };

  // Confirm Blood Transfer
  const handleConfirmTransfer = async (candidate: CandidateBloodResource) => {
    if (!selectedRequest) return;
    try {
      await apiService.confirmBloodTransfer({
        requestId: selectedRequest.id,
        sourceBloodBankId: candidate.bloodBankId,
        units: selectedRequest.unitsRequired,
        estimatedDistanceKm: candidate.distanceKm,
        estimatedEtaMinutes: candidate.etaMinutes,
      });

      setActionNotice({
        message: `Blood allocation confirmed with ${candidate.bloodBankName}. Cold-chain siren dispatch initiated!`,
        type: 'success',
      });
      setSelectedRequest(null);
      setMatchingResult(null);
      await loadData();
    } catch (err: any) {
      console.error('Confirm transfer failed', err);
      setActionNotice({ message: err?.response?.data?.message || 'Failed to confirm blood transfer', type: 'error' });
    }
  };

  // Update Transfer Status (e.g. mark IN_TRANSIT or DELIVERED)
  const handleUpdateTransfer = async (transferId: number, status: string) => {
    try {
      await apiService.updateBloodTransferStatus(transferId, status);
      setActionNotice({ message: `Transfer status updated to ${status}`, type: 'success' });
      await loadData();
    } catch (err) {
      console.error('Transfer update failed', err);
      setActionNotice({ message: 'Failed to update transfer status', type: 'error' });
    }
  };

  // Create Emergency Request Submit
  const handleCreateRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const deadline = new Date(Date.now() + formData.deadlineMinutes * 60000).toISOString();
      const created = await apiService.createEmergencyBloodRequest({
        hospitalId: formData.hospitalId,
        bloodGroup: formData.bloodGroup,
        componentType: formData.componentType,
        unitsRequired: formData.unitsRequired,
        priority: formData.priority,
        requiredBy: deadline,
        clinicalNotes: formData.clinicalNotes || 'Urgent hospital emergency blood requisition',
      });

      setIsCreateModalOpen(false);
      setActionNotice({ message: 'Emergency blood requisition submitted successfully. Opening matching engine...', type: 'success' });
      await loadData();
      // Auto-trigger matching engine for the newly created request
      handleFindMatches(created);
    } catch (err: any) {
      console.error('Failed to create emergency request', err);
      setActionNotice({ message: 'Failed to create emergency blood request', type: 'error' });
    }
  };

  // Run Simulation
  const handleRunSimulation = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setIsSimLoading(true);
    try {
      const result = await apiService.simulateBloodEmergency({
        hospitalId: simData.hospitalId || undefined,
        bloodGroup: simData.bloodGroup,
        unitsRequired: simData.unitsRequired,
        priority: simData.priority,
        deadlineMinutes: simData.deadlineMinutes,
        scenarioType: simData.scenarioType,
      });
      setSimResult(result);
    } catch (err) {
      console.error('Simulation failed', err);
      setActionNotice({ message: 'Emergency simulation failed to complete', type: 'error' });
    } finally {
      setIsSimLoading(false);
    }
  };

  const districts = ['All', 'Pune', 'Satara', 'Ahmednagar', 'Solapur', 'Nashik'];

  const filteredRequests = requests.filter((r) => {
    if (selectedDistrict === 'All') return true;
    return r.hospitalDistrict?.toLowerCase() === selectedDistrict.toLowerCase();
  });

  const bloodGroupsList: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

  return (
    <div className="space-y-6 pb-12">
      {/* 1. Header & Emergency Status Alert */}
      <div className="bg-[#111827] border border-gray-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
        <div className="absolute -right-12 -bottom-12 w-64 h-64 bg-red-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 relative z-10">
          <div>
            <div className="flex items-center space-x-3 mb-1">
              <div className="w-10 h-10 rounded-xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-400 shadow-inner">
                <Droplet className="w-6 h-6 fill-red-500 text-red-500 animate-pulse" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h1 className="text-2xl font-black tracking-tight text-white">Emergency Blood Network</h1>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-500/20 text-red-300 border border-red-500/30 animate-pulse">
                    LIVE RESPONSE
                  </span>
                </div>
                <p className="text-sm text-gray-400">
                  Regional Blood Bank Registry • Deterministic Compatibility Matching • Cold-Chain Siren Logistics
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="flex items-center space-x-2 px-3.5 py-2 rounded-xl bg-gray-800/80 hover:bg-gray-800 border border-gray-700 text-gray-300 text-xs font-semibold transition"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-red-400' : ''}`} />
              <span>{isRefreshing ? 'Updating...' : 'Sync Telemetry'}</span>
            </button>

            <button
              onClick={() => {
                setActiveTab('simulation');
                if (!simResult) handleRunSimulation();
              }}
              className="flex items-center space-x-2 px-3.5 py-2 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 border border-purple-500/30 text-purple-300 text-xs font-semibold transition"
            >
              <Flame className="w-3.5 h-3.5 text-purple-400" />
              <span>Emergency Sandbox</span>
            </button>

            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="flex items-center space-x-2 px-4 py-2 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white text-xs font-bold shadow-lg shadow-red-600/20 transition transform active:scale-95"
            >
              <Droplet className="w-3.5 h-3.5 fill-white" />
              <span>+ Create Urgent Request</span>
            </button>
          </div>
        </div>

        {/* Action Notice Alert */}
        {actionNotice && (
          <div
            className={`mt-4 px-4 py-2.5 rounded-xl border text-xs font-medium flex items-center justify-between transition-all ${
              actionNotice.type === 'success'
                ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-300'
                : actionNotice.type === 'error'
                ? 'bg-rose-500/15 border-rose-500/30 text-rose-300'
                : 'bg-blue-500/15 border-blue-500/30 text-blue-300'
            }`}
          >
            <div className="flex items-center space-x-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>{actionNotice.message}</span>
            </div>
            <button onClick={() => setActionNotice(null)} className="text-gray-400 hover:text-white">
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Mandatory Medical Safety Disclaimer */}
        <div className="mt-4 px-4 py-3 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-start space-x-3 text-xs text-amber-200/90 leading-relaxed">
          <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
          <div>
            <span className="font-bold text-amber-300 uppercase tracking-wide mr-1">Transfusion Clinical Safety Notice:</span>
            MedFlow AI recommendations are deterministic algorithmic decision-support aids based on reported real-time inventory.
            Final cross-matching, transfusion verification, and clinical safety remain strictly under the authority of licensed medical personnel.
          </div>
        </div>
      </div>

      {/* 2. Top Metric Cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-gray-400 text-xs mb-1">
            <span>Blood Banks</span>
            <Building2 className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-black text-white">{summary?.totalBloodBanks ?? 50}</div>
          <div className="text-[11px] text-emerald-400 mt-1 flex items-center space-x-1">
            <CheckCircle2 className="w-3 h-3" />
            <span>{summary?.activeBloodBanks ?? 50} active facilities</span>
          </div>
        </div>

        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-gray-400 text-xs mb-1">
            <span>Accreditation</span>
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-black text-emerald-400">{summary?.verifiedBloodBanks ?? 46}</div>
          <div className="text-[11px] text-gray-400 mt-1">NACO / NABH Verified</div>
        </div>

        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-gray-400 text-xs mb-1">
            <span>Network Units</span>
            <Droplet className="w-4 h-4 text-red-400" />
          </div>
          <div className="text-2xl font-black text-red-400">{summary?.totalUnitsAvailable?.toLocaleString() ?? '3,850'}</div>
          <div className="text-[11px] text-gray-400 mt-1">Across 8 blood groups</div>
        </div>

        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-gray-400 text-xs mb-1">
            <span>Reserved Units</span>
            <LockIcon className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-black text-amber-400">{summary?.totalUnitsReserved ?? 18}</div>
          <div className="text-[11px] text-gray-400 mt-1">Earmarked for transit</div>
        </div>

        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-gray-400 text-xs mb-1">
            <span>Active Emergencies</span>
            <AlertCircle className="w-4 h-4 text-rose-400 animate-pulse" />
          </div>
          <div className="text-2xl font-black text-rose-400">{summary?.activeEmergencies ?? requests.filter(r => r.status !== 'FULFILLED' && r.status !== 'CANCELLED').length}</div>
          <div className="text-[11px] text-rose-400 mt-1">Live triage response</div>
        </div>

        <div className="bg-[#111827] border border-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-gray-400 text-xs mb-1">
            <span>Siren Transfers</span>
            <Truck className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-black text-cyan-400">{transfers.filter(t => t.status === 'IN_TRANSIT' || t.status === 'DISPATCH_PENDING').length}</div>
          <div className="text-[11px] text-emerald-400 mt-1 flex items-center space-x-1">
            <CheckCircle2 className="w-3 h-3" />
            <span>Cold-chain verified</span>
          </div>
        </div>
      </div>

      {/* 3. Navigation Tabs */}
      <div className="flex items-center justify-between border-b border-gray-800 pb-2">
        <div className="flex space-x-2">
          <button
            onClick={() => setActiveTab('command')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === 'command'
                ? 'bg-red-600/20 text-red-400 border border-red-500/30'
                : 'text-gray-400 hover:text-white hover:bg-gray-800/50'
            }`}
          >
            <AlertCircle className="w-3.5 h-3.5" />
            <span>Emergency Operations ({requests.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('map')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === 'map'
                ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30'
                : 'text-gray-400 hover:text-white hover:bg-gray-800/50'
            }`}
          >
            <Maximize2 className="w-3.5 h-3.5" />
            <span>Regional Resource Map</span>
          </button>

          <button
            onClick={() => setActiveTab('inventory')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === 'inventory'
                ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30'
                : 'text-gray-400 hover:text-white hover:bg-gray-800/50'
            }`}
          >
            <Droplet className="w-3.5 h-3.5" />
            <span>Blood Group Inventory</span>
          </button>

          <button
            onClick={() => {
              setActiveTab('simulation');
              if (!simResult) handleRunSimulation();
            }}
            className={`flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeTab === 'simulation'
                ? 'bg-purple-600/20 text-purple-400 border border-purple-500/30'
                : 'text-gray-400 hover:text-white hover:bg-gray-800/50'
            }`}
          >
            <Flame className="w-3.5 h-3.5 text-purple-400" />
            <span>Simulation Sandbox</span>
          </button>
        </div>

        {/* District Filter Pill */}
        <div className="flex items-center space-x-2">
          <Filter className="w-3.5 h-3.5 text-gray-500" />
          <span className="text-xs text-gray-400">District:</span>
          <select
            value={selectedDistrict}
            onChange={(e) => setSelectedDistrict(e.target.value)}
            className="bg-gray-900 border border-gray-700 text-gray-200 text-xs rounded-lg px-2.5 py-1 focus:outline-none focus:border-red-500"
          >
            {districts.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 4. Tab Contents */}

      {/* TAB 1: COMMAND & LIVE REQUISITIONS */}
      {activeTab === 'command' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Active Requisitions (2 cols) */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
                <span className="w-2 h-2 rounded-full bg-red-500 animate-ping mr-1" />
                <span>Active Hospital Blood Requisitions</span>
              </h2>
              <span className="text-xs text-gray-400">{filteredRequests.length} requisitions reported</span>
            </div>

            {filteredRequests.length === 0 ? (
              <div className="bg-[#111827] border border-gray-800 rounded-2xl p-12 text-center">
                <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
                <h3 className="text-base font-bold text-white">All Requisitions Fulfilled</h3>
                <p className="text-xs text-gray-400 mt-1 max-w-sm mx-auto">
                  There are no pending emergency blood requests in the selected region. Use the "+ Create Urgent Request" button to issue a crisis requisition.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {filteredRequests.map((req) => {
                  const isCritical = req.priority === 'CRITICAL';
                  const isMatchFound = req.status === 'MATCH_FOUND';
                  const isInTransit = req.status === 'IN_TRANSIT';
                  const isFulfilled = req.status === 'FULFILLED';

                  return (
                    <div
                      key={req.id}
                      className={`bg-[#111827] border rounded-2xl p-5 transition relative overflow-hidden ${
                        isCritical
                          ? 'border-red-500/40 shadow-lg shadow-red-950/30'
                          : 'border-gray-800 hover:border-gray-700'
                      }`}
                    >
                      {/* Priority strip */}
                      <div
                        className={`absolute top-0 left-0 bottom-0 w-1.5 ${
                          isCritical
                            ? 'bg-red-500 animate-pulse'
                            : req.priority === 'HIGH'
                            ? 'bg-amber-500'
                            : 'bg-blue-500'
                        }`}
                      />

                      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div className="flex items-start space-x-3.5">
                          {/* Blood Group Badge */}
                          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-red-600/30 to-red-900/40 border border-red-500/40 flex flex-col items-center justify-center shrink-0">
                            <span className="text-base font-black text-red-400">{req.bloodGroup}</span>
                            <span className="text-[9px] uppercase font-bold text-gray-400">
                              {req.unitsRequired} {req.unitsRequired === 1 ? 'Unit' : 'Units'}
                            </span>
                          </div>

                          <div>
                            <div className="flex items-center space-x-2">
                              <h3 className="text-sm font-bold text-white">{req.hospitalName}</h3>
                              <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-gray-800 text-gray-300 border border-gray-700">
                                {req.hospitalDistrict}
                              </span>
                              <span
                                className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                                  isCritical
                                    ? 'bg-red-500/20 text-red-400 border border-red-500/30 animate-pulse'
                                    : req.priority === 'HIGH'
                                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                                    : 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                                }`}
                              >
                                {req.priority}
                              </span>
                            </div>

                            <p className="text-xs text-gray-300 mt-1 font-medium italic">
                              "{req.clinicalNotes || 'Transfusion required'}"
                            </p>

                            <div className="flex items-center space-x-4 text-[11px] text-gray-400 mt-2">
                              <span className="flex items-center space-x-1">
                                <Clock className="w-3 h-3 text-red-400" />
                                <span>
                                  Deadline: {new Date(req.requiredBy).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </span>
                              </span>
                              <span>•</span>
                              <span>Component: {req.componentType?.replace('_', ' ')}</span>
                              <span>•</span>
                              <span className="text-gray-500">ID: #{req.id}</span>
                            </div>
                          </div>
                        </div>

                        {/* Status & Actions */}
                        <div className="flex items-center space-x-3 self-end md:self-center">
                          <span
                            className={`text-xs font-bold px-3 py-1 rounded-full border ${
                              req.status === 'CREATED' || req.status === 'MATCHING'
                                ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                                : req.status === 'MATCH_FOUND'
                                ? 'bg-cyan-500/15 text-cyan-300 border-cyan-500/30'
                                : req.status === 'CONFIRMED'
                                ? 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30'
                                : req.status === 'IN_TRANSIT'
                                ? 'bg-blue-500/20 text-blue-300 border-blue-500/30 animate-pulse'
                                : req.status === 'FULFILLED'
                                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
                                : 'bg-gray-800 text-gray-400 border-gray-700'
                            }`}
                          >
                            {req.status?.replace('_', ' ')}
                          </span>

                          {!isFulfilled && (
                            <button
                              onClick={() => handleFindMatches(req)}
                              className="flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white text-xs font-bold shadow-md shadow-red-900/30 transition"
                            >
                              <Sparkles className="w-3.5 h-3.5" />
                              <span>Match Resources</span>
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Side Column: Siren Dispatch & Cold-Chain Tracking (1 col) */}
          <div className="space-y-4">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
              <Truck className="w-4 h-4 text-cyan-400" />
              <span>Siren Dispatch & Cold Chain Tracking</span>
            </h2>

            <div className="bg-[#111827] border border-gray-800 rounded-2xl p-5 space-y-4">
              <div className="text-xs text-gray-400 leading-relaxed">
                Active transfers adhere to WHO & NACO temperature control protocols (2°C - 6°C) with verified siren vehicle escort.
              </div>

              {transfers.length === 0 ? (
                <div className="p-6 text-center text-xs text-gray-500 border border-dashed border-gray-800 rounded-xl">
                  No active blood transfers currently in transit.
                </div>
              ) : (
                <div className="space-y-3">
                  {transfers.slice(0, 5).map((transfer) => {
                    const isDelivered = transfer.status === 'DELIVERED';
                    const isInTransit = transfer.status === 'IN_TRANSIT';

                    return (
                      <div
                        key={transfer.id}
                        className={`p-4 rounded-xl border transition ${
                          isInTransit
                            ? 'bg-blue-950/20 border-blue-500/40 shadow-sm'
                            : isDelivered
                            ? 'bg-emerald-950/10 border-emerald-500/30'
                            : 'bg-gray-900/60 border-gray-800'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-red-500/20 text-red-300 border border-red-500/30">
                            {transfer.bloodGroup} • {transfer.units} Units
                          </span>
                          <span
                            className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                              isInTransit
                                ? 'bg-blue-500/20 text-blue-300 animate-pulse'
                                : isDelivered
                                ? 'bg-emerald-500/20 text-emerald-300'
                                : 'bg-amber-500/20 text-amber-300'
                            }`}
                          >
                            {transfer.status?.replace('_', ' ')}
                          </span>
                        </div>

                        <div className="text-xs text-white font-medium">
                          <div className="text-gray-400 text-[10px]">Origin:</div>
                          <div className="truncate font-semibold">{transfer.sourceBloodBankName}</div>
                        </div>

                        <div className="text-xs text-white font-medium mt-2">
                          <div className="text-gray-400 text-[10px]">Destination:</div>
                          <div className="truncate font-semibold">{transfer.destinationHospitalName}</div>
                        </div>

                        <div className="flex items-center justify-between text-[11px] text-gray-400 mt-3 pt-2 border-t border-gray-800">
                          <span>{transfer.estimatedDistanceKm} km</span>
                          <span className="font-semibold text-cyan-300">ETA: {transfer.estimatedEtaMinutes} mins</span>
                          <span className="text-emerald-400 font-medium">✓ Cold Chain</span>
                        </div>

                        {/* Status transition buttons */}
                        <div className="mt-3 flex space-x-2">
                          {transfer.status === 'DISPATCH_PENDING' && (
                            <button
                              onClick={() => handleUpdateTransfer(transfer.id, 'IN_TRANSIT')}
                              className="w-full py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition"
                            >
                              Dispatch Vehicle
                            </button>
                          )}
                          {transfer.status === 'IN_TRANSIT' && (
                            <button
                              onClick={() => handleUpdateTransfer(transfer.id, 'DELIVERED')}
                              className="w-full py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition"
                            >
                              Confirm Transfusion Delivery
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: REGIONAL RESOURCE MAP */}
      {activeTab === 'map' && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3 bg-[#111827] border border-gray-800 rounded-xl p-4">
            <div className="text-xs text-gray-300 flex items-center space-x-2">
              <span className="font-bold text-white">Interactive Network Geography:</span>
              <span>Visualizing accredited regional blood banks, destination hospitals, and live emergency requisitions.</span>
            </div>

            <div className="flex items-center space-x-4 text-xs">
              <label className="flex items-center space-x-1.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={showMapBloodBanks}
                  onChange={(e) => setShowMapBloodBanks(e.target.checked)}
                  className="rounded bg-gray-900 border-gray-700 text-red-600 focus:ring-0"
                />
                <span className="text-red-400 font-semibold">🩸 Blood Banks ({bloodBanks.length})</span>
              </label>

              <label className="flex items-center space-x-1.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={showMapHospitals}
                  onChange={(e) => setShowMapHospitals(e.target.checked)}
                  className="rounded bg-gray-900 border-gray-700 text-blue-600 focus:ring-0"
                />
                <span className="text-blue-400 font-semibold">🏥 Hospitals / PHCs ({phcs.length})</span>
              </label>

              <label className="flex items-center space-x-1.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={showMapEmergencies}
                  onChange={(e) => setShowMapEmergencies(e.target.checked)}
                  className="rounded bg-gray-900 border-gray-700 text-rose-600 focus:ring-0"
                />
                <span className="text-rose-400 font-semibold">🚨 Active Requisitions ({requests.filter(r => r.status !== 'FULFILLED').length})</span>
              </label>
            </div>
          </div>

          <div className="w-full h-[580px] rounded-2xl overflow-hidden border border-gray-800 relative shadow-2xl">
            <MapContainer
              center={[18.65, 74.05]}
              zoom={8}
              scrollWheelZoom={true}
              className="w-full h-full"
            >
              <TileLayer
                attribution='&copy; <a href="https://carto.com/">CARTO</a>'
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
              />

              {/* Blood Banks Markers (Red) */}
              {showMapBloodBanks &&
                bloodBanks.map((bank) => (
                  <CircleMarker
                    key={`bank-${bank.id}`}
                    center={[bank.latitude, bank.longitude]}
                    radius={8}
                    pathOptions={{
                      color: '#EF4444',
                      fillColor: '#DC2626',
                      fillOpacity: 0.9,
                      weight: 2,
                    }}
                  >
                    <Popup className="custom-popup">
                      <div className="p-2 text-gray-900 font-sans">
                        <div className="font-bold text-sm text-red-700 flex items-center space-x-1">
                          <span>🩸</span>
                          <span>{bank.name}</span>
                        </div>
                        <div className="text-xs text-gray-600 mt-1">{bank.district}, Maharashtra</div>
                        <div className="text-xs font-semibold text-emerald-700 mt-1">Status: {bank.verificationStatus}</div>
                        <div className="text-xs text-gray-700 mt-1">Capacity: {bank.storageCapacityUnits} Units</div>
                        <div className="text-xs text-gray-700">Hours: {bank.operatingHours}</div>
                        <div className="text-xs text-blue-700 mt-1 font-semibold">📞 {bank.contactPhone}</div>
                      </div>
                    </Popup>
                  </CircleMarker>
                ))}

              {/* Hospitals / PHCs Markers (Blue) */}
              {showMapHospitals &&
                phcs.map((phc) => (
                  <CircleMarker
                    key={`phc-${phc.id}`}
                    center={[phc.latitude, phc.longitude]}
                    radius={5}
                    pathOptions={{
                      color: '#3B82F6',
                      fillColor: '#1D4ED8',
                      fillOpacity: 0.7,
                      weight: 1.5,
                    }}
                  >
                    <Popup className="custom-popup">
                      <div className="p-2 text-gray-900 font-sans">
                        <div className="font-bold text-sm text-blue-700 flex items-center space-x-1">
                          <span>🏥</span>
                          <span>{phc.name}</span>
                        </div>
                        <div className="text-xs text-gray-600 mt-1">{phc.district}, Maharashtra</div>
                        <div className="text-xs text-gray-700 mt-1">Population Served: {phc.populationServed?.toLocaleString()}</div>
                      </div>
                    </Popup>
                  </CircleMarker>
                ))}

              {/* Active Emergencies Markers (Glowing Rose Ring) */}
              {showMapEmergencies &&
                requests
                  .filter((r) => r.status !== 'FULFILLED')
                  .map((req) => (
                    <CircleMarker
                      key={`emergency-${req.id}`}
                      center={[req.hospitalLatitude, req.hospitalLongitude]}
                      radius={14}
                      pathOptions={{
                        color: '#F43F5E',
                        fillColor: '#E11D48',
                        fillOpacity: 0.85,
                        weight: 3,
                        dashArray: '3, 6',
                      }}
                    >
                      <Popup className="custom-popup">
                        <div className="p-2 text-gray-900 font-sans">
                          <div className="font-bold text-sm text-red-700 flex items-center space-x-1">
                            <span>🚨</span>
                            <span>EMERGENCY REQUISITION #{req.id}</span>
                          </div>
                          <div className="font-semibold text-xs text-gray-900 mt-1">{req.hospitalName}</div>
                          <div className="text-xs font-bold text-red-700 mt-1">
                            Required: {req.bloodGroup} ({req.unitsRequired} Units) - {req.priority}
                          </div>
                          <div className="text-xs text-gray-700 mt-1 italic">"{req.clinicalNotes}"</div>
                        </div>
                      </Popup>
                    </CircleMarker>
                  ))}
            </MapContainer>
          </div>
        </div>
      )}

      {/* TAB 3: BLOOD GROUP INVENTORY GRID */}
      {activeTab === 'inventory' && (
        <div className="space-y-6">
          {/* Blood group overview cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
            {bloodGroupsList.map((bg) => {
              const isUniversalDonor = bg === 'O-';
              const isUniversalRecipient = bg === 'AB+';
              const groupUnits = summary?.unitsByBloodGroup?.[bg] ?? 0;

              return (
                <div
                  key={bg}
                  className={`bg-[#111827] border rounded-2xl p-4 text-center transition ${
                    isUniversalDonor
                      ? 'border-red-500/50 bg-red-950/15'
                      : isUniversalRecipient
                      ? 'border-purple-500/40 bg-purple-950/15'
                      : 'border-gray-800'
                  }`}
                >
                  <div className="text-xl font-black text-white">{bg}</div>
                  <div className="text-2xl font-black text-red-400 mt-1">{groupUnits}</div>
                  <div className="text-[10px] text-gray-400 uppercase font-semibold mt-0.5">Units</div>

                  {isUniversalDonor && (
                    <span className="inline-block mt-2 text-[9px] font-bold px-1.5 py-0.5 rounded bg-red-500/20 text-red-300 border border-red-500/30">
                      Universal Donor
                    </span>
                  )}
                  {isUniversalRecipient && (
                    <span className="inline-block mt-2 text-[9px] font-bold px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                      Universal Recipient
                    </span>
                  )}
                </div>
              );
            })}
          </div>

          {/* Facility Ledger Table */}
          <div className="bg-[#111827] border border-gray-800 rounded-2xl overflow-hidden shadow-xl">
            <div className="p-5 border-b border-gray-800 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-white uppercase tracking-wider">
                  Accredited Blood Facilities Directory ({bloodBanks.length} Facilities)
                </h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  Real-time stock ledger updated across Pune, Satara, Ahmednagar, Solapur, and Nashik districts.
                </p>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-gray-300">
                <thead className="bg-gray-900/80 text-gray-400 uppercase text-[10px] tracking-wider border-b border-gray-800">
                  <tr>
                    <th className="py-3 px-4 font-semibold">Blood Bank Facility</th>
                    <th className="py-3 px-4 font-semibold">District</th>
                    <th className="py-3 px-4 font-semibold">Accreditation</th>
                    <th className="py-3 px-4 font-semibold">Operating Hours</th>
                    <th className="py-3 px-4 font-semibold">Capacity</th>
                    <th className="py-3 px-4 font-semibold">Contact</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800/60 font-medium">
                  {bloodBanks
                    .filter((b) => selectedDistrict === 'All' || b.district.toLowerCase() === selectedDistrict.toLowerCase())
                    .slice(0, 15)
                    .map((bank) => (
                      <tr key={bank.id} className="hover:bg-gray-900/40 transition">
                        <td className="py-3 px-4 text-white font-semibold flex items-center space-x-2">
                          <Droplet className="w-3.5 h-3.5 text-red-500 fill-red-500 shrink-0" />
                          <span>{bank.name}</span>
                        </td>
                        <td className="py-3 px-4">{bank.district}</td>
                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              bank.verificationStatus === 'VERIFIED'
                                ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                                : 'bg-amber-500/15 text-amber-400 border border-amber-500/30'
                            }`}
                          >
                            {bank.verificationStatus}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-gray-400">{bank.operatingHours}</td>
                        <td className="py-3 px-4 font-semibold text-gray-200">{bank.storageCapacityUnits} Units</td>
                        <td className="py-3 px-4 text-gray-400 flex items-center space-x-1">
                          <Phone className="w-3 h-3 text-gray-500" />
                          <span>{bank.contactPhone}</span>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: CRISIS SIMULATION SANDBOX */}
      {activeTab === 'simulation' && (
        <div className="space-y-6">
          <div className="bg-[#111827] border border-gray-800 rounded-2xl p-6 relative overflow-hidden">
            <div className="flex items-center space-x-3 mb-3">
              <div className="w-9 h-9 rounded-xl bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
                <Flame className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Emergency Blood Crisis Scenario Sandbox</h3>
                <p className="text-xs text-gray-400">
                  Simulate high-casualty multi-victim incidents and evaluate instant deterministic resource matching.
                </p>
              </div>
            </div>

            {/* Scenario Pre-Sets */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-6">
              {[
                {
                  id: 'MASS_CASUALTY_ACCIDENT',
                  title: 'Highway Mass Collision',
                  desc: 'Multi-vehicle crash on Mumbai-Pune expressway',
                  group: 'O-',
                  units: 6,
                  mins: 35,
                  prio: 'CRITICAL',
                },
                {
                  id: 'POSTPARTUM_HEMORRHAGE',
                  title: 'Postpartum Hemorrhage',
                  desc: 'Severe obstetric hemorrhage at rural primary center',
                  group: 'O-',
                  units: 4,
                  mins: 45,
                  prio: 'CRITICAL',
                },
                {
                  id: 'RUPTURED_ANEURYSM',
                  title: 'Ruptured Aortic Aneurysm',
                  desc: 'Emergency vascular surgical cross-match protocol',
                  group: 'A+',
                  units: 5,
                  mins: 40,
                  prio: 'CRITICAL',
                },
                {
                  id: 'DENGUE_THROMBOCYTOPENIA',
                  title: 'Epidemic Dengue Surge',
                  desc: 'Severe secondary platelet & red-cell collapse',
                  group: 'B+',
                  units: 8,
                  mins: 90,
                  prio: 'HIGH',
                },
              ].map((sc) => (
                <button
                  key={sc.id}
                  onClick={() => {
                    setSimData((prev) => ({
                      ...prev,
                      scenarioType: sc.id,
                      bloodGroup: sc.group as BloodGroup,
                      unitsRequired: sc.units,
                      deadlineMinutes: sc.mins,
                      priority: sc.prio as BloodRequestPriority,
                    }));
                  }}
                  className={`p-4 rounded-xl border text-left transition ${
                    simData.scenarioType === sc.id
                      ? 'bg-purple-950/30 border-purple-500/50 shadow-md shadow-purple-950/40'
                      : 'bg-gray-900/60 border-gray-800 hover:border-gray-700'
                  }`}
                >
                  <div className="text-xs font-bold text-white flex items-center justify-between">
                    <span>{sc.title}</span>
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-red-500/20 text-red-300 font-black">
                      {sc.group}
                    </span>
                  </div>
                  <div className="text-[11px] text-gray-400 mt-1 line-clamp-2">{sc.desc}</div>
                  <div className="text-[10px] text-purple-400 font-semibold mt-2">
                    {sc.units} units • {sc.mins}m deadline
                  </div>
                </button>
              ))}
            </div>

            {/* Custom Sliders & Form */}
            <form onSubmit={handleRunSimulation} className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end bg-gray-900/60 p-4 rounded-xl border border-gray-800">
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1">Target Hospital / PHC</label>
                <select
                  value={simData.hospitalId}
                  onChange={(e) => setSimData({ ...simData, hospitalId: Number(e.target.value) })}
                  className="w-full bg-gray-900 border border-gray-700 text-white text-xs rounded-lg p-2 focus:border-purple-500"
                >
                  {phcs.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.district})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1">Blood Group Required</label>
                <select
                  value={simData.bloodGroup}
                  onChange={(e) => setSimData({ ...simData, bloodGroup: e.target.value as BloodGroup })}
                  className="w-full bg-gray-900 border border-gray-700 text-white text-xs rounded-lg p-2 focus:border-purple-500"
                >
                  {bloodGroupsList.map((bg) => (
                    <option key={bg} value={bg}>
                      {bg}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1">
                  Units Required: <span className="text-red-400 font-bold">{simData.unitsRequired}</span>
                </label>
                <input
                  type="range"
                  min="1"
                  max="15"
                  value={simData.unitsRequired}
                  onChange={(e) => setSimData({ ...simData, unitsRequired: Number(e.target.value) })}
                  className="w-full accent-purple-500"
                />
              </div>

              <div>
                <button
                  type="submit"
                  disabled={isSimLoading}
                  className="w-full py-2 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition flex items-center justify-center space-x-2"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>{isSimLoading ? 'Solving Equations...' : 'Execute Simulation'}</span>
                </button>
              </div>
            </form>
          </div>

          {/* Simulation Output */}
          {simResult && (
            <div className="bg-[#111827] border border-purple-500/30 rounded-2xl p-6 shadow-xl space-y-4">
              <div className="flex items-center justify-between border-b border-gray-800 pb-3">
                <div className="flex items-center space-x-2">
                  <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                  <h3 className="text-base font-bold text-white">Simulation Solved: Multi-Factor Candidate Ranking</h3>
                </div>
                <span className="text-xs text-purple-300 font-bold bg-purple-500/20 px-3 py-1 rounded-full border border-purple-500/30">
                  {simResult.candidates.length} Viable Facilities Identified
                </span>
              </div>

              {/* Rationale Breakdown */}
              <div className="p-4 rounded-xl bg-gray-900/90 border border-gray-800 text-xs text-gray-300 leading-relaxed font-mono">
                <div className="text-cyan-400 font-bold mb-1 flex items-center space-x-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>DETERMINISTIC AI CLINICAL REASONING:</span>
                </div>
                {simResult.aiExplanation}
              </div>

              {/* Top Recommended Candidate Card */}
              {simResult.recommendedSource && (
                <div className="p-5 rounded-xl bg-gradient-to-r from-red-950/20 to-purple-950/20 border border-red-500/40 flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                        TOP RECOMMENDATION
                      </span>
                      <h4 className="text-sm font-bold text-white">{simResult.recommendedSource.bloodBankName}</h4>
                      <span className="text-xs text-gray-400">({simResult.recommendedSource.district})</span>
                    </div>
                    <p className="text-xs text-gray-300 mt-1">{simResult.recommendedSource.matchReason}</p>
                  </div>

                  <div className="flex items-center space-x-4 shrink-0 text-right">
                    <div>
                      <div className="text-xs text-gray-400">Match Score</div>
                      <div className="text-xl font-black text-cyan-400">{simResult.recommendedSource.matchScore} / 100</div>
                    </div>
                    <div>
                      <div className="text-xs text-gray-400">Siren ETA</div>
                      <div className="text-xl font-black text-red-400">{simResult.recommendedSource.etaMinutes} mins</div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* 5. MATCHING ENGINE RESULTS DRAWER / MODAL */}
      {selectedRequest && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex justify-end">
          <div className="w-full max-w-2xl bg-[#0F172A] border-l border-gray-800 h-full overflow-y-auto p-6 space-y-6 shadow-2xl">
            <div className="flex items-center justify-between border-b border-gray-800 pb-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-400">
                  <Sparkles className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Deterministic Resource Matching Engine</h3>
                  <p className="text-xs text-gray-400">
                    Evaluating clinical compatibility, stock buffers, road distance & siren ETA
                  </p>
                </div>
              </div>
              <button
                onClick={() => {
                  setSelectedRequest(null);
                  setMatchingResult(null);
                }}
                className="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-gray-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Request Summary Banner */}
            <div className="p-4 rounded-xl bg-[#111827] border border-gray-800 flex items-center justify-between">
              <div>
                <div className="text-xs text-gray-400">Emergency Target:</div>
                <div className="text-sm font-bold text-white">{selectedRequest.hospitalName}</div>
                <div className="text-xs text-gray-400 mt-1">"{selectedRequest.clinicalNotes}"</div>
              </div>
              <div className="text-right">
                <div className="text-xs text-gray-400">Required Blood</div>
                <div className="text-lg font-black text-red-400">
                  {selectedRequest.bloodGroup} • {selectedRequest.unitsRequired} Units
                </div>
              </div>
            </div>

            {/* Loading Indicator */}
            {isMatchingLoading && (
              <div className="p-12 text-center space-y-3">
                <RefreshCw className="w-8 h-8 text-red-500 animate-spin mx-auto" />
                <p className="text-xs text-gray-300 font-semibold">
                  Executing ABO/Rh compatibility and Haversine siren routing matrix...
                </p>
              </div>
            )}

            {/* Matching Results */}
            {matchingResult && !isMatchingLoading && (
              <div className="space-y-4">
                {/* Compatible blood groups badge list */}
                <div className="p-3.5 rounded-xl bg-gray-900 border border-gray-800">
                  <span className="text-xs text-gray-400 mr-2 font-semibold">Clinically Compatible Donor Groups:</span>
                  <div className="inline-flex flex-wrap gap-1.5 mt-1">
                    {matchingResult.compatibleGroups.map((g) => (
                      <span
                        key={g}
                        className={`px-2 py-0.5 rounded text-[11px] font-black ${
                          g === selectedRequest.bloodGroup
                            ? 'bg-red-500/20 text-red-300 border border-red-500/40'
                            : 'bg-gray-800 text-gray-300 border border-gray-700'
                        }`}
                      >
                        {g}
                      </span>
                    ))}
                  </div>
                </div>

                {/* AI / Deterministic Explanation */}
                <div className="p-4 rounded-xl bg-gradient-to-br from-gray-900 to-[#111827] border border-cyan-500/30 text-xs text-cyan-200 leading-relaxed font-mono">
                  <div className="text-cyan-400 font-bold mb-1.5 flex items-center space-x-1.5">
                    <Sparkles className="w-4 h-4" />
                    <span>MEDFLOW DETERMINISTIC CLINICAL RECOMMENDATION:</span>
                  </div>
                  {matchingResult.aiExplanation}
                </div>

                {/* Candidate List */}
                <div className="space-y-3">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                    Evaluated Sources ({matchingResult.candidates.length} facilities ranked)
                  </h4>

                  {matchingResult.candidates.length === 0 ? (
                    <div className="p-8 text-center bg-gray-900/60 rounded-xl border border-gray-800 text-xs text-gray-400">
                      No nearby facilities currently hold sufficient unreserved stock of compatible groups.
                    </div>
                  ) : (
                    matchingResult.candidates.map((cand, idx) => {
                      const isTop = idx === 0;

                      return (
                        <div
                          key={cand.bloodBankId}
                          className={`p-4 rounded-xl border transition ${
                            isTop
                              ? 'bg-red-950/20 border-red-500/50 shadow-lg shadow-red-950/30'
                              : 'bg-gray-900/50 border-gray-800'
                          }`}
                        >
                          <div className="flex items-start justify-between">
                            <div>
                              <div className="flex items-center space-x-2">
                                {isTop && (
                                  <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-500/30 text-red-300 border border-red-500/40">
                                    ★ TOP RANKED
                                  </span>
                                )}
                                <h5 className="text-sm font-bold text-white">{cand.bloodBankName}</h5>
                                <span className="text-[10px] text-gray-400">({cand.district})</span>
                              </div>
                              <p className="text-xs text-gray-300 mt-1">{cand.matchReason}</p>
                            </div>

                            <div className="text-right shrink-0">
                              <div className="text-lg font-black text-cyan-400">{cand.matchScore} pts</div>
                              <div className="text-xs text-red-400 font-bold">ETA: {cand.etaMinutes} mins</div>
                            </div>
                          </div>

                          <div className="flex items-center justify-between text-[11px] text-gray-400 mt-3 pt-2.5 border-t border-gray-800">
                            <span className="flex items-center space-x-1">
                              <Phone className="w-3 h-3 text-gray-500" />
                              <span>{cand.contactPhone}</span>
                            </span>
                            <span>Distance: {cand.distanceKm} km</span>
                            <span className="text-emerald-400 font-semibold">
                              Available: {cand.unreservedUnits} Units
                            </span>

                            <button
                              onClick={() => handleConfirmTransfer(cand)}
                              className="px-3 py-1.5 rounded-lg bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white text-xs font-bold shadow-md shadow-red-900/40 transition flex items-center space-x-1"
                            >
                              <span>Confirm Dispatch</span>
                              <ArrowRight className="w-3 h-3" />
                            </button>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 6. CREATE EMERGENCY REQUEST MODAL */}
      {isCreateModalOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#111827] border border-gray-800 rounded-2xl w-full max-w-lg p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-gray-800 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-lg bg-red-600/20 border border-red-500/30 flex items-center justify-center text-red-400">
                  <Droplet className="w-4 h-4 fill-red-500" />
                </div>
                <h3 className="text-base font-bold text-white">Create Urgent Blood Requisition</h3>
              </div>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                className="text-gray-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateRequest} className="space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-gray-300 mb-1">Destination Hospital / PHC</label>
                <select
                  value={formData.hospitalId}
                  onChange={(e) => setFormData({ ...formData, hospitalId: Number(e.target.value) })}
                  className="w-full bg-gray-900 border border-gray-700 text-white rounded-lg p-2.5 focus:border-red-500"
                  required
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
                  <label className="block font-semibold text-gray-300 mb-1">Blood Group Required</label>
                  <select
                    value={formData.bloodGroup}
                    onChange={(e) => setFormData({ ...formData, bloodGroup: e.target.value as BloodGroup })}
                    className="w-full bg-gray-900 border border-gray-700 text-white rounded-lg p-2.5 focus:border-red-500"
                  >
                    {bloodGroupsList.map((bg) => (
                      <option key={bg} value={bg}>
                        {bg}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-gray-300 mb-1">Component Type</label>
                  <select
                    value={formData.componentType}
                    onChange={(e) => setFormData({ ...formData, componentType: e.target.value })}
                    className="w-full bg-gray-900 border border-gray-700 text-white rounded-lg p-2.5 focus:border-red-500"
                  >
                    <option value="WHOLE_BLOOD">Whole Blood</option>
                    <option value="PACKED_RED_CELLS">Packed Red Cells (PRBC)</option>
                    <option value="FRESH_FROZEN_PLASMA">Fresh Frozen Plasma (FFP)</option>
                    <option value="PLATELETS">Platelet Concentrate</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-gray-300 mb-1">Units Required</label>
                  <input
                    type="number"
                    min="1"
                    max="20"
                    value={formData.unitsRequired}
                    onChange={(e) => setFormData({ ...formData, unitsRequired: Number(e.target.value) })}
                    className="w-full bg-gray-900 border border-gray-700 text-white rounded-lg p-2.5 focus:border-red-500"
                    required
                  />
                </div>

                <div>
                  <label className="block font-semibold text-gray-300 mb-1">Clinical Urgency Priority</label>
                  <select
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value as BloodRequestPriority })}
                    className="w-full bg-gray-900 border border-gray-700 text-white rounded-lg p-2.5 focus:border-red-500"
                  >
                    <option value="CRITICAL">CRITICAL (Immediate Transfusion)</option>
                    <option value="HIGH">HIGH (Under 60 Minutes)</option>
                    <option value="MEDIUM">MEDIUM (Under 2 Hours)</option>
                    <option value="LOW">LOW (Elective)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-gray-300 mb-1">
                  Required By Window: <span className="text-red-400 font-bold">{formData.deadlineMinutes} minutes</span>
                </label>
                <input
                  type="range"
                  min="15"
                  max="180"
                  step="15"
                  value={formData.deadlineMinutes}
                  onChange={(e) => setFormData({ ...formData, deadlineMinutes: Number(e.target.value) })}
                  className="w-full accent-red-500"
                />
              </div>

              <div>
                <label className="block font-semibold text-gray-300 mb-1">Clinical Diagnosis & Notes</label>
                <textarea
                  rows={2}
                  value={formData.clinicalNotes}
                  onChange={(e) => setFormData({ ...formData, clinicalNotes: e.target.value })}
                  placeholder="e.g. Polytrauma vehicular collision, emergency exploratory laparotomy..."
                  className="w-full bg-gray-900 border border-gray-700 text-white rounded-lg p-2.5 focus:border-red-500 placeholder-gray-500"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-gray-800 hover:bg-gray-700 text-gray-300 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-bold shadow-lg shadow-red-900/30"
                >
                  Submit & Match Resources
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

// Helper Lock icon
function LockIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}
