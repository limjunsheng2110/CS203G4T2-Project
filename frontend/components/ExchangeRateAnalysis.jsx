import React, { useState } from 'react';
import { TrendingUp, TrendingDown, Minus, Calendar, DollarSign, ExternalLink, AlertCircle, CheckCircle } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';

const ExchangeRateAnalysis = ({ importingCountry, exportingCountry, theme }) => {
  const [analysisData, setAnalysisData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const colours = getThemeColours(theme);

  const fetchExchangeRateAnalysis = async () => {
    if (!importingCountry || !exportingCountry) {
      setError('Please select both importing and exporting countries');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:8080/api/exchange-rates/analyze?importingCountry=${encodeURIComponent(importingCountry)}&exportingCountry=${encodeURIComponent(exportingCountry)}`
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.details || 'Failed to fetch exchange rate analysis');
      }

      const data = await response.json();
      setAnalysisData(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch exchange rate data');
      console.error('Exchange rate analysis error:', err);
    } finally {
      setLoading(false);
    }
  };

  const getTrendIcon = (trend) => {
    switch (trend) {
      case 'increasing':
        return <TrendingUp className="text-red-600" size={24} />;
      case 'decreasing':
        return <TrendingDown className="text-green-600" size={24} />;
      default:
        return <Minus className="text-blue-600" size={24} />;
    }
  };

  const getTrendColor = (trend) => {
    switch (trend) {
      case 'increasing':
        return 'text-red-600';
      case 'decreasing':
        return 'text-green-600';
      default:
        return 'text-blue-600';
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  };

  const formatRate = (rate) => {
    if (!rate) return 'N/A';
    return parseFloat(rate).toFixed(4);
  };

  return (
    <div className={`${colours.cardBg} rounded-lg shadow-md p-6 mb-6`}>
      <div className="flex justify-between items-center mb-4">
        <h3 className={`text-xl font-bold ${colours.headingText}`}>
          Exchange Rate Analysis
        </h3>
        <button
          onClick={fetchExchangeRateAnalysis}
          disabled={loading}
          className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? 'Analyzing...' : 'Analyze Exchange Rates'}
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4 flex items-start gap-3">
          <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
          <div>
            <p className="text-red-800 font-medium">Error</p>
            <p className="text-red-700 text-sm">{error}</p>
          </div>
        </div>
      )}

      {analysisData && (
        <div className="space-y-6">
          {/* Data Source Status */}
          <div className={`${analysisData.liveDataAvailable ? 'bg-green-50 border-green-200' : 'bg-yellow-50 border-yellow-200'} border rounded-lg p-4 flex items-start gap-3`}>
            {analysisData.liveDataAvailable ? (
              <CheckCircle className="text-green-600 flex-shrink-0 mt-0.5" size={20} />
            ) : (
              <AlertCircle className="text-yellow-600 flex-shrink-0 mt-0.5" size={20} />
            )}
            <div>
              <p className={`${analysisData.liveDataAvailable ? 'text-green-800' : 'text-yellow-800'} font-medium`}>
                {analysisData.liveDataAvailable ? 'Live Data' : 'Fallback Data'}
              </p>
              <p className={`${analysisData.liveDataAvailable ? 'text-green-700' : 'text-yellow-700'} text-sm`}>
                {analysisData.message}
              </p>
              <a 
                href="https://openexchangerates.org/" 
                target="_blank" 
                rel="noopener noreferrer"
                className={`${analysisData.liveDataAvailable ? 'text-green-600 hover:text-green-800' : 'text-yellow-600 hover:text-yellow-800'} text-sm inline-flex items-center gap-1 mt-1`}
              >
                Validate on OpenExchangeRates <ExternalLink size={14} />
              </a>
            </div>
          </div>

          {/* Current Exchange Rate */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className={`${colours.fieldBg} rounded-lg p-4`}>
              <div className="flex items-center gap-2 mb-2">
                <DollarSign className="text-green-600" size={20} />
                <h4 className={`font-semibold ${colours.labelText}`}>Current Exchange Rate</h4>
              </div>
              <p className="text-3xl font-bold text-green-600">
                {formatRate(analysisData.currentRate)}
              </p>
              <p className={`text-sm ${colours.labelText} mt-1`}>
                {analysisData.exportingCurrency} → {analysisData.importingCurrency}
              </p>
              <p className="text-xs text-gray-500 mt-1">
                As of {formatDate(analysisData.currentRateDate)}
              </p>
            </div>

            {/* Trend Analysis */}
            <div className={`${colours.fieldBg} rounded-lg p-4`}>
              <div className="flex items-center gap-2 mb-2">
                {getTrendIcon(analysisData.trendAnalysis)}
                <h4 className={`font-semibold ${colours.labelText}`}>Trend Analysis</h4>
              </div>
              <p className={`text-2xl font-bold ${getTrendColor(analysisData.trendAnalysis)} capitalize`}>
                {analysisData.trendAnalysis}
              </p>
              <p className={`text-sm ${colours.labelText} mt-2`}>
                Past 6 months average: {formatRate(analysisData.averageRate)}
              </p>
            </div>
          </div>

          {/* Historical Min/Max */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className={`${colours.fieldBg} rounded-lg p-4 border-2 border-green-200`}>
              <h4 className={`font-semibold ${colours.labelText} mb-2`}>
                Best Rate (Lowest)
              </h4>
              <p className="text-2xl font-bold text-green-600">
                {formatRate(analysisData.minRate)}
              </p>
              <p className="text-sm text-gray-600 mt-1">
                on {formatDate(analysisData.minRateDate)}
              </p>
            </div>

            <div className={`${colours.fieldBg} rounded-lg p-4 border-2 border-red-200`}>
              <h4 className={`font-semibold ${colours.labelText} mb-2`}>
                Worst Rate (Highest)
              </h4>
              <p className="text-2xl font-bold text-red-600">
                {formatRate(analysisData.maxRate)}
              </p>
              <p className="text-sm text-gray-600 mt-1">
                on {formatDate(analysisData.maxRateDate)}
              </p>
            </div>
          </div>

          {/* Recommendation */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <Calendar className="text-blue-600 flex-shrink-0 mt-0.5" size={24} />
              <div>
                <h4 className="font-semibold text-blue-900 mb-2">
                  Recommended Purchase Date
                </h4>
                <p className="text-2xl font-bold text-blue-600 mb-2">
                  {formatDate(analysisData.recommendedPurchaseDate)}
                </p>
                <p className="text-sm text-blue-800">
                  {analysisData.recommendation}
                </p>
              </div>
            </div>
          </div>

          {/* Simple Historical Chart */}
          {analysisData.historicalRates && analysisData.historicalRates.length > 0 && (
            <div className={`${colours.fieldBg} rounded-lg p-4`}>
              <h4 className={`font-semibold ${colours.labelText} mb-4`}>
                6-Month Historical Trend
              </h4>
              <div className="space-y-2">
                {analysisData.historicalRates.slice(0, 10).map((point, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <span className="text-xs text-gray-600 w-24">
                      {formatDate(point.date)}
                    </span>
                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                      <div 
                        className="bg-green-600 h-2 rounded-full" 
                        style={{ 
                          width: `${(point.rate / analysisData.maxRate) * 100}%` 
                        }}
                      />
                    </div>
                    <span className="text-xs font-medium text-gray-700 w-16 text-right">
                      {formatRate(point.rate)}
                    </span>
                  </div>
                ))}
                {analysisData.historicalRates.length > 10 && (
                  <p className="text-xs text-gray-500 text-center mt-2">
                    Showing 10 of {analysisData.historicalRates.length} data points
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {!analysisData && !loading && !error && (
        <div className="text-center py-8">
          <DollarSign className="mx-auto text-gray-400 mb-3" size={48} />
          <p className={`${colours.labelText}`}>
            Click "Analyze Exchange Rates" to see exchange rate trends and recommendations
          </p>
        </div>
      )}
    </div>
  );
};

export default ExchangeRateAnalysis;

