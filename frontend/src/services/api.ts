import axios from 'axios';
import {
  ApiResponse,
  PageResponse,
  DashboardSummary,
  Phc,
  Medicine,
  InventoryItem,
  Prediction,
  Alert,
  Transfer,
  DemandRecord,
  FootfallRecord,
  BloodBank,
  BloodInventoryItem,
  EmergencyBloodRequest,
  CreateBloodRequestInput,
  CandidateBloodResource,
  BloodMatchingResult,
  ConfirmBloodTransferInput,
  BloodTransfer,
  BloodDashboardSummary,
  BloodSimulationInput,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('phcnet_jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const apiService = {
  // Authentication & RBAC
  login: async (username: string, password: string) => {
    const res = await apiClient.post<ApiResponse<any>>('/auth/login', { username, password });
    if (res.data.data?.token) {
      localStorage.setItem('phcnet_jwt_token', res.data.data.token);
      localStorage.setItem('phcnet_user_profile', JSON.stringify(res.data.data));
    }
    return res.data.data;
  },

  getCurrentUser: async () => {
    const res = await apiClient.get<ApiResponse<any>>('/auth/me');
    return res.data.data;
  },

  getDemoAccounts: async () => {
    const res = await apiClient.get<ApiResponse<any[]>>('/auth/demo-accounts');
    return res.data.data;
  },

  logout: () => {
    localStorage.removeItem('phcnet_jwt_token');
    localStorage.removeItem('phcnet_user_profile');
  },
  // Dashboard
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const res = await apiClient.get<ApiResponse<DashboardSummary>>('/dashboard/summary');
    return res.data.data;
  },

  // PHCs
  getPhcs: async (district?: string, page = 0, size = 20): Promise<PageResponse<Phc>> => {
    const params = { district, page, size };
    const res = await apiClient.get<ApiResponse<PageResponse<Phc>>>('/phcs', { params });
    return res.data.data;
  },

  getAllActivePhcs: async (): Promise<Phc[]> => {
    const res = await apiClient.get<ApiResponse<Phc[]>>('/phcs/active');
    return res.data.data;
  },

  getPhcById: async (id: number): Promise<Phc> => {
    const res = await apiClient.get<ApiResponse<Phc>>(`/phcs/${id}`);
    return res.data.data;
  },

  // Medicines
  getAllMedicines: async (): Promise<Medicine[]> => {
    const res = await apiClient.get<ApiResponse<Medicine[]>>('/medicines/all');
    return res.data.data;
  },

  // Inventory
  getInventories: async (phcId?: number, medicineId?: number, page = 0, size = 25): Promise<PageResponse<InventoryItem>> => {
    const params = { phcId, medicineId, page, size };
    const res = await apiClient.get<ApiResponse<PageResponse<InventoryItem>>>('/inventory', { params });
    return res.data.data;
  },

  getInventoryByPhc: async (phcId: number): Promise<InventoryItem[]> => {
    const res = await apiClient.get<ApiResponse<InventoryItem[]>>(`/inventory/phc/${phcId}`);
    return res.data.data;
  },

  updateInventory: async (id: number, quantity: number, reservedQuantity?: number, batchNumber?: string, expiryDate?: string): Promise<InventoryItem> => {
    const res = await apiClient.put<ApiResponse<InventoryItem>>(`/inventory/${id}`, {
      quantity,
      reservedQuantity,
      batchNumber,
      expiryDate,
    });
    return res.data.data;
  },

  // Predictions & Alerts
  getPredictions: async (riskLevel?: string, page = 0, size = 25): Promise<PageResponse<Prediction>> => {
    const params = { riskLevel, page, size };
    const res = await apiClient.get<ApiResponse<PageResponse<Prediction>>>('/predictions', { params });
    return res.data.data;
  },

  getActiveAlerts: async (): Promise<Alert[]> => {
    const res = await apiClient.get<ApiResponse<Alert[]>>('/alerts');
    return res.data.data;
  },

  evaluateAllRisks: async (): Promise<void> => {
    await apiClient.post<ApiResponse<string>>('/predictions/evaluate-all');
  },

  // Transfers
  getTransfers: async (status?: string, page = 0, size = 20): Promise<PageResponse<Transfer>> => {
    const params = { status, page, size };
    const res = await apiClient.get<ApiResponse<PageResponse<Transfer>>>('/transfers', { params });
    return res.data.data;
  },

  updateTransferStatus: async (id: number, status: string, rejectionReason?: string): Promise<Transfer> => {
    const res = await apiClient.patch<ApiResponse<Transfer>>(`/transfers/${id}/status`, {
      status,
      rejectionReason,
      approvedByUserId: 1, // Default admin user ID for demo workflow
    });
    return res.data.data;
  },

  createTransfer: async (data: {
    sourcePhcId: number;
    destinationPhcId: number;
    medicineId: number;
    quantity: number;
    recommendationReason?: string;
  }): Promise<Transfer> => {
    const res = await apiClient.post<ApiResponse<Transfer>>('/transfers', data);
    return res.data.data;
  },

  // Historical Telemetry for Charts
  getDemandHistory: async (phcId: number, medicineId: number, days = 30): Promise<DemandRecord[]> => {
    const res = await apiClient.get<ApiResponse<DemandRecord[]>>('/demands/history', {
      params: { phcId, medicineId, days },
    });
    return res.data.data;
  },

  getFootfallHistory: async (phcId: number, days = 30): Promise<FootfallRecord[]> => {
    const res = await apiClient.get<ApiResponse<FootfallRecord[]>>('/footfall/history', {
      params: { phcId, days },
    });
    return res.data.data;
  },

  // Emergency Outbreak Simulation
  getSimulationScenarios: async () => {
    const res = await apiClient.get<ApiResponse<import('../types').SimulationScenario[]>>('/simulation/scenarios');
    return res.data.data;
  },

  runSimulation: async (req: import('../types').SimulationRequest) => {
    const res = await apiClient.post<ApiResponse<import('../types').SimulationResult>>('/simulation/run', req);
    return res.data.data;
  },

  // Emergency Blood Network
  getBloodBanks: async (district?: string): Promise<BloodBank[]> => {
    const params = district ? { district } : {};
    const res = await apiClient.get<ApiResponse<BloodBank[]>>('/blood/banks', { params });
    return res.data.data;
  },

  getBloodBankById: async (id: number): Promise<BloodBank> => {
    const res = await apiClient.get<ApiResponse<BloodBank>>(`/blood/banks/${id}`);
    return res.data.data;
  },

  getBloodBankInventory: async (id: number): Promise<BloodInventoryItem[]> => {
    const res = await apiClient.get<ApiResponse<BloodInventoryItem[]>>(`/blood/banks/${id}/inventory`);
    return res.data.data;
  },

  getAllBloodInventory: async (): Promise<BloodInventoryItem[]> => {
    const res = await apiClient.get<ApiResponse<BloodInventoryItem[]>>('/blood/inventory');
    return res.data.data;
  },

  getEmergencyBloodRequests: async (): Promise<EmergencyBloodRequest[]> => {
    const res = await apiClient.get<ApiResponse<EmergencyBloodRequest[]>>('/blood/requests');
    return res.data.data;
  },

  getEmergencyBloodRequestById: async (id: number): Promise<EmergencyBloodRequest> => {
    const res = await apiClient.get<ApiResponse<EmergencyBloodRequest>>(`/blood/requests/${id}`);
    return res.data.data;
  },

  createEmergencyBloodRequest: async (input: CreateBloodRequestInput): Promise<EmergencyBloodRequest> => {
    const res = await apiClient.post<ApiResponse<EmergencyBloodRequest>>('/blood/requests', input);
    return res.data.data;
  },

  getBloodMatches: async (requestId: number): Promise<BloodMatchingResult> => {
    const res = await apiClient.get<ApiResponse<BloodMatchingResult>>(`/blood/requests/${requestId}/matches`);
    return res.data.data;
  },

  confirmBloodTransfer: async (input: ConfirmBloodTransferInput): Promise<BloodTransfer> => {
    const res = await apiClient.post<ApiResponse<BloodTransfer>>('/blood/transfers/confirm', input);
    return res.data.data;
  },

  updateBloodTransferStatus: async (transferId: number, status: string): Promise<BloodTransfer> => {
    const res = await apiClient.patch<ApiResponse<BloodTransfer>>(`/blood/transfers/${transferId}/status`, null, {
      params: { status },
    });
    return res.data.data;
  },

  getBloodTransfers: async (): Promise<BloodTransfer[]> => {
    const res = await apiClient.get<ApiResponse<BloodTransfer[]>>('/blood/transfers');
    return res.data.data;
  },

  getBloodDashboardSummary: async (): Promise<BloodDashboardSummary> => {
    const res = await apiClient.get<ApiResponse<BloodDashboardSummary>>('/blood/dashboard/summary');
    return res.data.data;
  },

  simulateBloodEmergency: async (input: BloodSimulationInput): Promise<BloodMatchingResult> => {
    const res = await apiClient.post<ApiResponse<BloodMatchingResult>>('/blood/simulate', input);
    return res.data.data;
  },
};

