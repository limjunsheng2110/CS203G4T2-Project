// frontend/App.jsx

import React, { useState, useEffect } from 'react';
import HomePage from './components/pages/HomePage';
import DetailPage from './components/pages/DetailPage';
import ResultsPage from './components/pages/ResultsPage';
import LoginPage from './components/pages/LoginPage';
import RegisterPage from './components/pages/RegisterPage';
import UserInfo from './components/common/UserInfo';
import AdminDashboard from './components/pages/AdminDashboard';
import apiService from './services/apiService';

const App = () => {
  const [currentPage, setCurrentPage] = useState('login');
  const [authPage, setAuthPage] = useState('login'); // 'login' or 'register'
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tariffResults, setTariffResults] = useState(null);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [formData, setFormData] = useState({
    importCountry: '',
    exportCountry: '',
    hsCode: '',
    valueType: 'price',
    value: '',
    quantity: '',
    unit: '',
    year: '',
    shippingMode: ''
  });

  // Check for existing auth on mount
  useEffect(() => {
    const token = localStorage.getItem('authToken');
    const savedUser = localStorage.getItem('user');

    if (token && savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        setUser(userData);
        setCurrentPage('home');
      } catch (err) {
        console.error('Error parsing saved user data:', err);
        localStorage.removeItem('authToken');
        localStorage.removeItem('user');
      }
    }
  }, []);

  // Check if session expired (from URL query parameter)
  const urlParams = new URLSearchParams(window.location.search);
  const sessionExpired = urlParams.get('sessionExpired') === 'true';

  const handleLoginSuccess = (userData) => {
    setUser(userData);
    setCurrentPage('home');
    // Clear the sessionExpired query parameter from URL
    window.history.replaceState({}, document.title, '/');
  };

  const handleRegisterSuccess = (userData) => {
    setUser(userData);
    setCurrentPage('home');
  };

  const handleLogout = () => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('user');
    setUser(null);
    setCurrentPage('login');
    setAuthPage('login');
    setTariffResults(null);
    setFormData({
      importCountry: '',
      hsCode: '',
      valueType: 'price',
      value: '',
      quantity: '',
      unit: '',
      year: '',
      shippingMode: ''
    });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // If hsCode is changed, fetch and store the product information
    if (name === 'hsCode' && value) {
      fetchProductInfo(value);
    } else if (name === 'hsCode' && !value) {
      setSelectedProduct(null);
    }
  };

  const fetchProductInfo = async (hsCode) => {
    try {
      const product = await apiService.product.getProductByHsCode(hsCode);
      setSelectedProduct(product);
    } catch (error) {
      console.error('Error fetching product info:', error);
      setSelectedProduct(null);
    }
  };

  const handleGetStarted = () => {
    setCurrentPage('detail');
  };

  const handleSearch = async () => {
    // Validate required fields
    if (!formData.importCountry || !formData.exportCountry || !formData.hsCode || !formData.value) {
      setError('Please fill in all required fields');
      return;
    }

    // Validate import and export countries are different
    if (formData.importCountry === formData.exportCountry) {
      setError('Import country and export country cannot be the same');
      return;
    }

    // Validate value is positive
    const valueNum = parseFloat(formData.value);
    if (isNaN(valueNum) || valueNum <= 0) {
      setError('Product value must be greater than 0');
      return;
    }

    // Validate shipping mode is selected
    if (!formData.shippingMode) {
      setError('Please select a shipping mode (Air or Sea)');
      return;
    }

    // Validate year if provided
    if (formData.year) {
      const yearNum = parseInt(formData.year);
      const currentYear = new Date().getFullYear();
      if (isNaN(yearNum) || yearNum < 2000 || yearNum > currentYear + 1) {
        setError(`Year must be between 2000 and ${currentYear + 1}`);
        return;
      }
    }

    setIsLoading(true);
    setError(null);

    try {
      console.log('Submitting tariff calculation request:', formData);

      // Call the tariff calculation API
      const results = await apiService.tariff.calculateTariff({
        importCountry: formData.importCountry,
        exportCountry: formData.exportCountry,
        hsCode: formData.hsCode,
        value: formData.value,
        year: formData.year ? parseInt(formData.year) : null,
        shippingMode: formData.shippingMode
      });

      console.log('Tariff calculation results:', results);

      // Store the results and navigate to results page
      setTariffResults(results);
      setCurrentPage('results');
    } catch (err) {
      console.error('Error calculating tariff:', err);
      setError(err.message || 'Failed to calculate tariff. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleBack = () => {
    setCurrentPage('detail');
    setError(null);
  };

  const handleBackToHome = () => {
    setCurrentPage('home');
    setError(null);
    setTariffResults(null);
  };

  const handleAdminClick = () => {
    setCurrentPage('admin');
  };

  // Render authentication pages
  if (!user) {
    if (authPage === 'login') {
      return (
        <LoginPage
          onLoginSuccess={handleLoginSuccess}
          onSwitchToRegister={() => setAuthPage('register')}
          sessionExpired={sessionExpired}
        />
      );
    } else {
      return (
        <RegisterPage
          onRegisterSuccess={handleRegisterSuccess}
          onSwitchToLogin={() => setAuthPage('login')}
        />
      );
    }
  }

  // Main app content (only shown when authenticated)
  return (
    <div className="min-h-screen bg-gradient-to-br from-black via-purple-950 to-black">
      <div className="container mx-auto px-4 py-6">
        {/* User Info Component - shows at top of all pages */}
        <UserInfo user={user} onLogout={handleLogout} onAdminClick={handleAdminClick} />

        {currentPage === 'home' && (
          <HomePage 
            onGetStarted={handleGetStarted}
          />
        )}

        {currentPage === 'detail' && (
          <DetailPage
            formData={formData}
            handleInputChange={handleInputChange}
            handleSearch={handleSearch}
            onBack={handleBackToHome}
            isLoading={isLoading}
            error={error}
            selectedProduct={selectedProduct}
          />
        )}

        {currentPage === 'results' && tariffResults && (
          <ResultsPage
            tariffResults={tariffResults}
            formData={formData}
            selectedProduct={selectedProduct}
            handleBack={handleBack}
          />
        )}

        {/* Admin dashboard - only for admin users */}
        {user && user.role === 'ADMIN' && currentPage === 'admin' && (
          <AdminDashboard onBack={handleBackToHome} />
        )}
      </div>
    </div>
  );
};

export default App;
