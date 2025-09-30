import React from 'react';
import { Package, TrendingDown } from 'lucide-react';

const ResultCard = ({ result, formData, theme, colors }) => {
  const bgColor = result.isRecommended 
    ? (theme === 'light' ? 'bg-green-50' : 'bg-green-950') 
    : colors.resultCardBg;
  const borderColor = result.isRecommended 
    ? (theme === 'light' ? 'border-green-200' : 'border-green-800') 
    : colors.border;

  return (
    <div className={`${bgColor} border ${borderColor} rounded-lg shadow-sm p-6`}>
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-start gap-3">
          <Package className={colors.textMuted} size={24} />
          <div>
            <h3 className={`text-lg font-semibold ${colors.text} mb-1`}>{result.type}</h3>
            <p className={`text-sm ${colors.textMuted}`}>{result.productName}</p>
          </div>
        </div>
        <div className="text-right">
          <div className={`text-2xl font-bold ${colors.text}`}>{result.tariffRate}</div>
          <div className={`text-xs ${colors.textMuted}`}>Tariff Rate</div>
        </div>
      </div>

      <div className={`${theme === 'light' ? 'bg-white' : 'bg-zinc-800'} rounded p-4 mb-3`}>
        <div className="grid grid-cols-4 gap-4 text-sm">
          <div>
            <div className={colors.textMuted + ' mb-1'}>HS Code</div>
            <div className={`font-semibold ${colors.text}`}>{result.hsCode}</div>
          </div>
          <div>
            <div className={colors.textMuted + ' mb-1'}>Base Value</div>
            <div className={`font-semibold ${colors.text}`}>${result.baseCost.toLocaleString()}</div>
          </div>
          <div>
            <div className={colors.textMuted + ' mb-1'}>Tariff</div>
            <div className={`font-semibold ${colors.text}`}>${result.tariffAmount.toLocaleString()}</div>
          </div>
          <div>
            <div className={colors.textMuted + ' mb-1'}>Fees</div>
            <div className={`font-semibold ${colors.text}`}>${result.fees.toLocaleString()}</div>
          </div>
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
          <div className={`text-xs ${colors.textMuted} mb-1`}>Total Cost</div>
          <div className={`text-2xl font-bold ${colors.text}`}>${result.totalCost.toLocaleString()}</div>
        </div>
      </div>

      <div className={`mt-4 pt-4 border-t ${colors.border}`}>
        <div className={`text-xs ${colors.textMuted} space-y-1`}>
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