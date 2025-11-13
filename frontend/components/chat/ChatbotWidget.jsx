import React, { useEffect, useRef, useState } from 'react';
import { AlertCircle, CheckCircle2, Info, Loader2, MessageCircle, Send, X, Maximize2, Minimize2 } from 'lucide-react';
import apiService from '../../services/apiService';

const CHAT_WIDGET_STORAGE_KEY = 'chatbot:isOpen';
const CHAT_WIDGET_VISITED_KEY = 'chatbot:hasVisited';
const AUTO_OPEN_DELAY_MS = 2000;
const AUTO_OPEN_ENABLED =
  typeof import.meta !== 'undefined' &&
  import.meta.env &&
  import.meta.env.VITE_CHATBOT_AUTO_OPEN === 'true';

const DEFAULT_CONFIDENCE_THRESHOLD = 0.7;

const ChatbotWidget = ({ onHsCodeSelected = () => {} }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [queryId, setQueryId] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const [consentLogging, setConsentLogging] = useState(true);
  const [pendingQuestions, setPendingQuestions] = useState([]);
  const [previousAnswers, setPreviousAnswers] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');
  const [confirmationCandidate, setConfirmationCandidate] = useState(null);
  const messagesEndRef = useRef(null);
  const typingTimeoutRef = useRef(null);

  const [messages, setMessages] = useState(() => [
    {
      id: 'intro',
      role: 'assistant',
      content:
        'Welcome to TariffNom! I\'m here to help you calculate tariffs. Ask me anything about:\n\n• How to fill out the transaction form\n• What each field means\n• Tips for getting accurate results\n• Understanding your tariff calculation\n\nWhat would you like to know?',
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
      // Check if this is a tutorial question or HS code search
      const tutorialResponse = getHelpResponse(trimmed.toLowerCase());
      
      if (tutorialResponse) {
        // Tutorial question detected - provide local response
        await new Promise(resolve => setTimeout(resolve, 500)); // Simulate thinking
        addMessage('assistant', tutorialResponse);
      } else {
        // No tutorial match - treat as HS code search
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
                `${index + 1}. **${candidate.hsCode}** - ${candidate.rationale}`
            )
            .join('\n\n');

          addMessage(
            'assistant',
            `Here are the matching products from our database:\n\n${formattedCandidates}`
          );

          const primaryCandidate = hsResponse.candidates[0];
          if (primaryCandidate && primaryCandidate.confidence >= DEFAULT_CONFIDENCE_THRESHOLD) {
            setConfirmationCandidate(primaryCandidate);
          } else {
            setConfirmationCandidate(null);
          }
        } else {
          addMessage(
            'assistant',
            "I couldn't find any matching products. Try:\n• Using different keywords\n• Being more specific about the product\n• Asking a tutorial question like 'What is an HS code?'"
          );
          setConfirmationCandidate(null);
        }

        if (hsResponse.disambiguationQuestions && hsResponse.disambiguationQuestions.length > 0) {
          setPendingQuestions(hsResponse.disambiguationQuestions);
        } else {
          setPendingQuestions([]);
        }
      }
    } catch (error) {
      console.error('Chatbot error:', error);
      
      // Handle validation errors (400 status)
      if (error.code === 400 || error.response?.status === 400) {
        const validationMessage = 
          error.response?.data?.message || 
          error.message || 
          'Please provide a valid product description between 10 and 2000 characters.';
        
        setErrorMessage(validationMessage);
        addMessage('assistant', validationMessage);
      } else {
        // Handle other errors
        const message = error.message || 'Sorry, I could not process that. Please try again.';
        setErrorMessage(message);
        addMessage(
          'assistant',
          'Something went wrong. Please try again or ask a tutorial question!'
        );
      }
    } finally {
      setIsTyping(false);
    }
  };

  const getHelpResponse = (query) => {
    // Import/Export Country
    if (query.includes('import') && query.includes('country') || query.includes('destination')) {
      return '**Import Country**: This is where your goods are being shipped TO (the destination).\n\nSelect the country that will receive the goods. This determines which tariffs and taxes apply.';
    }
    
    if (query.includes('export') && query.includes('country') || query.includes('origin')) {
      return '**Export Country**: This is where your goods are coming FROM (the origin).\n\nSelect the country sending the goods. This can affect tariff rates if there\'s a trade agreement between countries.';
    }

    // HS Code
    if (query.includes('hs code') || query.includes('product code') || query.includes('harmonized')) {
      return '**HS Code** (Harmonized System Code): A 6-digit code that classifies your product internationally.\n\n**How to find it:**\n• Search by product name in our dropdown\n• Visit https://www.trade.gov/harmonized-system-hs-codes\n• Search "HS code for [your product]"\n\n**Example:** Live goats = 010420';
    }

    // Total Value
    if (query.includes('total value') || query.includes('product value') || query.includes('price')) {
      return '**Total Value (USD)**: The total price of your shipment in US Dollars.\n\n**Important:**\n• Must be greater than $0\n• Enter the invoice value\n• Include product cost only (shipping calculated separately)';
    }

    // Weight
    if (query.includes('weight') || query.includes('kg') || query.includes('kilogram')) {
      return '**Weight (kg)**: Total weight of your shipment in kilograms.\n\n**Why we need this:**\n• Calculates shipping costs\n• Some duties are weight-based\n• Must be greater than 0';
    }

    // Year
    if (query.includes('year') || query.includes('tariff year') || query.includes('when')) {
      return '**Year**: The year for tariff rate calculation (optional).\n\n**Notes:**\n• Tariff rates change over time\n• Leave blank to use current rates\n• Choose 2022-2025 for historical comparison';
    }

    // Shipping Mode
    if (query.includes('shipping') || query.includes('air') || query.includes('sea')) {
      return '**Shipping Mode**: How your goods will be transported.\n\n**Options:**\n• **Air** - Faster but more expensive ($/kg higher)\n• **Sea** - Slower but cheaper for bulk shipments\n\nShipping costs are calculated automatically based on weight and mode.';
    }

    // General calculation
    if (query.includes('calculate') || query.includes('how to') || query.includes('use')) {
      return '**How to Calculate Tariffs:**\n\n1. Select **Import Country** (destination)\n2. Select **Export Country** (origin)\n3. Choose **Product (HS Code)** from dropdown\n4. Enter **Total Value** in USD\n5. Enter **Weight** in kg\n6. Select **Shipping Mode** (Air/Sea)\n7. (Optional) Choose **Year**\n8. Click **Search Tariffs**\n\nYou\'ll see the total cost breakdown including duties, taxes, and shipping!';
    }

    // Results explanation
    if (query.includes('result') || query.includes('understand') || query.includes('breakdown')) {
      return '**Understanding Your Results:**\n\n• **Total Cost** - Everything you\'ll pay\n• **Total Tariffs** - Import duties + VAT/GST\n• **Product Value** - Your original invoice amount\n• **Tariff Rate** - % charged on your goods\n• **Customs Value** - Base for calculating duties\n• **Shipping Cost** - Based on weight and mode\n\nThe breakdown shows exactly where your money goes!';
    }

    // VAT/GST
    if (query.includes('vat') || query.includes('gst') || query.includes('tax')) {
      return '**VAT/GST**: Value Added Tax or Goods & Services Tax.\n\n**What it is:**\n• Additional tax charged by importing country\n• Calculated on (Product Value + Duties + Shipping)\n• Varies by country (e.g., UK: 20%, Singapore: 9%)\n\nThis is automatically included in your total cost!';
    }

    // Trade agreements
    if (query.includes('trade agreement') || query.includes('fta') || query.includes('rate')) {
      return '**Trade Agreements**: Some countries have agreements that reduce tariffs.\n\n**How it works:**\n• If countries have an FTA, you may pay 0% tariff\n• Otherwise, you pay the MFN (Most Favored Nation) rate\n• We automatically detect and apply the best rate\n\n**Example:** Singapore-China FTA means 0% on many goods!';
    }

    // Default response
    return null; // Return null if no tutorial question detected - will trigger HS code search
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
    setConfirmationCandidate(null);
    
    // Find the question text to provide context
    const question = pendingQuestions.find(q => q.id === questionId);
    const fullResponse = question 
      ? `${question.question} Answer: ${answer}` 
      : answer;
    
    await handleSendMessage(fullResponse);
  };

  const handleConfirmCandidate = (candidate) => {
    addMessage(
      'assistant',
      `I'll use **${candidate.hsCode} - ${candidate.rationale}** for your calculation!`
    );
    setConfirmationCandidate(null);
    onHsCodeSelected({
      hsCode: candidate.hsCode,
      confidence: candidate.confidence,
      description: candidate.rationale,
    });
    setIsOpen(false);
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
        <div className={`fixed z-40 ${
          isFullscreen 
            ? 'inset-4' 
            : 'inset-x-4 bottom-24 sm:bottom-6 sm:right-6 sm:left-auto sm:w-96 max-w-full sm:max-w-sm'
        }`}>
          <div className={`bg-gray-900/95 backdrop-blur-md text-white rounded-2xl shadow-2xl border border-white/10 flex flex-col overflow-hidden ${
            isFullscreen ? 'h-full' : 'h-[34rem] sm:h-[30rem]'
          }`}>
            <div className="bg-gradient-to-r from-purple-600 to-indigo-600 px-4 py-3 flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-semibold tracking-wide">HS Code Assistant</p>
                <p className="text-xs text-white/80">
                  Describe your product to receive HS code guidance.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsFullscreen(!isFullscreen)}
                  className="p-1.5 rounded-full hover:bg-white/20 transition-colors"
                  aria-label={isFullscreen ? "Exit fullscreen" : "Fullscreen"}
                >
                  {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
                </button>
                <button
                  type="button"
                  onClick={handleToggle}
                  className="p-1.5 rounded-full hover:bg-white/20 transition-colors"
                  aria-label="Close HS Code Assistant"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
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
              {confirmationCandidate && (
                <div className="mt-3 bg-green-500/10 border border-green-400/40 text-white rounded-xl p-3 text-xs">
                  <div className="flex items-start gap-2">
                    <CheckCircle2 className="w-4 h-4 mt-0.5 text-green-300" />
                    <div className="flex-1">
                      <p className="font-semibold text-sm text-green-200">
                        Suggested HS Code: {confirmationCandidate.hsCode}
                      </p>
                      <p className="text-green-100 mt-1 leading-relaxed">
                        Confidence {(confirmationCandidate.confidence * 100).toFixed(0)}% –{' '}
                        {confirmationCandidate.rationale}
                      </p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => handleConfirmCandidate(confirmationCandidate)}
                          className="px-3 py-1.5 bg-green-500 hover:bg-green-400 text-xs font-medium rounded-full text-black transition-colors"
                        >
                          Use this HS code
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmationCandidate(null)}
                          className="px-3 py-1.5 bg-white/10 hover:bg-white/20 text-xs font-medium rounded-full transition-colors"
                        >
                          Keep looking
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {!isOpen && (
        <button
          type="button"
          onClick={handleToggle}
          aria-label="Open HS Code assistant"
          aria-expanded={false}
          className="fixed bottom-6 right-6 z-40 inline-flex items-center justify-center rounded-full bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-2xl p-4 sm:p-5 hover:scale-105 focus:outline-none focus-visible:ring-4 focus-visible:ring-purple-400/60 transition-transform"
        >
          <MessageCircle className="w-6 h-6 sm:w-7 sm:h-7" />
        </button>
      )}
    </>
  );
};

export default ChatbotWidget;

