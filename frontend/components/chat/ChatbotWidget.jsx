import React, { useEffect, useRef, useState } from 'react';
import { AlertCircle, CheckCircle2, Info, Loader2, MessageCircle, Send, X } from 'lucide-react';
import apiService from '../../services/apiService';

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
  const [queryId, setQueryId] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const [consentLogging, setConsentLogging] = useState(true);
  const [pendingQuestions, setPendingQuestions] = useState([]);
  const [previousAnswers, setPreviousAnswers] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');
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

  const appendMessage = (message) => {
    setMessages((prev) => [...prev, message]);
  };

  const handleSendMessage = async (overrideValue) => {
    const contentToSend = typeof overrideValue === 'string' ? overrideValue : inputValue;
    const trimmed = contentToSend.trim();
    if (!trimmed || isTyping) {
      return;
    }

    addMessage('user', trimmed);
    if (!overrideValue) {
      setInputValue('');
    }
    setIsTyping(true);
    setErrorMessage('');

    try {
      const hsResponse = await apiService.chatbot.resolveHsCode({
        queryId,
        productName: trimmed.length > 60 ? trimmed.slice(0, 60) : trimmed,
        description: trimmed,
        attributes: [],
        previousAnswers,
        sessionId,
        consentLogging,
      });

      if (!sessionId && hsResponse.sessionId) {
        setSessionId(hsResponse.sessionId);
      }
      if (hsResponse.queryId) {
        setQueryId(hsResponse.queryId);
      }

      if (hsResponse.candidates && hsResponse.candidates.length > 0) {
        const formattedCandidates = hsResponse.candidates
          .map(
            (candidate, index) =>
              `${index + 1}. ${candidate.hsCode} (confidence ${(candidate.confidence * 100).toFixed(
                0
              )}%)\n   ${candidate.rationale}`
          )
          .join('\n\n');

        addMessage(
          'assistant',
          `Here are the HS code suggestions based on your description:\n\n${formattedCandidates}`
        );
      } else {
        addMessage(
          'assistant',
          "I wasn't able to find a confident HS code match yet. Could you share more details about materials, function, or target market?"
        );
      }

      if (hsResponse.disambiguationQuestions && hsResponse.disambiguationQuestions.length > 0) {
        setPendingQuestions(hsResponse.disambiguationQuestions);
        const questionText = hsResponse.disambiguationQuestions
          .map((q) => `• ${q.question}`)
          .join('\n');
        addMessage(
          'assistant',
          `I need a bit more detail to narrow this down:\n${questionText}\n\nPlease select an option below.`
        );
      } else {
        setPendingQuestions([]);
      }

      if (hsResponse.fallback) {
        if (hsResponse.fallback.lastUsedCodes && hsResponse.fallback.lastUsedCodes.length > 0) {
          const fallbackList = hsResponse.fallback.lastUsedCodes
            .map((item) => `${item.hsCode} (from ${new Date(item.timestamp).toLocaleString()})`)
            .join('\n');
          addMessage(
            'assistant',
            `Previous HS codes you used:\n${fallbackList}\n\nNeed more help? ${hsResponse.fallback.manualSearchUrl}`
          );
        } else if (hsResponse.fallback.manualSearchUrl) {
          addMessage(
            'assistant',
            `I can’t determine the code yet. You can continue describing the product or check the manual search: ${hsResponse.fallback.manualSearchUrl}`
          );
        }
      }

      if (hsResponse.notice && hsResponse.notice.message) {
        addMessage('assistant', `${hsResponse.notice.message} More info: ${hsResponse.notice.privacyPolicyUrl}`);
        if (hsResponse.notice.consentGranted != null) {
          setConsentLogging(hsResponse.notice.consentGranted);
        }
      }
    } catch (error) {
      console.error('Chatbot simulation error:', error);
      const message = error.message || 'Sorry, I could not process that. Please try again.';
      setErrorMessage(message);
      addMessage(
        'assistant',
        'Something went wrong while fetching HS code suggestions. Please try again or give more description.'
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

  const handleOptionSelect = async (questionId, answer) => {
    const answerEntry = { questionId, answer };
    setPreviousAnswers((prev) => [...prev, answerEntry]);
    setPendingQuestions((prev) => prev.filter((question) => question.id !== questionId));
    await handleSendMessage(answer);
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
              <div className="flex items-center justify-between gap-2 mb-2 text-[11px] text-white/50">
                <label className="inline-flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={consentLogging}
                    onChange={(e) => setConsentLogging(e.target.checked)}
                    className="accent-purple-500"
                  />
                  <span>
                    We may store chat history to improve results.{' '}
                    <a href="/privacy" className="underline text-purple-300">
                      Privacy policy
                    </a>
                  </span>
                </label>
              </div>
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
              {pendingQuestions.length > 0 && (
                <div className="mt-2 space-y-2">
                  {pendingQuestions.map((question) => (
                    <div key={question.id} className="bg-white/5 border border-white/10 rounded-lg p-3">
                      <p className="text-xs text-white/80 mb-2">{question.question}</p>
                      <div className="flex flex-wrap gap-2">
                        {(question.options || ['Yes', 'No']).map((option) => (
                          <button
                            key={option}
                            type="button"
                            onClick={() => handleOptionSelect(question.id, option)}
                            className="px-3 py-1.5 rounded-full bg-purple-500/80 hover:bg-purple-400/90 text-xs text-white transition-colors"
                          >
                            {option}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
              {errorMessage && (
                <div className="mt-3 text-xs text-rose-300 flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 mt-0.5" />
                  <span>{errorMessage}</span>
                </div>
              )}
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

