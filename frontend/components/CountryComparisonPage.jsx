// CountryComparisonPage.jsx
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { ArrowLeft, TrendingDown, TrendingUp, DollarSign } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';
import ThemeToggle from './ThemeToggle';

const CountryComparisonPage = ({ formData, handleBack, theme, toggleTheme }) => {
  const colors = getThemeColours(theme);
  const [comparisonData, setComparisonData] = useState(null);
  const [loading, setLoading] = useState(false);

  const compareCountries = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/tariff/compare', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sourceCountries: formData.sourceCountries,
          destinationCountry: formData.destinationCountry,
          preferredCurrency: formData.preferredCurrency || 'SGD',
          productName: formData.productName,
          hsCode: formData.hsCode,
          productValue: formData.productValue,
          quantity: formData.quantity,
          unit: formData.unit,
          shippingMode: formData.shippingMode,
        }),
      });

      const data = await response.json();
      setComparisonData(data);
    } catch (error) {
      console.error('Error comparing countries:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    compareCountries();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatCurrency = (amount, currency) =>
    new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'SGD',
    }).format(amount);

  if (loading) {
    return (
      <div className={`min-h-screen ${colors.resultBg} flex items-center justify-center`}>
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-green-600 mx-auto mb-4"></div>
          <p className={colors.text}>Comparing prices across countries...</p>
        </div>
      </div>
    );
  }

  if (!comparisonData) return null;

  const { recommendedCountry, recommendedCountryName, lowestTotalCost, currency, countryOptions } =
    comparisonData;

  return (
    <div className={`min-h-screen ${colors.resultBg} py-8 px-4`}>
      <div className="max-w-6xl mx-auto">
        {/* Header */}
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

        {/* Recommendation Banner */}
        <div className="bg-gradient-to-r from-green-500 to-emerald-600 rounded-lg p-6 mb-8 text-white shadow-lg">
          <div className="flex items-center gap-3 mb-2">
            <TrendingDown size={32} />
            <h1 className="text-3xl font-bold">Best Deal Found!</h1>
          </div>
          <p className="text-lg mb-4">
            Buy from <span className="font-bold text-2xl">{recommendedCountryName}</span> for the
            lowest total cost
          </p>
          <div className="bg-white/20 backdrop-blur rounded-lg p-4 inline-block">
            <p className="text-sm opacity-90 mb-1">Total Cost</p>
            <p className="text-4xl font-bold">{formatCurrency(lowestTotalCost, currency)}</p>
          </div>
        </div>

        {/* Comparison Table */}
        <div className={`${colors.cardBg} rounded-lg shadow-lg overflow-hidden mb-8`}>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-green-50 border-b-2 border-green-200">
                <tr>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Rank</th>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-700">Country</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">
                    Product Value
                  </th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">Tariff</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">
                    Shipping
                  </th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">
                    Total Cost
                  </th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">
                    Exchange Rate
                  </th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-700">
                    Savings
                  </th>
                </tr>
              </thead>
              <tbody>
                {countryOptions.map((option) => (
                  <tr
                    key={option.countryCode}
                    className={`border-b ${
                      option.ranking === 1 ? 'bg-green-50' : ''
                    } hover:bg-gray-50 transition-colors`}
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        {option.ranking === 1 && (
                          <span className="bg-green-500 text-white px-2 py-1 rounded-full text-xs font-bold">
                            BEST
                          </span>
                        )}
                        <span className="font-semibold text-gray-600">#{option.ranking}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-semibold text-gray-900">{option.countryName}</p>
                        <p className="text-xs text-gray-500">{option.countryCode}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <p className="font-medium text-gray-900">
                        {formatCurrency(option.productValueConverted, currency)}
                      </p>
                      <p className="text-xs text-gray-500">
                        ({formatCurrency(option.productValueInOriginalCurrency, option.originalCurrency)})
                      </p>
                    </td>
                    <td className="px-6 py-4 text-right font-medium text-orange-600">
                      {formatCurrency(option.tariffAmount, currency)}
                    </td>
                    <td className="px-6 py-4 text-right font-medium text-blue-600">
                      {formatCurrency(option.shippingCost, currency)}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <p className="text-lg font-bold text-gray-900">
                        {formatCurrency(option.totalCost, currency)}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {option.exchangeRate > 1 ? (
                          <TrendingUp size={14} className="text-red-500" />
                        ) : (
                          <TrendingDown size={14} className="text-green-500" />
                        )}
                        <span className="text-sm text-gray-600">
                          {option.exchangeRate.toFixed(4)}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500">
                        {option.originalCurrency} → {currency}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {option.ranking > 1 ? (
                        <span className="text-sm font-semibold text-red-600">
                          +{formatCurrency(option.savingsVsExpensive, currency)}
                        </span>
                      ) : (
                        <span className="text-sm font-semibold text-green-600">Cheapest</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Detailed Cards View */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
          {countryOptions.map((option) => (
            <div
              key={option.countryCode}
              className={`${colors.cardBg} rounded-lg shadow-md p-6 border-2 ${
                option.ranking === 1 ? 'border-green-500' : 'border-transparent'
              }`}
            >
              {option.ranking === 1 && (
                <div className="bg-green-500 text-white px-3 py-1 rounded-full text-xs font-bold inline-block mb-3">
                  RECOMMENDED
                </div>
              )}

              <h3 className="text-xl font-bold text-gray-900 mb-1">{option.countryName}</h3>
              <p className="text-sm text-gray-500 mb-4">Rank #{option.ranking}</p>

              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Product Value:</span>
                  <span className="font-semibold text-gray-900">
                    {formatCurrency(option.productValueConverted, currency)}
                  </span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Tariff:</span>
                  <span className="font-semibold text-orange-600">
                    {formatCurrency(option.tariffAmount, currency)}
                  </span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Shipping:</span>
                  <span className="font-semibold text-blue-600">
                    {formatCurrency(option.shippingCost, currency)}
                  </span>
                </div>

                <div className="border-t pt-3 flex justify-between items-center">
                  <span className="text-sm font-semibold text-gray-700">Total Cost:</span>
                  <span className="text-xl font-bold text-gray-900">
                    {formatCurrency(option.totalCost, currency)}
                  </span>
                </div>

                <div className="bg-gray-50 rounded p-3 mt-3">
                  <p className="text-xs text-gray-600 mb-1">Exchange Rate</p>
                  <p className="text-sm font-medium text-gray-800">
                    1 {option.originalCurrency} = {option.exchangeRate.toFixed(4)} {currency}
                  </p>
                </div>

                {option.ranking > 1 && (
                  <div className="bg-red-50 border border-red-200 rounded p-2 mt-2">
                    <p className="text-xs text-red-700">
                      {formatCurrency(option.savingsVsExpensive, currency)} more expensive than
                      cheapest option
                    </p>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Info Box */}
        <div className={`${colors.infoBg} border ${colors.infoBorder} rounded-lg p-6`}>
          <div className="flex items-start gap-3">
            <DollarSign className="text-green-600 mt-1" size={24} />
            <div>
              <h4 className="font-semibold text-gray-900 mb-2">How This Comparison Works</h4>
              <ul className="text-sm text-gray-700 space-y-2">
                <li>• Exchange rates are fetched in real-time and updated every 24 hours</li>
                <li>• All costs are converted to {currency} for easy comparison</li>
                <li>• Tariff rates include any applicable trade agreements (FTA)</li>
                <li>• Shipping costs vary by country distance and selected mode</li>
                <li>• The recommended country offers the lowest total landed cost</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

CountryComparisonPage.propTypes = {
  formData: PropTypes.shape({
    sourceCountries: PropTypes.arrayOf(PropTypes.string).isRequired,
    destinationCountry: PropTypes.string.isRequired,
    preferredCurrency: PropTypes.string,
    productName: PropTypes.string,
    hsCode: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    productValue: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    quantity: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    unit: PropTypes.string,
    shippingMode: PropTypes.string,
  }).isRequired,
  handleBack: PropTypes.func.isRequired,
  theme: PropTypes.string.isRequired,
  toggleTheme: PropTypes.func.isRequired,
};

export default CountryComparisonPage;
