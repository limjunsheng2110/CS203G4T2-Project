import React, { useState } from 'react';
import { TrendingUp, TrendingDown, Minus, Calendar, AlertCircle, CheckCircle, ExternalLink, BarChart3 } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';

const PredictiveAnalysis = ({ importingCountry, exportingCountry, theme }) => {
  const [predictionData, setPredictionData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [enableNewsAnalysis, setEnableNewsAnalysis] = useState(true);
  const colours = getThemeColours(theme);

  const fetchPrediction = async () => {
    if (!importingCountry || !exportingCountry) {
      setError('Please select both importing and exporting countries');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:8080/api/predictive-analysis/predict?importingCountry=${encodeURIComponent(importingCountry)}&exportingCountry=${encodeURIComponent(exportingCountry)}&enableNewsAnalysis=${enableNewsAnalysis}`
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
    const percentage = ((score + 1) / 2 * 100).toFixed(0);
    return `${percentage}%`;
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
              <div className="space-y-2">
                {predictionData.sentimentHistory.map((point, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <span className="text-xs text-gray-600 w-32">
                      {new Date(point.weekStart).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} - {new Date(point.weekEnd).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                    </span>
                    <div className="flex-1">
                      <div className="relative bg-gray-200 rounded-full h-6">
                        <div 
                          className={`absolute h-6 rounded-full ${
                            point.averageSentiment > 0 ? 'bg-green-500' : 
                            point.averageSentiment < 0 ? 'bg-red-500' : 'bg-gray-400'
                          }`}
                          style={{ 
                            width: `${Math.abs(point.averageSentiment) * 100}%`,
                            left: point.averageSentiment < 0 ? 'auto' : '50%',
                            right: point.averageSentiment < 0 ? '50%' : 'auto'
                          }}
                        />
                        <div className="absolute inset-0 flex items-center justify-center">
                          <span className="text-xs font-medium text-gray-700">
                            {formatSentiment(point.averageSentiment)}
                          </span>
                        </div>
                      </div>
                    </div>
                    <span className="text-xs text-gray-600 w-16 text-right">
                      {point.articleCount} articles
                    </span>
                  </div>
                ))}
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

