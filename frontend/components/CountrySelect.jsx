import React from 'react';

const CountrySelect = ({ label, name, value, onChange, required, colours }) => {
  return (
    <div>
      <label className={`block text-sm font-medium ${colours.textSecondary} mb-2`}>
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <div className="relative">
        <select
          name={name}
          value={value}
          onChange={onChange}
          className={`w-full px-4 py-3 border-2 ${colours.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colours.inputBg} ${colours.text} appearance-none cursor-pointer transition-all ${colours.borderHover}`}
        >
          <option value="">Select {label.toLowerCase()}</option>
          <option value="US">United States</option>
          <option value="CN">China</option>
          <option value="SG">Singapore</option>
          <option value="MY">Malaysia</option>
          <option value="GB">United Kingdom</option>
        </select>
        <div className={`pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 ${colours.textMuted}`}>
          <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
            <path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"/>
          </svg>
        </div>
      </div>
    </div>
  );
};

export default CountrySelect;