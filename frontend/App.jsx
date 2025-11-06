// frontend/App.jsx

  import React, { useState } from 'react';
  import HomePage from './components/HomePage';
  import DetailPage from './components/DetailPage';
  import ResultsPage from './components/ResultsPage';
  import apiService from './services/apiService';

  const App = () => {
    const [currentPage, setCurrentPage] = useState('home');
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
      shippingMode: ''
    });

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

    const toggleTheme = () => {
      setTheme(prev => prev === 'light' ? 'dark' : 'light');
    };

    if (currentPage === 'home') {
      return (
        <HomePage
          onGetStarted={handleGetStarted}
          theme={theme}
          toggleTheme={toggleTheme}
        />
      );
    }

    return currentPage === 'detail' ? (
      <DetailPage
        formData={formData}
        selectedProduct={selectedProduct}
        handleInputChange={handleInputChange}
        handleSearch={handleSearch}
        theme={theme}
        toggleTheme={toggleTheme}
        onBack={handleBackToHome}
        isLoading={isLoading}
        error={error}
      />
    ) : (
      <ResultsPage
        formData={formData}
        selectedProduct={selectedProduct}
        tariffResults={tariffResults}
        handleBack={handleBack}
        theme={theme}
        toggleTheme={toggleTheme}
      />
    );
  };

  export default App;