import React from 'react';
import { Sun, Moon } from 'lucide-react';
import { getThemeColours } from '../utils/themeColours';

const ThemeToggle = ({ theme, toggleTheme }) => {
  const colours = getThemeColours(theme);

  return (
    <button
      onClick={toggleTheme}
      className={`p-3 rounded-full ${colours.cardBg} ${colours.border} border-2 transition-all hover:scale-110`}
    >
      {theme === 'light' ? (
        <Moon className={colours.text} size={20} />
      ) : (
        <Sun className="text-white" size={20} />
      )}
    </button>
  );
};

export default ThemeToggle;