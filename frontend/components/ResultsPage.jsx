import React from 'react';
import { ArrowLeft } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';
import ThemeToggle from './ThemeToggle';
import ResultCard from './ResultCard';
import ExchangeRateAnalysis from './ExchangeRateAnalysis';

const ResultsPage = ({ formData, selectedProduct, tariffResults, handleBack, theme, toggleTheme }) => {
  const colours = getThemeColours(theme);

  // If no results are available, show a message
  if (!tariffResults) {
    return (
      <div className={`min-h-screen ${colours.resultBg} py-8 px-4`}>
        <div className="max-w-5xl mx-auto">
          <div className="flex justify-between items-center mb-6">
            <button
              onClick={handleBack}
              className="flex items-center gap-2 text-green-700 hover:text-green-800 font-medium"
            >
              <ArrowLeft size={20} />
              Back to Search
            </button>

            <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
          </div>

          <div className="flex justify-center mb-6">
            <img
              src="/TariffNomLogo.png"
              alt="TariffNom Logo"
              className="w-96 h-auto"
            />
          </div>

          <div className={`${colours.cardBg} rounded-lg shadow-lg p-8 border ${colours.border} text-center`}>
            <p className={`${colours.text} text-lg`}>No tariff calculation results available.</p>
            <p className={`${colours.textMuted} mt-2`}>Please go back and submit a calculation request.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`min-h-screen ${colours.resultBg} py-8 px-4`}>
      <div className="max-w-5xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <button
            onClick={handleBack}
            className="flex items-center gap-2 text-green-700 hover:text-green-800 font-medium"
          >
            <ArrowLeft size={20} />
            Back to Search
          </button>
          
          <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
        </div>

        <div className="flex justify-center mb-6">
          <img 
            src="/TariffNomLogo.png"
            alt="TariffNom Logo"
            className="w-96 h-auto"
          />
        </div>

        {/* Exchange Rate Analysis Section */}
        <ExchangeRateAnalysis 
          importingCountry={formData.importCountry}
          exportingCountry={formData.exportCountry}
          theme={theme}
        />

        {/* Tariff Calculation Results */}
        <div className="space-y-4">
          <TariffResultCard
            result={tariffResults}
            formData={formData}
            selectedProduct={selectedProduct}
            theme={theme}
            colours={colours}
          />
        </div>
      </div>
    </div>
  );
};

// New component to display tariff calculation results
const TariffResultCard = ({ result, formData, selectedProduct, theme, colours }) => {
  const formatCurrency = (amount) => {
    if (!amount) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Logic to determine trade agreement status based on tariff rate
  const getTradeAgreementStatus = () => {
    const tariffRate = result.adValoremRate || 0;
    if (tariffRate === 0) {
      return `${result.exportingCountry}-${result.importingCountry} Trade Agreement Applied`;
    } else {
      return 'MFN Rate Applied';
    }
  };

  return (
    <div className={`${colours.cardBg} rounded-lg shadow-lg border ${colours.border} overflow-hidden`}>
      {/* Header */}
      <div className="bg-green-600 text-white p-6">
        <h2 className="text-2xl font-bold mb-4">Tariff Calculation Results</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mb-4">
          <div>
            <span className="font-medium">From:</span> {result.exportingCountry}
          </div>
          <div>
            <span className="font-medium">To:</span> {result.importingCountry}
          </div>
        </div>

        {/* Product Information Section */}
        <div className="bg-white/10 rounded-lg p-4">
          <h3 className="font-semibold text-lg mb-2">Product Details</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <span className="font-medium">HS Code:</span> {result.hsCode}
            </div>
            {selectedProduct && (
              <>
                <div>
                  <span className="font-medium">Category:</span> {selectedProduct.category || 'N/A'}
                </div>
                <div className="md:col-span-2">
                  <span className="font-medium">Product:</span>
                  <span className="ml-2">{selectedProduct.description}</span>
                </div>
              </>
            )}
          </div>
        </div>

        {/* Trade Agreement Status */}
        <div className="bg-white/10 rounded-lg p-3 mt-4">
          <div className="flex items-center gap-2">
            <span className="font-semibold">Trade Status:</span>
            <span className="bg-white/20 px-2 py-1 rounded text-sm font-medium">
              {getTradeAgreementStatus()}
            </span>
          </div>
        </div>
      </div>

      <div className="p-6">
        {/* Summary Section */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className={`${colours.inputBg} rounded-lg p-4 text-center`}>
            <div className={`text-2xl font-bold text-green-600 mb-1`}>
              {formatCurrency(result.totalCost)}
            </div>
            <div className={`text-sm ${colours.textMuted}`}>Total Cost</div>
          </div>

          <div className={`${colours.inputBg} rounded-lg p-4 text-center`}>
            <div className={`text-xl font-semibold ${colours.text} mb-1`}>
              {formatCurrency(result.tariffAmount)}
            </div>
            <div className={`text-sm ${colours.textMuted}`}>Total Tariffs</div>
          </div>

          <div className={`${colours.inputBg} rounded-lg p-4 text-center`}>
            <div className={`text-xl font-semibold ${colours.text} mb-1`}>
              {formatCurrency(result.productValue)}
            </div>
            <div className={`text-sm ${colours.textMuted}`}>Product Value</div>
          </div>

          <div className={`${colours.inputBg} rounded-lg p-4 text-center`}>
            <div className={`text-xl font-semibold ${colours.text} mb-1`}>
              {result.adValoremRate ? `${(result.adValoremRate).toFixed(2)}%` : 'N/A'}
            </div>
            <div className={`text-sm ${colours.textMuted}`}>Tariff Rate</div>
          </div>
        </div>

        {/* VAT Rate Display - New Section */}
        {result.vatRate && result.vatRate > 0 && (
          <div className={`${colours.inputBg} rounded-lg p-4 mb-6 border-l-4 border-green-500`}>
            <div className="flex items-center justify-between">
              <div>
                <span className={`font-medium ${colours.text}`}>VAT/GST Rate Applied:</span>
                <span className={`ml-2 ${colours.textMuted} text-sm`}>
                  ({result.importingCountry})
                </span>
              </div>
              <div className="text-xl font-bold text-green-600">
                {result.vatRate.toFixed(2)}%
              </div>
            </div>
          </div>
        )}

        {/* Detailed Breakdown */}
        <div className="space-y-6">
          <h3 className={`text-lg font-semibold ${colours.text} border-b ${colours.border} pb-2`}>
            Cost Breakdown
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className={colours.text}>Customs Value:</span>
                <span className={`font-medium ${colours.text}`}>
                  {formatCurrency(result.customsValue)}
                </span>
              </div>

              <div className="flex justify-between items-center">
                <span className={colours.text}>Base Duty:</span>
                <span className={`font-medium ${colours.text}`}>
                  {formatCurrency(result.baseDuty)}
                </span>
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className={colours.text}>
                  VAT/GST:
                  {result.vatRate && result.vatRate > 0 && (
                    <span className="text-xs ml-1 text-green-600">
                      ({result.vatRate.toFixed(2)}%)
                    </span>
                  )}
                </span>
                <span className={`font-medium ${colours.text}`}>
                  {formatCurrency(result.vatOrGst)}
                </span>
              </div>

              <div className="flex justify-between items-center">
                <span className={colours.text}>Shipping Cost:</span>
                <span className={`font-medium ${colours.text}`}>
                  {formatCurrency(result.shippingCost)}
                </span>
              </div>

              <div className="flex justify-between items-center border-t pt-2">
                <span className={`font-semibold ${colours.text}`}>Total Cost:</span>
                <span className={`font-bold text-lg text-green-600`}>
                  {formatCurrency(result.totalCost)}
                </span>
              </div>
            </div>
          </div>

          {/* Additional Information */}
          <div className={`${colours.inputBg} rounded-lg p-4`}>
            <h4 className={`font-medium ${colours.text} mb-3`}>Additional Information</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <span className={`font-medium ${colours.textSecondary}`}>Trade Status:</span>
                <span className={`ml-2 ${colours.text}`}>{getTradeAgreementStatus()}</span>
              </div>

              {result.TariffType && (
                <div>
                  <span className={`font-medium ${colours.textSecondary}`}>Tariff Type:</span>
                  <span className={`ml-2 ${colours.text}`}>{result.TariffType}</span>
                </div>
              )}

              <div>
                <span className={`font-medium ${colours.textSecondary}`}>Calculation Date:</span>
                <span className={`ml-2 ${colours.text}`}>{formatDate(result.calculationDate)}</span>
              </div>

              {result.totalWeight && (
                <div>
                  <span className={`font-medium ${colours.textSecondary}`}>Total Weight:</span>
                  <span className={`ml-2 ${colours.text}`}>{result.totalWeight} kg</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResultsPage;
