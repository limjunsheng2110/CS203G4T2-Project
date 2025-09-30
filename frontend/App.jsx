import React, { useState } from 'react';
import DetailPage from './components/DetailPage';
import ResultsPage from './components/ResultsPage';

const App = () => {
  const [currentPage, setCurrentPage] = useState('detail');
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

  const handleSearch = () => {
    setCurrentPage('results');
  };

  const handleBack = () => {
    setCurrentPage('detail');
  };

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  return currentPage === 'detail' ? (
    <DetailPage
      formData={formData}
      handleInputChange={handleInputChange}
      handleSearch={handleSearch}
      theme={theme}
      toggleTheme={toggleTheme}
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