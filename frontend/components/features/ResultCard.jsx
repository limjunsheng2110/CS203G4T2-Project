import React from 'react';
import { Package, TrendingDown } from 'lucide-react';

const ResultCard = ({ result, formData, theme, colours }) => {
  const bgColour = result.isRecommended 
    ? (theme === 'light' ? 'bg-green-50' : 'bg-green-950') 
    : colours.resultCardBg;
  const borderColour = result.isRecommended 
    ? (theme === 'light' ? 'border-green-200' : 'border-green-800') 
    : colours.border;

  return (
    <div className={`${bgColour} border ${borderColour} rounded-lg shadow-sm p-6`}>
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-start gap-3">
          <Package className={colours.textMuted} size={24} />
          <div>
            <h3 className={`text-lg font-semibold ${colours.text} mb-1`}>{result.type}</h3>
            <p className={`text-sm ${colours.textMuted}`}>{result.productName}</p>
          </div>
        </div>
        <div className="text-right">
          <div className={`text-2xl font-bold ${colours.text}`}>{result.tariffRate}</div>
          <div className={`text-xs ${colours.textMuted}`}>Tariff Rate</div>
        </div>
      </div>

      <div className="flex justify-between items-center">
        <div>
          {result.savings && (
            <div className="flex items-center gap-2 text-green-700 font-medium">
              <TrendingDown size={18} />
              Saves ${result.savings.toLocaleString()} compared to standard rate
            </div>
          )}
        </div>
        <div className="text-right">
          <div className={`text-xs ${colours.textMuted} mb-1`}>Total Cost</div>
          <div className={`text-2xl font-bold ${colours.text}`}>${result.totalCost.toLocaleString()}</div>
        </div>
      </div>

      <div className={`mt-4 pt-4 border-t ${colours.border}`}>
        <div className={`text-xs ${colours.textMuted} space-y-1`}>
          <div><span className="font-semibold">Import:</span> {formData.importCountry || 'United States'}</div>
          <div><span className="font-semibold">Export:</span> {formData.exportCountry || 'China'}</div>
          {formData.shippingMode && (
            <div><span className="font-semibold">Shipping:</span> {formData.shippingMode.charAt(0).toUpperCase() + formData.shippingMode.slice(1)}</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ResultCard;