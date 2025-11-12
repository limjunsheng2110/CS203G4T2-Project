import React, { useEffect, useState } from 'react';
import { MessageCircle, X } from 'lucide-react';

const CHAT_WIDGET_STORAGE_KEY = 'chatbot:isOpen';

const ChatbotWidget = () => {
  const [isOpen, setIsOpen] = useState(false);

  // Restore open state per session
  useEffect(() => {
    try {
      const stored = sessionStorage.getItem(CHAT_WIDGET_STORAGE_KEY);
      if (stored !== null) {
        setIsOpen(stored === 'true');
      }
    } catch (error) {
      console.warn('Unable to access sessionStorage for chatbot widget state:', error);
    }
  }, []);

  // Persist state when toggled
  useEffect(() => {
    try {
      sessionStorage.setItem(CHAT_WIDGET_STORAGE_KEY, JSON.stringify(isOpen));
    } catch (error) {
      console.warn('Unable to persist chatbot widget state:', error);
    }
  }, [isOpen]);

  const handleToggle = () => {
    setIsOpen((prev) => !prev);
  };

  return (
    <>
      {isOpen && (
        <div className="fixed inset-x-4 bottom-24 sm:bottom-6 sm:right-6 sm:left-auto z-40 sm:w-96 max-w-full sm:max-w-sm">
          <div className="bg-gray-900/95 backdrop-blur-md text-white rounded-2xl shadow-2xl border border-white/10 flex flex-col h-[32rem] sm:h-[30rem] overflow-hidden">
            <div className="bg-gradient-to-r from-purple-600 to-indigo-600 px-4 py-3 flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-semibold tracking-wide">HS Code Assistant</p>
                <p className="text-xs text-white/80">
                  Describe your product to receive HS code guidance.
                </p>
              </div>
              <button
                type="button"
                onClick={handleToggle}
                className="p-1.5 rounded-full hover:bg-white/20 transition-colors"
                aria-label="Close HS Code Assistant"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
              <div className="bg-white/10 border border-white/10 rounded-xl p-4 text-sm leading-relaxed text-white/80">
                👋 Welcome! This HS Code assistant is here to help you find the correct classification for your product. Tell us what you&apos;re importing or exporting to get started.
              </div>
              <div className="hidden sm:block text-xs text-white/50">
                Tip: Provide details like material, usage, and target market to improve results.
              </div>
            </div>
            <div className="border-t border-white/10 px-4 py-3 bg-black/20">
              <div className="h-12 rounded-xl bg-white/5 border border-white/10 flex items-center px-3 text-sm text-white/50">
                Chat input coming soon…
              </div>
            </div>
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={handleToggle}
        aria-label={isOpen ? 'Close HS Code assistant' : 'Open HS Code assistant'}
        aria-expanded={isOpen}
        className="fixed bottom-6 right-6 z-40 inline-flex items-center justify-center rounded-full bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-2xl p-4 sm:p-5 hover:scale-105 focus:outline-none focus-visible:ring-4 focus-visible:ring-purple-400/60 transition-transform"
      >
        <MessageCircle className="w-6 h-6 sm:w-7 sm:h-7" />
      </button>
    </>
  );
};

export default ChatbotWidget;

