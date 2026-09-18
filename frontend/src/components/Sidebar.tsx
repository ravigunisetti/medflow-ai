import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  Package,
  AlertOctagon,
  ArrowLeftRight,
  Bot,
  Flame,
  ActivitySquare,
  Droplet,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/phcs', label: 'PHC Facilities', icon: Building2 },
    { to: '/inventory', label: 'Inventory Ledger', icon: Package },
    { to: '/predictions', label: 'Predictions & Alerts', icon: AlertOctagon },
    { to: '/transfers', label: 'Stock Transfers', icon: ArrowLeftRight },
    { to: '/blood-network', label: 'Emergency Blood', icon: Droplet, badge: 'URGENT' },
    { to: '/ai-assistant', label: 'Gemini AI Agent', icon: Bot, badge: 'AI' },
    { to: '/simulation', label: 'Emergency Simulation', icon: Flame, badge: 'DEMO' },
    { to: '/system-health', label: 'System Health', icon: ActivitySquare },
  ];

  return (
    <aside className="w-64 bg-[#0F172A] border-r border-gray-800 flex flex-col justify-between shrink-0">
      <div className="py-5 px-3">
        <div className="px-3 mb-4 text-[11px] font-semibold text-gray-400 uppercase tracking-wider">
          Operations Command
        </div>
        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                    isActive
                      ? 'bg-blue-600/15 text-blue-400 border border-blue-500/30'
                      : 'text-gray-300 hover:bg-gray-800/60 hover:text-white'
                  }`
                }
              >
                <div className="flex items-center space-x-3">
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${
                      item.badge === 'AI'
                        ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                        : item.badge === 'URGENT'
                        ? 'bg-red-500/25 text-red-300 border border-red-500/40 animate-pulse'
                        : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Network Metadata Footer */}
      <div className="p-4 border-t border-gray-800/80 bg-gray-950/40 text-xs text-gray-400">
        <div className="flex items-center justify-between mb-1">
          <span>Target Network</span>
          <span className="font-semibold text-gray-200">Maharashtra Cluster</span>
        </div>
        <div className="flex items-center justify-between">
          <span>Active Nodes</span>
          <span className="font-semibold text-emerald-400">100 PHCs</span>
        </div>
        <div className="mt-3 pt-2 border-t border-gray-800 text-[10px] text-gray-400 text-center">
          Google Cloud Hackathon • 2026
        </div>
      </div>
    </aside>
  );
};
