import React, { useState } from 'react';
import { TrendingUp, TrendingDown, Minus, Calendar, AlertCircle, CheckCircle, ExternalLink, BarChart3, Bug } from 'lucide-react';
import { getThemeColours } from '../../utils/themeColours';

const PredictiveAnalysis = ({ importingCountry, exportingCountry, theme }) => {
  const [predictionData, setPredictionData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [enableNewsAnalysis, setEnableNewsAnalysis] = useState(true);
  const [debugInfo, setDebugInfo] = useState(null);
  const [debugLoading, setDebugLoading] = useState(false);
  const [debugError, setDebugError] = useState(null);
  const [showDebugTools, setShowDebugTools] = useState(false);
  const [runLiveCheck, setRunLiveCheck] = useState(false);
  const colours = getThemeColours(theme);

  const fetchPrediction = async () => {
    if (!importingCountry || !exportingCountry) {
      setError('Please select both importing and exporting countries');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('authToken');
      const headers = {
        'Content-Type': 'application/json',
      };
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }

      const response = await fetch(
        `http://localhost:8080/api/predictive-analysis/predict?importingCountry=${encodeURIComponent(importingCountry)}&exportingCountry=${encodeURIComponent(exportingCountry)}&enableNewsAnalysis=${enableNewsAnalysis}`,
        {
          headers,
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.details || 'Failed to fetch prediction');
      }

      const data = await response.json();
      setPredictionData(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch prediction');
      console.error('Prediction error:', err);
    } finally {
      setLoading(false);
    }
  };

  const runDiagnostics = async () => {
    if (!importingCountry || !exportingCountry) {
      setDebugError('Please select both importing and exporting countries to run diagnostics.');
      setShowDebugTools(true);
      return;
    }

    setDebugLoading(true);
    setDebugError(null);

    try {
      const url = `http://localhost:8080/api/predictive-analysis/debug/diagnostics?importingCountry=${encodeURIComponent(importingCountry)}&exportingCountry=${encodeURIComponent(exportingCountry)}&testNewsApi=${runLiveCheck}`;
      const token = localStorage.getItem('authToken');
      const headers = {};
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
      const response = await fetch(url, { headers });

      const contentType = response.headers.get('content-type');
      if (!response.ok) {
        if (contentType && contentType.includes('application/json')) {
          const data = await response.json();
          const message = data.details || data.error || JSON.stringify(data);
          throw new Error(message || 'Diagnostics failed');
        }
        const text = await response.text();
        throw new Error(text || `Diagnostics failed with status ${response.status}`);
      }

      const data = await response.json();
      setDebugInfo(data);
    } catch (err) {
      console.error('Predictive diagnostics error:', err);
      setDebugError(err.message || 'Failed to run diagnostics');
      setDebugInfo(null);
    } finally {
      setDebugLoading(false);
    }
  };

  const getRecommendationColor = (recommendation) => {
    switch (recommendation) {
      case 'BUY':
        return 'bg-green-100 border-green-500 text-green-800';
      case 'WAIT':
        return 'bg-yellow-100 border-yellow-500 text-yellow-800';
      case 'HOLD':
        return 'bg-blue-100 border-blue-500 text-blue-800';
      default:
        return 'bg-gray-100 border-gray-500 text-gray-800';
    }
  };

  const getRecommendationIcon = (recommendation) => {
    switch (recommendation) {
      case 'BUY':
        return <TrendingUp className="text-green-600" size={32} />;
      case 'WAIT':
        return <Minus className="text-yellow-600" size={32} />;
      case 'HOLD':
        return <TrendingDown className="text-blue-600" size={32} />;
      default:
        return <BarChart3 className="text-gray-600" size={32} />;
    }
  };

  const formatSentiment = (score) => {
    if (score === null || score === undefined) return 'N/A';
    // Show actual score with label instead of percentage
    const label = getSentimentLabel(score);
    const scoreStr = score.toFixed(2);
    return `${scoreStr} (${label})`;
  };

  const getSentimentLabel = (score) => {
    if (score > 0.3) return 'Positive';
    if (score < -0.3) return 'Negative';
    return 'Neutral';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className={`${colours.cardBg} rounded-lg shadow-md p-6 mb-6`}>
      {/* Header with Toggle */}
      <div className="flex justify-between items-center mb-4">
        <div>
          <h3 className={`text-xl font-bold ${colours.headingText} flex items-center gap-2`}>
            <BarChart3 size={24} />
            AI-Driven Predictive Analysis
          </h3>
          <p className="text-sm text-gray-600 mt-1">
            News sentiment + market trends = smart purchase timing
          </p>
        </div>
        
        <div className="flex items-center gap-4">
          {/* Toggle for News Analysis */}
          <label className="flex items-center gap-2 cursor-pointer">
            <span className="text-sm font-medium text-gray-700">Enable News Analysis</span>
            <div 
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                enableNewsAnalysis ? 'bg-green-600' : 'bg-gray-300'
              }`}
              onClick={() => setEnableNewsAnalysis(!enableNewsAnalysis)}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  enableNewsAnalysis ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </div>
          </label>
          
          <button
            onClick={fetchPrediction}
            disabled={loading}
            className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          >
            {loading ? 'Analyzing...' : 'Get Prediction'}
          </button>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4 flex items-start gap-3">
          <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
          <div>
            <p className="text-red-800 font-medium">Error</p>
            <p className="text-red-700 text-sm">{error}</p>
          </div>
        </div>
      )}

      {/* Debug Tools */}
      <div className={`${colours.inputBg} border ${colours.border} rounded-lg p-4 mb-6`}>
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <Bug size={18} className="text-purple-300" />
            <span className={`text-sm font-semibold ${colours.labelText}`}>Debug Tools</span>
          </div>
          <button
            type="button"
            onClick={() => setShowDebugTools(!showDebugTools)}
            className="text-xs font-medium text-purple-300 hover:text-purple-200 transition-colors"
          >
            {showDebugTools ? 'Hide' : 'Show'}
          </button>
        </div>

        {showDebugTools && (
          <div className="mt-4 space-y-4">
            <p className={`text-xs ${colours.textMuted}`}>
              Quickly inspect common configuration issues. Optional live checks will hit external APIs and may take a few seconds.
            </p>

            <div className="flex items-center gap-3">
              <span className={`text-xs ${colours.textMuted}`}>Run live News API connectivity test</span>
              <div
                className={`relative inline-flex h-5 w-10 items-center rounded-full transition-colors ${runLiveCheck ? 'bg-green-500' : 'bg-gray-500'}`}
                onClick={() => setRunLiveCheck(!runLiveCheck)}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${runLiveCheck ? 'translate-x-5' : 'translate-x-1'}`}
                />
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                onClick={runDiagnostics}
                disabled={debugLoading}
                className="bg-purple-600 hover:bg-purple-700 text-white px-3 py-2 rounded-lg text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {debugLoading ? 'Running diagnostics...' : 'Run Diagnostics'}
              </button>
              {debugLoading && (
                <span className={`text-xs ${colours.textMuted}`}>Checking backend services…</span>
              )}
            </div>

            {debugError && (
              <div className="bg-red-500/10 border border-red-500/40 rounded-lg p-3">
                <p className="text-sm text-red-200">{debugError}</p>
              </div>
            )}

            {debugInfo && (
              <div className="space-y-3">
                <div className={`${colours.fieldBg} border ${colours.border} rounded-lg p-3`}>
                  <p className={`text-xs uppercase tracking-wide ${colours.textMuted}`}>News API</p>
                  <p className={`text-sm font-semibold ${debugInfo.newsApiKeyPresent ? 'text-green-300' : 'text-red-300'} mt-1`}>
                    {debugInfo.newsApiKeyPresent ? 'API key detected' : 'API key missing'}
                  </p>
                  {debugInfo.newsApiReachable !== null && (
                    <p className={`text-xs mt-1 ${debugInfo.newsApiReachable ? 'text-green-300' : 'text-red-300'}`}>
                      {debugInfo.newsApiReachable ? 'Connectivity test passed' : 'Connectivity test failed'}
                    </p>
                  )}
                  {debugInfo.newsApiMessage && (
                    <p className={`text-xs mt-1 ${colours.textMuted}`}>{debugInfo.newsApiMessage}</p>
                  )}
                </div>

                <div className={`${colours.fieldBg} border ${colours.border} rounded-lg p-3`}>
                  <p className={`text-xs uppercase tracking-wide ${colours.textMuted}`}>Sentiment Data</p>
                  {debugInfo.sentimentDataAvailable ? (
                    <>
                      <p className="text-sm font-semibold text-green-300 mt-1">
                        Latest score: {debugInfo.latestSentimentScore != null ? debugInfo.latestSentimentScore.toFixed(2) : 'N/A'}
                      </p>
                      <p className={`text-xs ${colours.textMuted}`}>
                        Articles analyzed: {debugInfo.sentimentArticlesAnalyzed ?? 'N/A'}
                      </p>
                      {debugInfo.latestSentimentTimestamp && (
                        <p className={`text-xs ${colours.textMuted}`}>
                          Updated: {formatDate(debugInfo.latestSentimentTimestamp)}
                        </p>
                      )}
                    </>
                  ) : (
                    <p className="text-sm text-red-300 mt-1">No stored sentiment data.</p>
                  )}
                </div>

                <div className={`${colours.fieldBg} border ${colours.border} rounded-lg p-3`}>
                  <p className={`text-xs uppercase tracking-wide ${colours.textMuted}`}>Exchange Rates</p>
                  {debugInfo.exchangeRateAvailable ? (
                    <>
                      <p className="text-sm font-semibold text-green-300 mt-1">
                        Pair: {debugInfo.exchangeRatePair}
                      </p>
                      <p className={`text-xs ${colours.textMuted}`}>
                        Latest rate: {debugInfo.latestExchangeRate != null ? debugInfo.latestExchangeRate.toFixed(4) : 'N/A'} (on{' '}
                        {debugInfo.latestExchangeRateDate ? new Date(debugInfo.latestExchangeRateDate).toLocaleDateString('en-US', {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric'
                        }) : 'N/A'})
                      </p>
                    </>
                  ) : (
                    <p className="text-sm text-red-300 mt-1">No exchange rate data for selected route.</p>
                  )}
                  {debugInfo.currencyResolutionMessage && (
                    <p className={`text-xs mt-1 ${colours.textMuted}`}>{debugInfo.currencyResolutionMessage}</p>
                  )}
                </div>

                {debugInfo.warnings && debugInfo.warnings.length > 0 && (
                  <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3">
                    <p className="text-sm font-semibold text-red-200 mb-2">Warnings</p>
                    <ul className="list-disc list-inside text-xs text-red-200 space-y-1">
                      {debugInfo.warnings.map((warning, idx) => (
                        <li key={idx}>{warning}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {debugInfo.suggestions && debugInfo.suggestions.length > 0 && (
                  <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-3">
                    <p className="text-sm font-semibold text-green-200 mb-2">Suggestions</p>
                    <ul className="list-disc list-inside text-xs text-green-200 space-y-1">
                      {debugInfo.suggestions.map((suggestion, idx) => (
                        <li key={idx}>{suggestion}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {debugInfo.generatedAt && (
                  <p className={`text-xs ${colours.textMuted}`}>Generated: {formatDate(debugInfo.generatedAt)}</p>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Prediction Results */}
      {predictionData && (
        <div className="space-y-6">
          {/* Data Source Status */}
          <div className={`${predictionData.liveNewsAvailable ? 'bg-green-50 border-green-200' : 'bg-yellow-50 border-yellow-200'} border rounded-lg p-4 flex items-start gap-3`}>
            {predictionData.liveNewsAvailable ? (
              <CheckCircle className="text-green-600 flex-shrink-0 mt-0.5" size={20} />
            ) : (
              <AlertCircle className="text-yellow-600 flex-shrink-0 mt-0.5" size={20} />
            )}
            <div className="flex-1">
              <p className={`${predictionData.liveNewsAvailable ? 'text-green-800' : 'text-yellow-800'} font-medium`}>
                {predictionData.liveNewsAvailable ? 'Live News Data' : 'Fallback Data'}
              </p>
              <p className={`${predictionData.liveNewsAvailable ? 'text-green-700' : 'text-yellow-700'} text-sm`}>
                {predictionData.message}
              </p>
              <a 
                href="https://newsapi.org/" 
                target="_blank" 
                rel="noopener noreferrer"
                className={`${predictionData.liveNewsAvailable ? 'text-green-600 hover:text-green-800' : 'text-yellow-600 hover:text-yellow-800'} text-sm inline-flex items-center gap-1 mt-1`}
              >
                Verify on NewsAPI.org <ExternalLink size={14} />
              </a>
            </div>
          </div>

          {/* Main Recommendation Card */}
          <div className={`border-4 rounded-xl p-6 ${getRecommendationColor(predictionData.recommendation)}`}>
            <div className="flex items-center gap-4">
              <div className="flex-shrink-0">
                {getRecommendationIcon(predictionData.recommendation)}
              </div>
              <div className="flex-1">
                <h4 className="text-2xl font-bold mb-1">
                  Recommendation: {predictionData.recommendation}
                </h4>
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg font-semibold">
                    Confidence: {(predictionData.confidenceScore * 100).toFixed(0)}%
                  </span>
                  <div className="flex-1 bg-white bg-opacity-50 rounded-full h-3">
                    <div 
                      className="bg-current h-3 rounded-full" 
                      style={{ width: `${predictionData.confidenceScore * 100}%` }}
                    />
                  </div>
                </div>
                <p className="text-sm">
                  {predictionData.rationale}
                </p>
              </div>
            </div>
          </div>

          {/* Sentiment & Exchange Rate Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Current Sentiment */}
            <div className={`${colours.fieldBg} rounded-lg p-4 border-2 border-purple-200`}>
              <h4 className={`font-semibold ${colours.labelText} mb-3`}>
                Market Sentiment
              </h4>
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Sentiment Score</span>
                  <span className="text-lg font-bold text-purple-600">
                    {formatSentiment(predictionData.currentSentiment)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Polarity</span>
                  <span className={`font-medium ${
                    predictionData.currentSentiment > 0 ? 'text-green-600' : 
                    predictionData.currentSentiment < 0 ? 'text-red-600' : 'text-gray-600'
                  }`}>
                    {getSentimentLabel(predictionData.currentSentiment)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Trend</span>
                  <span className="font-medium capitalize">
                    {predictionData.sentimentTrend}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Articles Analyzed</span>
                  <span className="font-medium">
                    {predictionData.articlesAnalyzed}
                  </span>
                </div>
              </div>
            </div>

            {/* Exchange Rate Context */}
            <div className={`${colours.fieldBg} rounded-lg p-4 border-2 border-blue-200`}>
              <h4 className={`font-semibold ${colours.labelText} mb-3`}>
                Exchange Rate Context
              </h4>
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Current Rate</span>
                  <span className="text-lg font-bold text-blue-600">
                    {predictionData.currentExchangeRate?.toFixed(4)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Currency Pair</span>
                  <span className="font-medium">
                    {predictionData.exportingCountry} → {predictionData.importingCountry}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Rate Trend</span>
                  <span className="font-medium capitalize">
                    {predictionData.exchangeRateTrend}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Supporting Headlines */}
          {predictionData.supportingHeadlines && predictionData.supportingHeadlines.length > 0 && (
            <div className={`${colours.fieldBg} rounded-lg p-4`}>
              <h4 className={`font-semibold ${colours.labelText} mb-3`}>
                Supporting News Headlines
              </h4>
              <div className="space-y-3">
                {predictionData.supportingHeadlines.map((headline, index) => (
                  <div key={index} className="border-l-4 border-purple-500 pl-4 py-2">
                    <a 
                      href={headline.url} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:text-blue-800 font-medium hover:underline"
                    >
                      {headline.title}
                    </a>
                    <div className="flex items-center gap-3 mt-1 text-xs text-gray-600">
                      <span>{headline.source}</span>
                      <span>•</span>
                      <span>{formatDate(headline.publishedAt)}</span>
                      <span>•</span>
                      <span className={headline.sentimentScore > 0 ? 'text-green-600' : headline.sentimentScore < 0 ? 'text-red-600' : 'text-gray-600'}>
                        Sentiment: {formatSentiment(headline.sentimentScore)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Sentiment History Chart */}
          {predictionData.sentimentHistory && predictionData.sentimentHistory.length > 0 && (
            <div className={`${colours.fieldBg} rounded-lg p-4`}>
              <h4 className={`font-semibold ${colours.labelText} mb-4`}>
                Sentiment Trend (Past 4 Weeks)
              </h4>
              <div className="space-y-3">
                {predictionData.sentimentHistory.map((point, index) => (
                  <div key={index} className="space-y-1">
                    <div className="flex items-center justify-between text-xs text-gray-600">
                      <span>
                        {new Date(point.weekStart).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} - {new Date(point.weekEnd).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                      </span>
                      <span>{point.articleCount} articles</span>
                    </div>
                    <div className="flex items-center gap-3">
                      {/* Negative side indicator */}
                      <span className="text-xs text-gray-500 w-12 text-right">-1.0</span>

                      {/* Sentiment bar */}
                      <div className="flex-1 relative">
                        {/* Background track with center line */}
                        <div className="relative bg-gray-200 rounded-full h-8 flex items-center">
                          {/* Center line */}
                          <div className="absolute left-1/2 h-full w-0.5 bg-gray-400"></div>

                          {/* Sentiment fill */}
                          {point.averageSentiment !== 0 && (
                            <div
                              className={`absolute h-full rounded-full transition-all ${
                                point.averageSentiment > 0 ? 'bg-green-500' : 'bg-red-500'
                              }`}
                              style={{
                                width: `${(Math.abs(point.averageSentiment) / 2) * 100}%`,
                                left: point.averageSentiment > 0 ? '50%' : 'auto',
                                right: point.averageSentiment < 0 ? '50%' : 'auto'
                              }}
                            />
                          )}

                          {/* Score label */}
                          <div className="absolute inset-0 flex items-center justify-center">
                            <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                              point.averageSentiment > 0.3 ? 'bg-green-600 text-white' :
                              point.averageSentiment < -0.3 ? 'bg-red-600 text-white' :
                              'bg-gray-600 text-white'
                            }`}>
                              {point.averageSentiment.toFixed(2)} ({getSentimentLabel(point.averageSentiment)})
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Positive side indicator */}
                      <span className="text-xs text-gray-500 w-12">+1.0</span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Legend */}
              <div className="mt-4 pt-3 border-t border-gray-200 flex items-center justify-center gap-6 text-xs">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 bg-red-500 rounded"></div>
                  <span className="text-gray-600">Negative (&lt; -0.3)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 bg-gray-400 rounded"></div>
                  <span className="text-gray-600">Neutral (-0.3 to +0.3)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 bg-green-500 rounded"></div>
                  <span className="text-gray-600">Positive (&gt; +0.3)</span>
                </div>
              </div>
            </div>
          )}

          {/* Analysis Timestamp */}
          <div className="text-center text-xs text-gray-500">
            Analysis generated: {formatDate(predictionData.analysisTimestamp)}
          </div>
        </div>
      )}

      {/* Empty State */}
      {!predictionData && !loading && !error && (
        <div className="text-center py-8">
          <BarChart3 className="mx-auto text-gray-400 mb-3" size={48} />
          <p className={`${colours.labelText} mb-2`}>
            Click "Get Prediction" to see AI-driven purchase recommendations
          </p>
          <p className="text-sm text-gray-500">
            Based on news sentiment analysis and exchange rate trends
          </p>
        </div>
      )}
    </div>
  );
};

export default PredictiveAnalysis;
