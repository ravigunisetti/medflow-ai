export type RiskLevel = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export interface Phc {
  id: number;
  name: string;
  district: string;
  state: string;
  latitude: number;
  longitude: number;
  populationServed: number;
  isActive: boolean;
}

export interface Medicine {
  id: number;
  code: string;
  name: string;
  category: string;
  unit: string;
  safetyStock: number;
  shelfLifeDays: number;
  requiresColdChain: boolean;
}

export interface InventoryItem {
  id: number;
  phcId: number;
  phcName: string;
  district: string;
  medicineId: number;
  medicineCode: string;
  medicineName: string;
  category: string;
  unit: string;
  safetyStock: number;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  batchNumber: string;
  expiryDate: string;
}

export interface Prediction {
  id: number;
  phcId: number;
  phcName: string;
  district: string;
  medicineId: number;
  medicineCode: string;
  medicineName: string;
  predictedDailyDemand: number;
  predictedDaysToStockout: number;
  riskLevel: RiskLevel;
  modelVersion: string;
  confidenceScore: number;
  createdAt: string;
}

export interface Alert {
  phcId: number;
  phcName: string;
  district: string;
  medicineId: number;
  medicineName: string;
  currentStock: number;
  dailyDemand: number;
  daysRemaining: number;
  riskLevel: RiskLevel;
  message: string;
  timestamp: string;
}

export interface Transfer {
  id: number;
  sourcePhcId: number;
  sourcePhcName: string;
  sourceDistrict: string;
  destinationPhcId: number;
  destinationPhcName: string;
  destinationDistrict: string;
  medicineId: number;
  medicineName: string;
  medicineUnit: string;
  quantity: number;
  distanceKm: number;
  estimatedCost: number;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'IN_TRANSIT' | 'COMPLETED' | 'REJECTED';
  recommendationReason: string;
  approvedByUserId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DistrictSummary {
  district: string;
  totalPhcs: number;
  criticalPhcs: number;
  highRiskPhcs: number;
  averageDaysRemaining: number;
  overallStatus: string;
}

export interface DashboardSummary {
  totalPhcsMonitored: number;
  totalMedicinesTracked: number;
  criticalStockouts: number;
  highRiskPredictions: number;
  pendingTransfers: number;
  completedTransfers: number;
  networkHealthScore: number;
  districtSummaries: DistrictSummary[];
}

export interface FootfallRecord {
  id: number;
  phcId: number;
  phcName: string;
  recordDate: string;
  patientCount: number;
  emergencyCount: number;
}

export interface DemandRecord {
  id: number;
  phcId: number;
  phcName: string;
  medicineId: number;
  medicineCode: string;
  medicineName: string;
  recordDate: string;
  quantityUsed: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface SimulationScenario {
  scenarioId: string;
  name: string;
  description: string;
  defaultSurgePercent: number;
  defaultAcuteMultiplier: number;
  defaultSupplyDelayDays: number;
  affectedCategory: string;
  riskSeverity: string;
}

export interface VulnerableFacility {
  phcId: number;
  phc: string;
  district: string;
  medicineName?: string;
  medicineCode?: string;
  currentStock?: number;
  baselineDemand?: number;
  crisisDemand?: number;
  beforeStockDays: number;
  afterStockDays: number;
  newRisk: string;
}

export interface SimulationResult {
  beforeCriticalCount: number;
  afterCriticalCount: number;
  emergencyTransfersGenerated: number;
  preventedStockouts: number;
  affectedFacilitiesCount?: number;
  mitigationRatePercent?: number;
  executionTimeMs?: number;
  scenarioSummary?: string;
  vulnerableFacilities: VulnerableFacility[];
  recommendedTransfers?: any[];
}

export interface SimulationRequest {
  scenarioId?: string;
  district?: string;
  footfallSurgePercent?: number;
  acuteDemandMultiplier?: number;
  supplyDelayDays?: number;
  affectedCategory?: string;
}

// ==========================================
// Emergency Blood Network Types
// ==========================================

export type BloodGroup = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-';
export type BloodRequestPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type BloodRequestStatus =
  | 'CREATED'
  | 'MATCHING'
  | 'MATCH_FOUND'
  | 'CONFIRMED'
  | 'IN_TRANSIT'
  | 'FULFILLED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface BloodBank {
  id: number;
  name: string;
  district: string;
  state: string;
  latitude: number;
  longitude: number;
  verificationStatus: 'VERIFIED' | 'PROVISIONAL' | 'PENDING_AUDIT';
  contactPhone: string;
  contactEmail?: string;
  operatingHours: string;
  storageCapacityUnits: number;
  isActive: boolean;
  createdAt?: string;
}

export interface BloodInventoryItem {
  id: number;
  bloodBankId: number;
  bloodBankName: string;
  bloodGroup: string;
  componentType: string;
  unitsAvailable: number;
  reservedUnits: number;
  unreservedUnits: number;
  lastUpdated: string;
}

export interface EmergencyBloodRequest {
  id: number;
  hospitalId: number;
  hospitalName: string;
  hospitalDistrict: string;
  hospitalLatitude: number;
  hospitalLongitude: number;
  bloodGroup: string;
  componentType: string;
  unitsRequired: number;
  priority: BloodRequestPriority;
  requiredBy: string;
  status: BloodRequestStatus;
  clinicalNotes?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBloodRequestInput {
  hospitalId: number;
  bloodGroup: string;
  componentType?: string;
  unitsRequired: number;
  priority: BloodRequestPriority;
  requiredBy: string;
  clinicalNotes?: string;
}

export interface CandidateBloodResource {
  bloodBankId: number;
  bloodBankName: string;
  district: string;
  latitude: number;
  longitude: number;
  verificationStatus: string;
  contactPhone: string;
  bloodGroup: string;
  componentType: string;
  unitsAvailable: number;
  unreservedUnits: number;
  distanceKm: number;
  etaMinutes: number;
  estimatedArrival: string;
  meetsDeadline: boolean;
  matchScore: number;
  isExactMatch: boolean;
  matchReason: string;
}

export interface BloodMatchingResult {
  request: EmergencyBloodRequest;
  compatibleGroups: string[];
  candidates: CandidateBloodResource[];
  recommendedSource: CandidateBloodResource | null;
  aiExplanation: string;
  safetyDisclaimer: string;
}

export interface ConfirmBloodTransferInput {
  requestId: number;
  sourceBloodBankId: number;
  units: number;
  estimatedDistanceKm?: number;
  estimatedEtaMinutes?: number;
}

export interface BloodTransfer {
  id: number;
  requestId: number;
  bloodGroup: string;
  units: number;
  sourceBloodBankId: number;
  sourceBloodBankName: string;
  sourceDistrict: string;
  destinationHospitalId: number;
  destinationHospitalName: string;
  destinationDistrict: string;
  estimatedDistanceKm: number;
  estimatedEtaMinutes: number;
  status: string;
  coldChainVerified: boolean;
  dispatchedAt?: string;
  deliveredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BloodDashboardSummary {
  totalBloodBanks: number;
  activeBloodBanks: number;
  verifiedBloodBanks: number;
  totalUnitsAvailable: number;
  totalUnitsReserved: number;
  totalEmergencyRequests: number;
  activeEmergencies: number;
  fulfilledEmergencies: number;
  activeTransfers: number;
  unitsByBloodGroup: Record<string, number>;
  requestsByStatus: Record<string, number>;
}

export interface BloodSimulationInput {
  hospitalId?: number;
  bloodGroup: string;
  unitsRequired: number;
  priority: BloodRequestPriority;
  deadlineMinutes: number;
  scenarioType?: string;
}

