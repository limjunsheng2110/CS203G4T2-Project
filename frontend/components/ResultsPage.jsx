import React from 'react';
import { ArrowLeft } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';
import { mockResults } from '../data/mockResults';
import ThemeToggle from './ThemeToggle';
import ResultCard from './ResultCard';
import ExchangeRateAnalysis from './ExchangeRateAnalysis';

const ResultsPage = ({ formData, handleBack, theme, toggleTheme }) => {
  const colours = getThemeColours(theme);

  return (
    <div className={`min-h-screen ${colours.resultBg} py-8 px-4`}>
      <div className="max-w-5xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <button
            onClick={handleBack}
            className="flex items-center gap-2 text-green-700 hover:text-green-800 font-medium"
          >
            <ArrowLeft size={20} />
            Back to Search
          </button>
          
          <ThemeToggle theme={theme} toggleTheme={toggleTheme} />
        </div>

        <div className="flex justify-center mb-6">
          <img 
            src="/TariffNomLogo.png" 
            alt="TariffNom Logo" 
            className="w-96 h-auto"
          />
        </div>

        {/* Exchange Rate Analysis Section */}
        <ExchangeRateAnalysis 
          importingCountry={formData.importCountry}
          exportingCountry={formData.exportCountry}
          theme={theme}
        />

        <div className="space-y-4">
          {mockResults.map((result, index) => (
            <ResultCard
              key={index}
              result={result}
              formData={formData}
              theme={theme}
              colours={colours}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default ResultsPage;