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
