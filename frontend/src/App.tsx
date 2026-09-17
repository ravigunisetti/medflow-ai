import React, { useState, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { DashboardPage } from './pages/DashboardPage';
import { PhcsPage } from './pages/PhcsPage';
import { InventoryPage } from './pages/InventoryPage';
import { PredictionsPage } from './pages/PredictionsPage';
import { TransfersPage } from './pages/TransfersPage';
import { AiAssistantPage } from './pages/AiAssistantPage';
import { SimulationPage } from './pages/SimulationPage';
import { SystemHealthPage } from './pages/SystemHealthPage';
import { apiService } from './services/api';

export const App: React.FC = () => {
  const [healthScore, setHealthScore] = useState<number>(96.4);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const refreshTelemetry = async () => {
    setIsRefreshing(true);
    try {
      const summary = await apiService.getDashboardSummary();
      if (summary?.networkHealthScore) {
        setHealthScore(summary.networkHealthScore);
      }
    } catch (err) {
      console.warn('Telemetry refresh notice:', err);
    } finally {
      setTimeout(() => setIsRefreshing(false), 500);
    }
  };

  useEffect(() => {
    refreshTelemetry();
  }, []);

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-[#0B0F19]">
      {/* Fixed Sidebar */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Navbar
          healthScore={healthScore}
          onRefresh={refreshTelemetry}
          isRefreshing={isRefreshing}
        />

        <main className="flex-1 overflow-y-auto p-6">
          <div className="max-w-7xl mx-auto">
            <Routes>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/phcs" element={<PhcsPage />} />
              <Route path="/inventory" element={<InventoryPage />} />
              <Route path="/predictions" element={<PredictionsPage />} />
              <Route path="/transfers" element={<TransfersPage />} />
              <Route path="/ai-assistant" element={<AiAssistantPage />} />
              <Route path="/simulation" element={<SimulationPage />} />
              <Route path="/system-health" element={<SystemHealthPage />} />
            </Routes>
          </div>
        </main>
      </div>
    </div>
  );
};

export default App;
