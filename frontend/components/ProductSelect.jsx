import React, { useState, useEffect, useRef } from 'react';
import { ChevronDown, Search, Package } from 'lucide-react';
import apiService from '../services/apiService';

const ProductSelect = ({ label, name, value, onChange, required, colours }) => {
  const [products, setProducts] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const dropdownRef = useRef(null);

  // Fetch products from backend API using centralized API service
  useEffect(() => {
    const fetchProducts = async () => {
      setLoading(true);
      setError(null);
      try {
        console.log('Fetching products from API...');
        const productsData = await apiService.product.getAllProducts();
        console.log('Products fetched successfully:', productsData.length, 'products');
        setProducts(productsData);
      } catch (err) {
        console.error('Error fetching products:', err);

        // More detailed error message based on error type
        let errorMessage = 'Failed to load products. ';

        if (err.code === 'NETWORK_ERROR' || err.message.includes('Network Error')) {
          errorMessage += 'Network connection failed. Please check your internet connection.';
        } else if (err.response?.status === 404) {
          errorMessage += 'Products endpoint not found. Please check the API configuration.';
        } else if (err.response?.status >= 500) {
          errorMessage += 'Server error occurred. Please try again later.';
        } else if (err.name === 'AbortError' || err.code === 'ECONNABORTED') {
          errorMessage += 'Request timed out. Please try again.';
        } else {
          errorMessage += `Error: ${err.message || 'Unknown error occurred'}`;
        }

        setError(errorMessage);
        setProducts([]); // Clear products on error
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
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

  // Filter products based on search term
  const filteredProducts = products.filter(product =>
    product.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
    product.hsCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (product.category && product.category.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  // Get selected product display name
  const selectedProduct = products.find(product => product.hsCode === value);
  const displayValue = selectedProduct ?
    `${selectedProduct.hsCode} - ${selectedProduct.description}` : '';

  const handleProductSelect = (product) => {
    onChange({
      target: {
        name: name,
        value: product.hsCode
      }
    });
    setIsOpen(false);
    setSearchTerm('');
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <label className="block text-sm font-medium text-white mb-2">
        {label} {required && <span className="text-red-500">*</span>}
      </label>

      {/* Main select button */}
      <div
        onClick={() => setIsOpen(!isOpen)}
        className={`w-full px-4 py-3 border-2 ${colours.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colours.inputBg} ${colours.text} cursor-pointer transition-all ${colours.borderHover} flex items-center justify-between`}
      >
        <span className={displayValue ? colours.text : colours.textMuted}>
          {displayValue || `Select Product (e.g 010329)`}
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
                placeholder="Search by HS code, description, or category..."
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
                Loading products...
              </div>
            ) : error ? (
              <div className="p-3 text-center text-red-500 text-sm bg-white">
                {error}
              </div>
            ) : filteredProducts.length === 0 ? (
              <div className="p-3 text-center text-gray-500 bg-white">
                No products found
              </div>
            ) : (
              filteredProducts.map((product) => (
                <div
                  key={product.hsCode}
                  onClick={() => handleProductSelect(product)}
                  className={`px-4 py-3 cursor-pointer transition-colors hover:bg-green-50 ${
                    value === product.hsCode ? 'bg-green-100' : 'bg-white'
                  } text-black`}
                >
                  <div className="flex items-start gap-2">
                    <Package size={16} className="mt-0.5 text-gray-400" />
                    <div className="flex-1 min-w-0">
                      <div className="font-medium truncate">{product.hsCode}</div>
                      <div className="text-sm text-gray-700 line-clamp-2">{product.description}</div>
                      {product.category && (
                        <div className="text-xs text-gray-500 mt-1">{product.category}</div>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductSelect;
