import React, { useState, useEffect, useRef } from 'react';
import { ChevronDown, Search } from 'lucide-react';
import axios from 'axios';

const CountrySelect = ({ label, name, value, onChange, required, colours }) => {
  const [countries, setCountries] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const dropdownRef = useRef(null);

  // Fetch countries from backend API using axios
  useEffect(() => {
    const fetchCountries = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await axios.get('/api/countries/all');
        setCountries(response.data);
      } catch (err) {
        console.error('Error fetching countries:', err);
        setError('Failed to load countries');
        // Fallback to hardcoded countries if API fails
        setCountries([
          { code: 'US', name: 'United States' },
          { code: 'CN', name: 'China' },
          { code: 'SG', name: 'Singapore' },
          { code: 'MY', name: 'Malaysia' },
          { code: 'GB', name: 'United Kingdom' }
        ]);
      } finally {
        setLoading(false);
      }
    };

    fetchCountries();
  }, []);

  // Close dropdown when clicking outside
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

  // Filter countries based on search term
  const filteredCountries = countries.filter(country =>
    country.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    country.code.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Get selected country display name
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

      {/* Main select button */}
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

      {/* Dropdown */}
      {isOpen && (
        <div className={`absolute z-50 w-full mt-1 ${colours.cardBg} border-2 ${colours.border} rounded-lg shadow-lg max-h-60 overflow-hidden`}>
          {/* Search input */}
          <div className="p-3 border-b border-gray-200 dark:border-gray-700">
            <div className="relative">
              <Search size={16} className={`absolute left-3 top-1/2 transform -translate-y-1/2 ${colours.textMuted}`} />
              <input
                type="text"
                placeholder="Search countries..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className={`w-full pl-10 pr-4 py-2 border ${colours.border} rounded-md focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colours.inputBg} ${colours.text} text-sm`}
                onClick={(e) => e.stopPropagation()}
              />
            </div>
          </div>

          {/* Options list */}
          <div className="max-h-40 overflow-y-auto">
            {loading ? (
              <div className={`p-3 text-center ${colours.textMuted}`}>
                Loading countries...
              </div>
            ) : error ? (
              <div className="p-3 text-center text-red-500 text-sm">
                {error}
              </div>
            ) : filteredCountries.length === 0 ? (
              <div className={`p-3 text-center ${colours.textMuted}`}>
                No countries found
              </div>
            ) : (
              filteredCountries.map((country) => (
                <div
                  key={country.code}
                  onClick={() => handleCountrySelect(country)}
                  className={`px-4 py-3 cursor-pointer transition-colors hover:bg-green-50 dark:hover:bg-green-900/20 ${
                    value === country.code ? 'bg-green-100 dark:bg-green-800/30' : ''
                  } ${colours.text}`}
                >
                  <div className="font-medium">{country.name}</div>
                  <div className={`text-sm ${colours.textMuted}`}>{country.code}</div>
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