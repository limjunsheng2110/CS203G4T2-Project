export const getThemeColours = (theme = 'dark') => {
  if (theme === 'light') {
    return {
      bg: 'bg-white',
      cardBg: 'bg-orange-50',
      text: 'text-gray-800',
      textSecondary: 'text-gray-700',
      textMuted: 'text-gray-600',
      border: 'border-orange-200',
      borderHover: 'hover:border-orange-300',
      inputBg: 'bg-white',
      resultBg: 'bg-gray-50',
      resultCardBg: 'bg-white',
      headingText: 'text-gray-900',
      labelText: 'text-gray-800',
      fieldBg: 'bg-white',
      buttonBorder: 'border-orange-300',
      buttonBorderHover: 'hover:bg-orange-100',
      infoBg: 'bg-blue-50',
      infoBorder: 'border-blue-200',
      infoText: 'text-blue-700'
    };
  }

  return {
    bg: 'bg-black',
    cardBg: 'bg-zinc-900',
    text: 'text-white',
    textSecondary: 'text-gray-200',
    textMuted: 'text-gray-400',
    border: 'border-zinc-700',
    borderHover: 'hover:border-zinc-600',
    inputBg: 'bg-zinc-800',
    resultBg: 'bg-black',
    resultCardBg: 'bg-zinc-900',
    headingText: 'text-white',
    labelText: 'text-white',
    fieldBg: 'bg-zinc-800',
    buttonBorder: 'border-zinc-700',
    buttonBorderHover: 'hover:bg-zinc-800',
    infoBg: 'bg-zinc-800',
    infoBorder: 'border-zinc-700',
    infoText: 'text-gray-300'
  };
};