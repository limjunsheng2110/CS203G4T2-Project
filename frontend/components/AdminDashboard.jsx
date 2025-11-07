// frontend/components/AdminDashboard.jsx
import React, { useState } from 'react';
import { ArrowLeft, Users, DollarSign, Settings } from 'lucide-react';
import ManageUsers from './admin/ManageUsers';
import ManageTariffRates from './admin/ManageTariffRates';

const AdminDashboard = ({ onBack, theme }) => {
  const [activeTab, setActiveTab] = useState('users');

  const tabs = [
    { id: 'users', label: 'Manage Users', icon: Users },
    { id: 'tariff-rates', label: 'Manage Tariff Rates', icon: DollarSign },
  ];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={onBack}
            className="flex items-center gap-2 text-indigo-600 hover:text-indigo-700 dark:text-indigo-400 dark:hover:text-indigo-300 font-medium mb-4"
          >
            <ArrowLeft size={20} />
            Back to Home
          </button>

          <div className="flex items-center gap-3 mb-2">
            <Settings className="w-8 h-8 text-purple-600 dark:text-purple-400" />
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              Admin Dashboard
            </h1>
          </div>
          <p className="text-gray-600 dark:text-gray-400">
            Manage users, tariff rates, and system settings
          </p>
        </div>

        {/* Tabs */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
          <div className="border-b border-gray-200 dark:border-gray-700">
            <nav className="flex -mb-px">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`
                      flex items-center gap-2 px-6 py-4 text-sm font-medium border-b-2 transition-colors
                      ${isActive
                        ? 'border-purple-500 text-purple-600 dark:text-purple-400'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
                      }
                    `}
                  >
                    <Icon size={18} />
                    {tab.label}
                  </button>
                );
              })}
            </nav>
          </div>

          {/* Tab Content */}
          <div className="p-6">
            {activeTab === 'users' && <ManageUsers theme={theme} />}
            {activeTab === 'tariff-rates' && <ManageTariffRates theme={theme} />}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;

