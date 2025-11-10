// frontend/components/AdminDashboard.jsx
import React, { useState } from 'react';
import { ArrowLeft, Users, DollarSign, Settings, Ship } from 'lucide-react';
import ManageUsers from '../admin/ManageUsers';
import ManageTariffRates from '../admin/ManageTariffRates';
import ManageShippingRates from '../admin/ManageShippingRates';

const AdminDashboard = ({ onBack }) => {
  const [activeTab, setActiveTab] = useState('users');

  const tabs = [
    { id: 'users', label: 'Manage Users', icon: Users },
    { id: 'tariff-rates', label: 'Manage Tariff Rates', icon: DollarSign },
    { id: 'shipping-rates', label: 'Manage Shipping Rates', icon: Ship },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-black via-purple-950 to-black">
      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={onBack}
            className="flex items-center gap-2 text-purple-400 hover:text-purple-300 font-medium mb-4"
          >
            <ArrowLeft size={20} />
            Back to Home
          </button>

          <div className="flex items-center gap-3 mb-2">
            <Settings className="w-8 h-8 text-purple-400" />
            <h1 className="text-3xl font-bold text-white">
              Admin Dashboard
            </h1>
          </div>
          <p className="text-gray-400">
            Manage users, tariff rates, shipping rates, and system settings
          </p>
        </div>

        {/* Tabs */}
        <div className="bg-zinc-900 rounded-lg shadow-md overflow-hidden border border-zinc-700">
          <div className="border-b border-zinc-700">
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
                        ? 'border-purple-500 text-purple-400'
                        : 'border-transparent text-gray-400 hover:text-gray-300 hover:border-gray-600'
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
            {activeTab === 'users' && <ManageUsers />}
            {activeTab === 'tariff-rates' && <ManageTariffRates />}
            {activeTab === 'shipping-rates' && <ManageShippingRates />}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
