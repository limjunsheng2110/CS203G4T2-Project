import axios from 'axios';

// Base API configuration
// Use environment variable for backend URL, fallback to /api for local development
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000, // 60 second (1 minute) timeout to accommodate scraping operations
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for adding auth tokens if needed
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for handling auth errors
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // If error is 401 or 403 (token expired/invalid), logout and redirect to login
    const isAuthError = error.response?.status === 401 || error.response?.status === 403;
    
    // Don't redirect to login if it's a validation error (400) from chatbot
    const isChatbotValidationError = 
      error.response?.status === 400 && 
      error.config?.url?.includes('/hs/resolve');
    
    if (isAuthError && !isChatbotValidationError) {
      // Clear auth data
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      
      // Redirect to login page with session expired flag
      window.location.href = '/?sessionExpired=true';
      
      return Promise.reject(error);
    }

    return Promise.reject(error);
  }
);

// Country API endpoints
export const countryApi = {
  /**
   * Get all available countries
   * @returns {Promise} Promise resolving to array of countries
   */
  getAllCountries: async () => {
    const response = await apiClient.get('/countries/all');
    // Transform backend data structure to match frontend expectations
    return response.data.map(country => ({
      code: country.countryCode,
      name: country.countryName
    }));
  },

  /**
   * Get country by code
   * @param {string} countryCode - ISO country code
   * @returns {Promise} Promise resolving to country data
   */
  getCountryByCode: async (countryCode) => {
    const response = await apiClient.get(`/countries/${countryCode}`);
    // Transform backend data structure to match frontend expectations
    return {
      code: response.data.countryCode,
      name: response.data.countryName
    };
  }
};

// Exchange Rate API endpoints
export const exchangeRateApi = {
  /**
   * Get exchange rate analysis between two countries
   * @param {string} importingCountry - Importing country code
   * @param {string} exportingCountry - Exporting country code
   * @returns {Promise} Promise resolving to exchange rate analysis
   */
  getExchangeRateAnalysis: async (importingCountry, exportingCountry) => {
    if (!importingCountry || !exportingCountry) {
      throw new Error('Please select both importing and exporting countries');
    }

    try {
      const response = await apiClient.post('/exchange-rates/analyze', {
        importingCountry,
        exportingCountry
      });
      return response.data;
    } catch (error) {
      const errorMessage = error.response?.data?.details || 'Failed to fetch exchange rate analysis';
      throw new Error(errorMessage);
    }
  },

  /**
   * Get current exchange rate between two currencies
   * @param {string} fromCurrency - Source currency code
   * @param {string} toCurrency - Target currency code
   * @returns {Promise} Promise resolving to exchange rate data
   */
  getCurrentExchangeRate: async (fromCurrency, toCurrency) => {
    const response = await apiClient.get('/exchange-rates/current', {
      params: {
        from: fromCurrency,
        to: toCurrency
      }
    });
    return response.data;
  }
};

// Tariff API endpoints
export const tariffApi = {
  /**
   * Calculate tariff costs
   * @param {Object} calculationParams - Calculation parameters
   * @returns {Promise} Promise resolving to calculated costs
   */
  calculateTariff: async (calculationParams) => {
    try {
      const response = await apiClient.post('/tariff/calculate', {
        importingCountry: calculationParams.importCountry,
        exportingCountry: calculationParams.exportCountry,
        hsCode: calculationParams.hsCode,
        productValue: parseFloat(calculationParams.value),
        weight: parseFloat(calculationParams.weight),
        shippingMode: calculationParams.shippingMode || null,
        year: calculationParams.year ? parseInt(calculationParams.year) : null
      });
      return response.data;
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data?.details || 'Failed to calculate tariff';
      throw new Error(errorMessage);
    }
  },

  /**
   * Search for tariff information
   * @param {Object} searchParams - Search parameters
   * @returns {Promise} Promise resolving to tariff search results
   */
  searchTariffs: async (searchParams) => {
    const response = await apiClient.post('/tariffs/search', searchParams);
    return response.data;
  },

  /**
   * Get tariff details by ID
   * @param {string} tariffId - Tariff ID
   * @returns {Promise} Promise resolving to tariff details
   */
  getTariffById: async (tariffId) => {
    const response = await apiClient.get(`/tariffs/${tariffId}`);
    return response.data;
  }
};

// Product API endpoints
export const productApi = {
  /**
   * Get all available products
   * @returns {Promise} Promise resolving to array of products
   */
  getAllProducts: async () => {
    const response = await apiClient.get('/products/all');
    // Transform backend data structure to match frontend expectations
    return response.data.map(product => ({
      hsCode: product.hsCode,
      description: product.description,
      category: product.category || 'Uncategorized'
    }));
  },

  /**
   * Search for products
   * @param {string} query - Search query
   * @returns {Promise} Promise resolving to product search results
   */
  searchProducts: async (query) => {
    const response = await apiClient.get('/products/search', {
      params: { q: query }
    });
    return response.data;
  },

  /**
   * Get product by HS Code
   * @param {string} hsCode - HS Code
   * @returns {Promise} Promise resolving to product data
   */
  getProductByHsCode: async (hsCode) => {
    const response = await apiClient.get(`/products/${hsCode}`);
    return {
      hsCode: response.data.hsCode,
      description: response.data.description,
      category: response.data.category || 'Uncategorized'
    };
  }
};

// Chatbot / HS Resolver endpoints
export const chatbotApi = {
  /**
   * Send message to RAG-enabled conversational chatbot
   * @param {Object} payload - Chat message payload { message, sessionId, consentLogging }
   * @returns {Promise} Promise resolving to chat response
   */
  sendMessage: async (payload) => {
    try {
      const response = await apiClient.post('/chat/message', payload);
      return response.data;
    } catch (error) {
      const errorMessage =
        error.response?.data?.message ||
        error.response?.data?.details ||
        'Unable to process your message at the moment';
      const customError = new Error(errorMessage);
      customError.code = error.response?.status;
      throw customError;
    }
  },

  /**
   * Resolve HS code suggestions for a given product description (legacy keyword-based)
   * @param {Object} payload - HS resolver request payload
   * @returns {Promise} Promise resolving to HS resolver response
   */
  resolveHsCode: async (payload) => {
    try {
      const response = await apiClient.post('/hs/resolve', payload);
      return response.data;
    } catch (error) {
      const errorMessage =
        error.response?.data?.details ||
        error.response?.data?.message ||
        'Unable to resolve HS code at the moment';
      const customError = new Error(errorMessage);
      customError.code = error.response?.status;
      throw customError;
    }
  }
};

// Scraping API endpoints
export const scrapingApi = {
  /**
   * Trigger scraping for specific parameters
   * @param {Object} scrapingParams - Scraping parameters
   * @returns {Promise} Promise resolving to scraping results
   */
  triggerScraping: async (scrapingParams) => {
    const response = await apiClient.post('/scraping/trigger', scrapingParams);
    return response.data;
  },

  /**
   * Get scraping status
   * @param {string} jobId - Scraping job ID
   * @returns {Promise} Promise resolving to scraping status
   */
  getScrapingStatus: async (jobId) => {
    const response = await apiClient.get(`/scraping/status/${jobId}`);
    return response.data;
  }
};

// Search History API endpoints
export const searchHistoryApi = {
  /**
   * Get user's search history
   * @returns {Promise} Promise resolving to search history
   */
  getSearchHistory: async () => {
    const response = await apiClient.get('/search-history');
    return response.data;
  },

  /**
   * Save search to history
   * @param {Object} searchData - Search data to save
   * @returns {Promise} Promise resolving to saved search
   */
  saveSearch: async (searchData) => {
    const response = await apiClient.post('/search-history', searchData);
    return response.data;
  },

  /**
   * Delete search from history
   * @param {string} searchId - Search ID to delete
   * @returns {Promise} Promise resolving to deletion confirmation
   */
  deleteSearch: async (searchId) => {
    const response = await apiClient.delete(`/search-history/${searchId}`);
    return response.data;
  }
};

// Auth API endpoints (if needed)
export const authApi = {
  /**
   * User login
   * @param {Object} credentials - User credentials
   * @returns {Promise} Promise resolving to auth data
   */
  login: async (credentials) => {
    const response = await apiClient.post('/auth/login', credentials);
    return response.data;
  },

  /**
   * User registration
   * @param {Object} userData - User registration data
   * @returns {Promise} Promise resolving to registration result
   */
  register: async (userData) => {
    const response = await apiClient.post('/auth/register', userData);
    return response.data;
  },

  /**
   * User logout
   * @returns {Promise} Promise resolving to logout result
   */
  logout: async () => {
    const response = await apiClient.post('/auth/logout');
    localStorage.removeItem('authToken');
    return response.data;
  }
};

// Default export with all API modules
const apiService = {
  country: countryApi,
  exchangeRate: exchangeRateApi,
  tariff: tariffApi,
  product: productApi,
  scraping: scrapingApi,
  searchHistory: searchHistoryApi,
  auth: authApi,
  chatbot: chatbotApi,
};

export default apiService;
