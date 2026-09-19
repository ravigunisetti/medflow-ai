import React, { useEffect, useState } from 'react';
import { Search, Filter, MapPin, Users, ExternalLink } from 'lucide-react';
import { Phc } from '../types';
import { apiService } from '../services/api';
import { PhcDetailModal } from '../components/PhcDetailModal';

export const PhcsPage: React.FC = () => {
  const [phcs, setPhcs] = useState<Phc[]>([]);
  const [districtFilter, setDistrictFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [selectedPhc, setSelectedPhc] = useState<Phc | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const districts = ['All Districts', 'Pune', 'Nashik', 'Satara', 'Ahmednagar', 'Kolhapur'];

  useEffect(() => {
    setLoading(true);
    const filter = districtFilter === 'All Districts' ? '' : districtFilter;
    apiService
      .getPhcs(filter, page, 20)
      .then((data) => {
        setPhcs(Array.isArray(data?.content) ? data.content : []);
        setTotalPages(data?.totalPages || 1);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, [districtFilter, page]);

  const filteredPhcs = (phcs || []).filter(
    (p) =>
      p && (
        p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.district.toLowerCase().includes(searchQuery.toLowerCase())
      )
  );

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight">Primary Health Centres (PHCs)</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            Directory and operational status of 100 primary healthcare facilities across 5 Maharashtra districts
          </p>
        </div>

        {/* Filters */}
        <div className="flex items-center space-x-3">
          <div className="relative">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search PHC name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-gray-900 border border-gray-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          <select
            value={districtFilter}
            onChange={(e) => {
              setDistrictFilter(e.target.value);
              setPage(0);
            }}
            className="bg-gray-900 border border-gray-800 rounded-lg px-3 py-1.5 text-xs text-gray-200 focus:outline-none focus:border-blue-500"
          >
            {districts.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Facilities Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredPhcs.map((phc) => (
          <div
            key={phc.id}
            onClick={() => setSelectedPhc(phc)}
            className="bg-[#111827] border border-gray-800 rounded-xl p-5 hover:border-blue-500/50 cursor-pointer transition flex flex-col justify-between group"
          >
            <div>
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold text-white group-hover:text-blue-400 transition text-sm">
                    {phc.name}
                  </h3>
                  <div className="flex items-center space-x-1.5 text-xs text-gray-400 mt-1">
                    <MapPin className="w-3.5 h-3.5 text-gray-500" />
                    <span>
                      {phc.district}, {phc.state}
                    </span>
                  </div>
                </div>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-gray-800 text-gray-300 font-mono font-medium">
                  #{phc.id}
                </span>
              </div>

              <div className="mt-4 pt-3 border-t border-gray-800/80 grid grid-cols-2 gap-2 text-xs">
                <div>
                  <span className="text-gray-500 block text-[10px] uppercase tracking-wider">Population</span>
                  <span className="font-semibold text-gray-200">{phc.populationServed.toLocaleString()}</span>
                </div>
                <div>
                  <span className="text-gray-500 block text-[10px] uppercase tracking-wider">Coordinates</span>
                  <span className="font-mono text-[11px] text-gray-300">
                    {phc.latitude.toFixed(2)}, {phc.longitude.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>

            <div className="mt-4 pt-3 border-t border-gray-800/50 flex items-center justify-between text-xs text-blue-400 font-medium group-hover:translate-x-0.5 transition">
              <span>Inspect Facility Ledger</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </div>
          </div>
        ))}
      </div>

      {/* Pagination Bar */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-800 text-xs text-gray-400">
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

      <PhcDetailModal phc={selectedPhc} onClose={() => setSelectedPhc(null)} />
    </div>
  );
};
