import React, { useState, useEffect } from 'react';
import { Activity, ShieldCheck, AlertTriangle, Bell, RefreshCw, ChevronDown, Check, UserCheck } from 'lucide-react';
import { apiService } from '../services/api';

interface NavbarProps {
  healthScore?: number;
  onRefresh?: () => void;
  isRefreshing?: boolean;
}

interface UserProfile {
  username: string;
  fullName: string;
  role: string;
  assignedDistrict?: string;
  assignedPhcId?: number;
}

const DEFAULT_USERS = [
  {
    username: 'admin',
    password: 'admin123',
    fullName: 'Dr. Rajesh Verma',
    roleLabel: 'State Health Director',
    roleTag: 'ADMIN',
    badgeClass: 'bg-purple-600/30 border-purple-500/40 text-purple-300',
    detail: 'Full State Network Authority',
  },
  {
    username: 'district_pune',
    password: 'pune123',
    fullName: 'Dr. Sunita Kulkarni',
    roleLabel: 'District Health Officer',
    roleTag: 'DHO',
    badgeClass: 'bg-blue-600/30 border-blue-500/40 text-blue-300',
    detail: 'Pune District Logistics & Transfers',
  },
  {
    username: 'phc_shirwal',
    password: 'shirwal123',
    fullName: 'Dr. Amit Deshmukh',
    roleLabel: 'Medical Officer In-Charge',
    roleTag: 'MO',
    badgeClass: 'bg-emerald-600/30 border-emerald-500/40 text-emerald-300',
    detail: 'Shirwal PHC-2 Local Inventory',
  },
];

export const Navbar: React.FC<NavbarProps> = ({ healthScore = 96.4, onRefresh, isRefreshing = false }) => {
  const [currentUser, setCurrentUser] = useState<typeof DEFAULT_USERS[0]>(DEFAULT_USERS[1]);
  const [isDropdownOpen, setIsDropdownOpen] = useState<boolean>(false);

  useEffect(() => {
    const saved = localStorage.getItem('phcnet_user_profile');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        const match = DEFAULT_USERS.find((u) => u.username === parsed.username);
        if (match) setCurrentUser(match);
      } catch (e) {
        // use default
      }
    }
  }, []);

  const handleSelectRole = async (userConfig: typeof DEFAULT_USERS[0]) => {
    setCurrentUser(userConfig);
    setIsDropdownOpen(false);

    try {
      await apiService.login(userConfig.username, userConfig.password);
    } catch (err) {
      console.warn('Backend login fallback used', err);
      localStorage.setItem('phcnet_user_profile', JSON.stringify({
        username: userConfig.username,
        fullName: userConfig.fullName,
        role: userConfig.roleTag,
      }));
    }
  };

  return (
    <header className="h-16 bg-[#111827] border-b border-gray-800 px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Brand & Context */}
      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20 font-bold text-white text-lg">
            +
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold tracking-tight text-white text-lg">PHC-NET AI</span>
              <span className="text-[10px] uppercase font-semibold tracking-wider px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">
                PROTOTYPE
              </span>
            </div>
            <p className="text-xs text-gray-400">National Health Mission • Primary Supply-Chain Command</p>
          </div>
        </div>
      </div>

      {/* Center Disclaimer Banner */}
      <div className="hidden lg:flex items-center space-x-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300">
        <AlertTriangle className="w-3.5 h-3.5" />
        <span>Demonstration Mode • Synthetic Epidemiology & Inventory Data Active</span>
      </div>

      {/* Telemetry & Controls */}
      <div className="flex items-center space-x-5">
        {/* Network Health Indicator */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-gray-900 border border-gray-800">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <div className="text-xs">
            <span className="text-gray-400">Health Index: </span>
            <span className="font-bold text-emerald-400">{healthScore}%</span>
          </div>
        </div>

        {/* Refresh button */}
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="p-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 hover:text-white transition disabled:opacity-50"
            title="Refresh live telemetry"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        )}

        {/* User Role Badge & Dropdown */}
        <div className="relative pl-2 border-l border-gray-800">
          <button
            onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            className="flex items-center space-x-2 p-1.5 rounded-lg hover:bg-gray-800/60 transition group cursor-pointer"
            title="Click to switch active RBAC role"
          >
            <div className={`w-8 h-8 rounded-full border flex items-center justify-center text-xs font-bold ${currentUser.badgeClass}`}>
              {currentUser.roleTag}
            </div>
            <div className="hidden sm:block text-left text-xs">
              <div className="font-medium text-gray-200 group-hover:text-white flex items-center space-x-1">
                <span>{currentUser.fullName}</span>
                <ChevronDown className="w-3 h-3 text-gray-400" />
              </div>
              <div className="text-gray-400 text-[10px]">{currentUser.roleLabel}</div>
            </div>
          </button>

          {/* RBAC Switcher Dropdown */}
          {isDropdownOpen && (
            <div className="absolute right-0 mt-2 w-72 bg-[#111827] border border-gray-800 rounded-xl shadow-2xl py-2 z-50">
              <div className="px-3 py-1.5 border-b border-gray-800 text-[11px] font-semibold text-gray-400 uppercase tracking-wider flex items-center justify-between">
                <span>Switch RBAC Role</span>
                <span className="text-[9px] text-indigo-400">Hackathon Sandbox</span>
              </div>
              <div className="py-1">
                {DEFAULT_USERS.map((user) => {
                  const isActive = currentUser.username === user.username;
                  return (
                    <button
                      key={user.username}
                      onClick={() => handleSelectRole(user)}
                      className={`w-full px-3 py-2.5 text-left flex items-start space-x-3 transition ${
                        isActive ? 'bg-indigo-600/15 border-l-2 border-indigo-500' : 'hover:bg-gray-800/50'
                      }`}
                    >
                      <div className={`w-7 h-7 rounded-full border flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5 ${user.badgeClass}`}>
                        {user.roleTag}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-medium text-white truncate">{user.fullName}</span>
                          {isActive && <Check className="w-3.5 h-3.5 text-indigo-400 shrink-0" />}
                        </div>
                        <div className="text-[11px] text-gray-400">{user.roleLabel}</div>
                        <div className="text-[10px] text-gray-500 mt-0.5">{user.detail}</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

