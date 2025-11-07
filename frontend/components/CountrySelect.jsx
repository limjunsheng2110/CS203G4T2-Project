import React, { useState, useEffect, useRef } from 'react';
import { ChevronDown, Search } from 'lucide-react';
import apiService from '../services/apiService';

const CountrySelect = ({ label, name, value, onChange, required, colours }) => {
  const [countries, setCountries] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const dropdownRef = useRef(null);

  // --- (Fetch Countries and Outside Click Hooks remain the same) ---
  useEffect(() => {
    const fetchCountries = async () => {
      setLoading(true);
      setError(null);
      try {
        const countriesData = await apiService.country.getAllCountries();
        setCountries(countriesData);
      } catch (err) {
        let errorMessage = 'Failed to load countries. ';
        if (err.code === 'NETWORK_ERROR' || err.message.includes('Network Error')) {
          errorMessage += 'Network connection failed. Please check your internet connection.';
        } else if (err.response?.status === 404) {
          errorMessage += 'Countries endpoint not found. Please check the API configuration.';
        } else if (err.response?.status >= 500) {
          errorMessage += 'Server error occurred. Please try again later.';
        } else if (err.name === 'AbortError' || err.code === 'ECONNABORTED') {
          errorMessage += 'Request timed out. Please try again.';
        } else {
          errorMessage += `Error: ${err.message || 'Unknown error occurred'}`;
        }
        setError(errorMessage);
        setCountries([]);
      } finally {
        setLoading(false);
      }
    };
    fetchCountries();
  }, []);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // --- (Helper functions remain the same) ---
  const filteredCountries = countries.filter(country =>
      country.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      country.code.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const selectedCountry = countries.find(country => country.code === value);
  const displayValue = selectedCountry ? `${selectedCountry.name} (${selectedCountry.code})` : '';

  const handleCountrySelect = (country) => {
    onChange({
      target: {
        name: name,
        value: country.code
      }
    });
    setIsOpen(false);
    setSearchTerm('');
  };

  return (
      <div className="relative" ref={dropdownRef}>
        <label className={`block text-sm font-medium ${colours.textSecondary} mb-2`}>
          {label} {required && <span className="text-red-500">*</span>}
        </label>

        {/* Main select button (UNTOUCHED - good styling) */}
        <div
            onClick={() => setIsOpen(!isOpen)}
            className={`w-full px-4 py-3 border-2 ${colours.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colours.inputBg} ${colours.text} cursor-pointer transition-all ${colours.borderHover} flex items-center justify-between`}
        >
        <span className={displayValue ? colours.text : colours.textMuted}>
          {displayValue || `Select ${label.toLowerCase()}`}
        </span>
          <ChevronDown
              size={16}
              className={`transition-transform ${isOpen ? 'rotate-180' : ''} ${colours.textMuted}`}
          />
        </div>

        {/* Dropdown - White background with black text */}
        {isOpen && (
            <div className="absolute z-50 w-full mt-1 bg-white border-2 border-gray-200 rounded-lg shadow-xl max-h-60 overflow-hidden">

              {/* Search input container - White background */}
              <div className="sticky top-0 z-10 p-3 border-b border-gray-200 bg-white">
                <div className="relative">
                  <Search size={16} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                  <input
                      type="text"
                      placeholder="Search countries..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-white text-black text-sm"
                      onClick={(e) => e.stopPropagation()}
                  />
                </div>
              </div>

              {/* Options list container - White background */}
              <div className="max-h-40 overflow-y-auto bg-white">
                {loading ? (
                    <div className="p-3 text-center text-gray-500 bg-white">
                      Loading countries...
                    </div>
                ) : error ? (
                    <div className="p-3 text-center text-red-500 text-sm bg-white">
                      {error}
                    </div>
                ) : filteredCountries.length === 0 ? (
                    <div className="p-3 text-center text-gray-500 bg-white">
                      No countries found
                    </div>
                ) : (
                    filteredCountries.map((country) => (
                        <div
                            key={country.code}
                            onClick={() => handleCountrySelect(country)}
                            className={`px-4 py-3 cursor-pointer transition-colors hover:bg-green-50 ${
                                value === country.code
                                    ? 'bg-green-100' // Selected: green highlight
                                    : 'bg-white' // Default: white background
                            } text-black`}
                        >
                          <div className="font-medium">{country.name}</div>
                          <div className="text-sm text-gray-600">{country.code}</div>
                        </div>
                    ))
                )}
              </div>
            </div>
        )}
      </div>
  );
};

export default CountrySelect;
