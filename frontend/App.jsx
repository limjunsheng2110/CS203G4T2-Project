// frontend/App.jsx

import React, { useState } from 'react';
import HomePage from './components/HomePage';
import DetailPage from './components/DetailPage';
import ResultsPage from './components/ResultsPage';

const App = () => {
  const [currentPage, setCurrentPage] = useState('home');
  const [theme, setTheme] = useState('light');
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
  };

  const handleGetStarted = () => {
    setCurrentPage('detail');
  };

  const handleSearch = () => {
    setCurrentPage('results');
  };

  const handleBack = () => {
    setCurrentPage('detail');
  };

  const handleBackToHome = () => {
    setCurrentPage('home');
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
      handleInputChange={handleInputChange}
      handleSearch={handleSearch}
      theme={theme}
      toggleTheme={toggleTheme}
      onBack={handleBackToHome}
    />
  ) : (
    <ResultsPage
      formData={formData}
      handleBack={handleBack}
      theme={theme}
      toggleTheme={toggleTheme}
    />
  );
};

export default App;