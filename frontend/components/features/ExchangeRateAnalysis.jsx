import React, { useState } from 'react';
import { TrendingUp, TrendingDown, Minus, Calendar, DollarSign, ExternalLink, AlertCircle, CheckCircle } from 'lucide-react';
import { XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts';
import { getThemeColours } from '../../utils/themeColours';
import apiService from '../../services/apiService';

const ExchangeRateAnalysis = ({ importingCountry, exportingCountry, theme }) => {
  const [analysisData, setAnalysisData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const colours = getThemeColours(theme);

  const fetchExchangeRateAnalysis = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await apiService.exchangeRate.getExchangeRateAnalysis(importingCountry, exportingCountry);
      setAnalysisData(data);
    } catch (err) {
      setError(err.message);
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

  // Custom tooltip for the chart
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-gray-800 text-white p-3 rounded-lg shadow-lg border border-gray-600">
          <p className="text-sm font-medium">{formatDate(payload[0].payload.date)}</p>
          <p className="text-lg font-bold text-purple-400">
            {formatRate(payload[0].value)}
          </p>
        </div>
      );
    }
    return null;
  };

  // Format date for chart axis
  const formatDateShort = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
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
          className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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
          <div className={`${analysisData.liveDataAvailable ? 'bg-green-900/30 border-green-500' : 'bg-blue-900/30 border-blue-500'} border rounded-lg p-4 flex items-start gap-3`}>
            {analysisData.liveDataAvailable ? (
              <CheckCircle className="text-green-400 flex-shrink-0 mt-0.5" size={20} />
            ) : (
              <AlertCircle className="text-blue-400 flex-shrink-0 mt-0.5" size={20} />
            )}
            <div>
              <p className={`${analysisData.liveDataAvailable ? 'text-green-300' : 'text-blue-300'} font-medium`}>
                {analysisData.liveDataAvailable ? 'Live Data' : 'Historical Data'}
              </p>
              <p className={`${analysisData.liveDataAvailable ? 'text-green-200' : 'text-blue-200'} text-sm`}>
                {analysisData.message}
              </p>
              <a 
                href="https://openexchangerates.org/" 
                target="_blank" 
                rel="noopener noreferrer"
                className={`${analysisData.liveDataAvailable ? 'text-green-400 hover:text-green-300' : 'text-blue-400 hover:text-blue-300'} text-sm inline-flex items-center gap-1 mt-1`}
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

          {/* Historical Chart */}
          {analysisData.historicalRates && analysisData.historicalRates.length > 0 && (
            <div className={`${colours.fieldBg} rounded-lg p-6`}>
              <h4 className={`font-semibold ${colours.labelText} mb-4 text-lg`}>
                6-Month Historical Trend
              </h4>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart
                  data={analysisData.historicalRates}
                  margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                >
                  <defs>
                    <linearGradient id="colorRate" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#9333ea" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#9333ea" stopOpacity={0.1}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                  <XAxis
                    dataKey="date"
                    tickFormatter={formatDateShort}
                    stroke="#9ca3af"
                    style={{ fontSize: '12px' }}
                  />
                  <YAxis
                    domain={['dataMin - 0.0005', 'dataMax + 0.0005']}
                    tickFormatter={(value) => value.toFixed(4)}
                    stroke="#9ca3af"
                    style={{ fontSize: '12px' }}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Area
                    type="monotone"
                    dataKey="rate"
                    stroke="#9333ea"
                    strokeWidth={3}
                    fillOpacity={1}
                    fill="url(#colorRate)"
                  />
                </AreaChart>
              </ResponsiveContainer>
              <p className="text-xs text-gray-500 text-center mt-4">
                Showing {analysisData.historicalRates.length} data points over the past 6 months
              </p>
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
