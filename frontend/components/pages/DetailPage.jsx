import React from 'react';
import { Search, ArrowLeft, Info } from 'lucide-react';
import { getThemeColours } from '../../utils/themeColours';
import FormField from '../forms/FormField';
import CountrySelect from '../forms/CountrySelect';
import ProductSelect from '../forms/ProductSelect';

const DetailPage = ({
  formData,
  selectedProduct,
  handleInputChange,
  handleSearch,
  onBack,
  isLoading,
  error,
  chatbotContext,
  theme,
  toggleTheme,
}) => {
  const colours = getThemeColours();

  return (
    <div className="py-8 px-4">
      <div className="max-w-5xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <button
            onClick={onBack}
            disabled={isLoading}
            className={`flex items-center gap-2 text-purple-400 hover:text-purple-300 font-medium ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            <ArrowLeft size={20} />
            Back to Home
          </button>
        </div>

        <div className="text-center mb-6">
          <div className="flex justify-center">
            <img 
              src="/TariffNomLogo.png"
              alt="TariffNom Logo"
              className="w-[768px] h-auto"
            />
          </div>
        </div>

        <div className={`${colours.cardBg} rounded-lg shadow-lg p-8 border ${colours.border}`}>
          <h2 className={`text-2xl font-semibold ${colours.text} mb-2`}>Enter Transaction Information</h2>
          <p className={`${colours.textMuted} mb-6`}>Please provide details about your trade transaction</p>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-600 text-sm font-medium mb-1">Error</p>
              <p className="text-red-600 text-sm">{error}</p>
              {error.includes('timeout') && (
                <p className="text-red-500 text-xs mt-2">
                  The tariff calculation is taking longer than expected. This might happen when scraping new data. Please try again.
                </p>
              )}
            </div>
          )}

          {/* Loading Message with Progress Indicator */}
          {isLoading && (
            <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-center gap-3">
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
                <div>
                  <p className="text-blue-600 text-sm font-medium">Calculating Tariffs...</p>
                  <p className="text-blue-500 text-xs mt-1">
                    This may take up to 1 minute if we need to scrape new data from external sources.
                  </p>
                </div>
              </div>
            </div>
          )}

          <div className="space-y-5">
            <CountrySelect
              label="Import Country"
              name="importCountry"
              value={formData.importCountry}
              onChange={handleInputChange}
              required={true}
              colours={colours}
            />

            <CountrySelect
              label="Export Country"
              name="exportCountry"
              value={formData.exportCountry}
              onChange={handleInputChange}
              required={true}
              colours={colours}
            />

            {/* Chatbot context notice */}
            {chatbotContext && chatbotContext.hsCode === formData.hsCode && (
              <div className="p-3 bg-purple-500/10 border border-purple-400/40 rounded-lg text-sm text-white">
                <div className="flex items-start gap-2">
                  <Info className="w-4 h-4 mt-0.5 text-purple-200" />
                  <div>
                    <p className="font-semibold text-purple-100">
                      HS code {chatbotContext.hsCode} suggested by assistant
                      {chatbotContext.confidence != null &&
                        ` (confidence ${(chatbotContext.confidence * 100).toFixed(0)}%)`}
                    </p>
                    {chatbotContext.rationale && (
                      <p className="text-xs text-purple-100 mt-1 leading-relaxed">
                        {chatbotContext.rationale}
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Warning if same country selected */}
            {formData.importCountry && formData.exportCountry && 
             formData.importCountry === formData.exportCountry && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-600 text-sm font-medium">
                  ⚠️ Import and export countries cannot be the same
                </p>
              </div>
            )}

            <ProductSelect
              label="Product (HS Code)"
              name="hsCode"
              value={formData.hsCode}
              onChange={handleInputChange}
              required={true}
              colours={colours}
            />

            {/* Display selected product information */}
            {selectedProduct && (
              <div className={`p-3 ${colours.inputBg} border ${colours.border} rounded-lg`}>
                <div className="flex items-start gap-2">
                  <div className="flex-1">
                    <p className={`text-sm font-medium ${colours.text}`}>Selected Product:</p>
                    <p className={`text-xs ${colours.textMuted} mt-1`}>{selectedProduct.description}</p>
                    {selectedProduct.category && (
                      <p className={`text-xs ${colours.textMuted}`}>Category: {selectedProduct.category}</p>
                    )}
                  </div>
                </div>
              </div>
            )}

            <FormField
              label="Total Value (USD)"
              name="value"
              type="number"
              value={formData.value}
              onChange={handleInputChange}
              placeholder="Enter total value (must be positive)"
              required={true}
              colours={colours}
            />
            {formData.value && parseFloat(formData.value) <= 0 && (
              <p className="text-red-500 text-sm mt-1">Value must be greater than 0</p>
            )}

            <FormField
              label="Weight (kg)"
              name="weight"
              type="number"
              value={formData.weight}
              onChange={handleInputChange}
              placeholder="Enter weight in kilograms"
              required={true}
              colours={colours}
            />
            {formData.weight && parseFloat(formData.weight) <= 0 && (
              <p className="text-red-500 text-sm mt-1">Weight must be greater than 0</p>
            )}

            <FormField
              label="Year"
              name="year"
              type="number"
              value={formData.year}
              onChange={handleInputChange}
              placeholder="e.g., 2025"
              required={false}
              colours={colours}
            />

            <div>
              <label className={`block text-sm font-medium ${colours.labelText} mb-2`}>
                Shipping Mode <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <select
                  name="shippingMode"
                  value={formData.shippingMode}
                  onChange={handleInputChange}
                  className={`w-full px-4 py-3 border-2 ${colours.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colours.inputBg} ${colours.text} appearance-none cursor-pointer transition-all ${colours.borderHover}`}
                >
                  <option value="">Select shipping mode</option>
                  <option value="air">Air</option>
                  <option value="sea">Sea</option>
                </select>
                <div className={`pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 ${colours.textMuted}`}>
                  <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                    <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>

          <div className="flex justify-end mt-8">
            <button
              onClick={handleSearch}
              disabled={isLoading}
              className={`px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-2 font-medium shadow-md hover:shadow-lg ${isLoading ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
              {isLoading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  Calculating...
                </>
              ) : (
                <>
                  <Search size={18} />
                  Search Tariffs
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DetailPage;

