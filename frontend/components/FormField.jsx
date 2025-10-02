import React from 'react';

const FormField = ({ label, name, type, value, onChange, placeholder, required, colours }) => {
  return (
    <div>
      {label && (
        <label className={`block text-sm font-medium ${colours.textSecondary} mb-2`}>
          {label} {required && <span className="text-red-500">*</span>}
        </label>
      )}
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className={`w-full px-4 py-3 border-2 ${colours.border} rounded-lg focus:ring-2 focus:ring-green-500 focus:border-green-500 ${colours.inputBg} ${colours.text} transition-all ${colours.borderHover}`}
      />
    </div>
  );
};

export default FormField;