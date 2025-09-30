import React from 'react';
import { Search } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';
import ThemeToggle from './ThemeToggle';
import FormField from './FormField';
import CountrySelect from './CountrySelect';

const DetailPage = ({ formData, handleInputChange, handleSearch, theme, toggleTheme }) => {
  const colors = getThemeColours(theme);

  return (
    <div className={`min-h-screen ${theme === 'light' ? 'bg-amber-50' : 'bg-black'} py-8 px-4`}>
      <div className="max-w-2xl mx-auto">
        <div className="flex justify-end mb-4">
          <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
        </div>
        
        <div className="text-center mb-8">
          <h1 className={`text-4xl font-bold ${colors.text} mb-2`}>T.A.R.I.F.F</h1>
          <p className={colors.textSecondary}>Transaction Details</p>
        </div>

        <div className={`${colors.cardBg} rounded-lg shadow-lg p-8 border ${colors.border}`}>
          <h2 className={`text-2xl font-semibold ${colors.text} mb-2`}>Enter Transaction Information</h2>
          <p className={`${colors.textMuted} mb-6`}>Please provide details about your trade transaction</p>

          <div className="space-y-5">
            <CountrySelect
              label="Import Country"
              name="importCountry"
              value={formData.importCountry}
              onChange={handleInputChange}
              required={true}
              colors={colors}
            />

            <CountrySelect
              label="Export Country"
              name="exportCountry"
              value={formData.exportCountry}
              onChange={handleInputChange}
              required={true}
              colors={colors}
            />

            <FormField
              label="HS Code"
              name="hsCode"
              type="text"
              value={formData.hsCode}
              onChange={handleInputChange}
              placeholder="Enter HS code (e.g., 8517.12.00)"
              required={true}
              colors={colors}
            />

            <div>
              <label className={`block text-sm font-medium ${colors.textSecondary} mb-2`}>
                Value Type <span className="text-red-500">*</span>
              </label>
              <div className="flex gap-4 mb-3">
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="valueType"
                    value="price"
                    checked={formData.valueType === 'price'}
                    onChange={handleInputChange}
                    className="mr-2"
                  />
                  <span className={colors.textSecondary}>Total Value (USD)</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="valueType"
                    value="quantity"
                    checked={formData.valueType === 'quantity'}
                    onChange={handleInputChange}
                    className="mr-2"
                  />
                  <span className={colors.textSecondary}>Quantity & Unit</span>
                </label>
              </div>

              {formData.valueType === 'price' ? (
                <FormField
                  name="value"
                  type="number"
                  value={formData.value}
                  onChange={handleInputChange}
                  placeholder="Enter total value"
                  colors={colors}
                />
              ) : (
                <div className="grid grid-cols-2 gap-3">
                  <FormField
                    name="quantity"
                    type="number"
                    value={formData.quantity}
                    onChange={handleInputChange}
                    placeholder="Enter quantity"
                    colors={colors}
                  />
                  <div className="relative">
                    <select
                      name="unit"
                      value={formData.unit}
                      onChange={handleInputChange}
                      className={`w-full px-4 py-3 border-2 ${colors.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colors.inputBg} ${colors.text} appearance-none cursor-pointer transition-all ${colors.borderHover}`}
                    >
                      <option value="">Select unit</option>
                      <option value="kg">Kilograms (kg)</option>
                      <option value="tons">Tons</option>
                      <option value="pieces">Pieces</option>
                      <option value="litres">Litres</option>
                    </select>
                    <div className={`pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 ${colors.textMuted}`}>
                      <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                        <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"/>
                      </svg>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div>
              <label className={`block text-sm font-medium ${colors.textSecondary} mb-2`}>
                Shipping Mode (Optional)
              </label>
              <div className="relative">
                <select
                  name="shippingMode"
                  value={formData.shippingMode}
                  onChange={handleInputChange}
                  className={`w-full px-4 py-3 border-2 ${colors.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colors.inputBg} ${colors.text} appearance-none cursor-pointer transition-all ${colors.borderHover}`}
                >
                  <option value="">Select shipping mode</option>
                  <option value="air">Air</option>
                  <option value="sea">Sea</option>
                  <option value="land">Land</option>
                </select>
                <div className={`pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 ${colors.textMuted}`}>
                  <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
                    <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>

          <div className="flex justify-between mt-8">
            <button
              onClick={() => alert('Back functionality')}
              className={`px-6 py-3 border-2 ${colors.buttonBorder} rounded-lg ${colors.textSecondary} ${colors.buttonBorderHover} transition-colors font-medium`}
            >
              Back
            </button>
            <button
              onClick={handleSearch}
              className="px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2 font-medium shadow-md hover:shadow-lg"
            >
              <Search size={18} />
              Search Tariffs
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DetailPage;