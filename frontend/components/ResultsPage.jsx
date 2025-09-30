import React from 'react';
import { ArrowLeft } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';
import { mockResults } from '../data/mockResults';
import ThemeToggle from './ThemeToggle';
import ResultCard from './ResultCard';

const ResultsPage = ({ formData, handleBack, theme, toggleTheme }) => {
  const colors = getThemeColours(theme);

  return (
    <div className={`min-h-screen ${colors.resultBg} py-8 px-4`}>
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

        <div className="text-center mb-8">
          <h1 className={`text-4xl font-bold ${colors.text} mb-2`}>Full Result Page</h1>
          <p className={colors.textMuted}>All Tariff Options</p>
          <p className={`text-sm ${colors.textMuted} mt-1`}>Compare different tariff rates and classifications</p>
        </div>

        <div className="space-y-4">
          {mockResults.map((result, index) => (
            <ResultCard
              key={index}
              result={result}
              formData={formData}
              theme={theme}
              colors={colors}
            />
          ))}
        </div>

        <div className={`mt-8 ${colors.infoBg} border ${colors.infoBorder} rounded-lg p-4`}>
          <h4 className={`font-semibold ${colors.infoText} mb-2`}>Important Notes</h4>
          <ul className={`text-sm ${colors.infoText} space-y-1`}>
            <li>• Tariff rates are subject to change based on current trade agreements</li>
            <li>• Additional fees may apply depending on product category and customs requirements</li>
            <li>• Preferential rates require valid certificates of origin</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default ResultsPage;