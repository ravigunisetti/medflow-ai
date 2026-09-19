import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('MEDFLOW Uncaught Application Error:', error, errorInfo);
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen w-full bg-[#0B0F19] text-gray-100 flex items-center justify-center p-6">
          <div className="max-w-md w-full bg-[#111827] border border-red-500/30 rounded-2xl p-8 shadow-2xl text-center">
            <div className="w-14 h-14 rounded-full bg-red-500/10 border border-red-500/20 text-red-400 flex items-center justify-center mx-auto mb-4">
              <AlertTriangle className="w-7 h-7" />
            </div>

            <h2 className="text-xl font-bold text-white mb-2">MedFlow AI Recovery Mode</h2>
            <p className="text-sm text-gray-400 mb-6">
              An unexpected runtime exception was intercepted. MedFlow safety telemetry prevented a total application crash.
            </p>

            {this.state.error && (
              <div className="p-3 mb-6 rounded-lg bg-gray-950/80 border border-gray-800 text-left font-mono text-xs text-red-300/90 overflow-x-auto max-h-28">
                {this.state.error.message || 'Unknown error'}
              </div>
            )}

            <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
              <button
                onClick={this.handleReload}
                className="w-full sm:w-auto px-4 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold flex items-center justify-center space-x-2 transition"
              >
                <RefreshCw className="w-4 h-4" />
                <span>Reload Application</span>
              </button>

              <button
                onClick={this.handleGoHome}
                className="w-full sm:w-auto px-4 py-2.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-xs font-semibold flex items-center justify-center space-x-2 transition"
              >
                <Home className="w-4 h-4" />
                <span>Command Center</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
