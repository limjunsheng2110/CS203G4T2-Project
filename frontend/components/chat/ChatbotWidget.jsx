import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Loader2, MessageCircle, Send, X } from 'lucide-react';

const CHAT_WIDGET_STORAGE_KEY = 'chatbot:isOpen';
const CHAT_WIDGET_VISITED_KEY = 'chatbot:hasVisited';
const AUTO_OPEN_DELAY_MS = 2000;
const AUTO_OPEN_ENABLED =
  typeof import.meta !== 'undefined' &&
  import.meta.env &&
  import.meta.env.VITE_CHATBOT_AUTO_OPEN === 'true';

const ChatbotWidget = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);

  const [messages, setMessages] = useState(() => [
    {
      id: 'intro',
      role: 'assistant',
      content:
        '👋 Welcome! I can help suggest HS codes for your product. Tell me what you are importing or exporting, including details like material, use, and any special characteristics.',
      timestamp: new Date().toISOString(),
    },
  ]);

  // Restore open state per session
  useEffect(() => {
    let storedState = null;
    let autoOpenTimer;

    try {
      storedState = sessionStorage.getItem(CHAT_WIDGET_STORAGE_KEY);
      if (storedState !== null) {
        setIsOpen(storedState === 'true');
      }
    } catch (error) {
      console.warn('Unable to access sessionStorage for chatbot widget state:', error);
    }

    try {
      const hasVisited = localStorage.getItem(CHAT_WIDGET_VISITED_KEY) === 'true';

      if (!hasVisited) {
        localStorage.setItem(CHAT_WIDGET_VISITED_KEY, 'true');

        if (AUTO_OPEN_ENABLED && storedState === null) {
          autoOpenTimer = setTimeout(() => {
            setIsOpen(true);
          }, AUTO_OPEN_DELAY_MS);
        }
      }
    } catch (error) {
      console.warn('Unable to access localStorage for chatbot visit tracking:', error);
    }

    return () => {
      if (autoOpenTimer) {
        clearTimeout(autoOpenTimer);
      }
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
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

  const handleScrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  useEffect(() => {
    if (isOpen) {
      handleScrollToBottom();
    }
  }, [isOpen, messages, isTyping]);

  const handleInputChange = (event) => {
    setInputValue(event.target.value);
  };

  const addMessage = (role, content) => {
    setMessages((prev) => [
      ...prev,
      {
        id: `${role}-${Date.now()}`,
        role,
        content,
        timestamp: new Date().toISOString(),
      },
    ]);
  };

  const simulateAssistantResponse = async () => {
    // Placeholder for future backend integration.
    await new Promise((resolve) => {
      typingTimeoutRef.current = setTimeout(resolve, 1500);
    });

    addMessage(
      'assistant',
      "I'm still learning! In the final version I'll look up HS codes for you. For now, try telling me about your product's material, usage, and any special features."
    );
  };

  const handleSendMessage = async () => {
    const trimmed = inputValue.trim();
    if (!trimmed || isTyping) {
      return;
    }

    addMessage('user', trimmed);
    setInputValue('');
    setIsTyping(true);

    try {
      await simulateAssistantResponse();
    } catch (error) {
      console.error('Chatbot simulation error:', error);
      addMessage(
        'assistant',
        "Sorry, I couldn't process that just yet. Please try again in a moment."
      );
    } finally {
      setIsTyping(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSendMessage();
    }
  };

  const formattedTimestamp = (isoString) => {
    try {
      return new Intl.DateTimeFormat(undefined, {
        hour: 'numeric',
        minute: '2-digit',
      }).format(new Date(isoString));
    } catch {
      return '';
    }
  };

  return (
    <>
      {isOpen && (
        <div className="fixed inset-x-4 bottom-24 sm:bottom-6 sm:right-6 sm:left-auto z-40 sm:w-96 max-w-full sm:max-w-sm">
          <div className="bg-gray-900/95 backdrop-blur-md text-white rounded-2xl shadow-2xl border border-white/10 flex flex-col h-[34rem] sm:h-[30rem] overflow-hidden">
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
            <div className="flex-1 overflow-y-auto px-4 pt-4 pb-6 space-y-4">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`rounded-2xl px-4 py-3 max-w-[85%] text-sm leading-relaxed shadow-lg border ${
                      message.role === 'user'
                        ? 'bg-purple-600/90 border-purple-400/30'
                        : 'bg-white/10 border-white/10 text-white/80'
                    }`}
                  >
                    <p className="whitespace-pre-line break-words">{message.content}</p>
                    <span className="block text-[10px] uppercase tracking-wide mt-1 text-white/40">
                      {formattedTimestamp(message.timestamp)}
                    </span>
                  </div>
                </div>
              ))}

              {isTyping && (
                <div className="flex justify-start">
                  <div className="rounded-2xl px-4 py-3 max-w-[70%] bg-white/10 border border-white/10 text-white/70 shadow-lg flex items-center gap-2 text-sm">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Typing…
                  </div>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
            <div className="border-t border-white/10 px-4 py-3 bg-black/20">
              <label htmlFor="chatbot-input" className="sr-only">
                Describe your product for HS code suggestions
              </label>
              <div className="flex items-end gap-2">
                <textarea
                  id="chatbot-input"
                  value={inputValue}
                  onChange={handleInputChange}
                  onKeyDown={handleKeyDown}
                  rows={1}
                  placeholder="Describe your product (material, function, usage)…"
                  className="flex-1 resize-none rounded-xl bg-white/5 border border-white/10 focus:ring-2 focus:ring-purple-400/60 focus:outline-none px-3 py-2 text-sm text-white placeholder:text-white/40"
                  maxLength={500}
                  disabled={isTyping}
                  aria-describedby="chatbot-helper-text"
                />
                <button
                  type="button"
                  onClick={handleSendMessage}
                  disabled={isTyping || !inputValue.trim()}
                  className="inline-flex items-center justify-center rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed transition-colors p-2.5 focus:outline-none focus-visible:ring-4 focus-visible:ring-purple-400/60"
                  aria-label="Send message"
                >
                  {isTyping ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                </button>
              </div>
              <p id="chatbot-helper-text" className="mt-2 text-[11px] text-white/40">
                Enter to send, Shift+Enter for newline.
              </p>
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

