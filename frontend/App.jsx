// frontend/App.jsx

import React, { useState, useEffect } from 'react';
import HomePage from './components/HomePage';
import DetailPage from './components/DetailPage';
import ResultsPage from './components/ResultsPage';
import LoginPage from './components/LoginPage';
import RegisterPage from './components/RegisterPage';
import UserInfo from './components/UserInfo';
import AdminDashboard from './components/AdminDashboard';
import apiService from './services/apiService';

const App = () => {
  const [currentPage, setCurrentPage] = useState('login');
  const [authPage, setAuthPage] = useState('login'); // 'login' or 'register'
  const [user, setUser] = useState(null);
  const [theme, setTheme] = useState('light');
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

  const handleLoginSuccess = (userData) => {
    setUser(userData);
    setCurrentPage('home');
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
    <div className={`min-h-screen ${theme === 'dark' ? 'dark bg-gray-900' : 'bg-gray-50'}`}>
      <div className="container mx-auto px-4 py-6">
        {/* User Info Component - shows at top of all pages */}
        <UserInfo user={user} onLogout={handleLogout} onAdminClick={handleAdminClick} />

        {currentPage === 'home' && (
          <HomePage onGetStarted={handleGetStarted} theme={theme} setTheme={setTheme} />
        )}

        {currentPage === 'detail' && (
          <DetailPage
            formData={formData}
            handleInputChange={handleInputChange}
            handleSearch={handleSearch}
            handleBackToHome={handleBackToHome}
            theme={theme}
            setTheme={setTheme}
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
            handleBackToHome={handleBackToHome}
            theme={theme}
            setTheme={setTheme}
          />
        )}

        {/* Admin dashboard - only for admin users */}
        {user && user.role === 'ADMIN' && currentPage === 'admin' && (
          <AdminDashboard onBack={handleBackToHome} theme={theme} />
        )}
      </div>
    </div>
  );
};

export default App;
