// frontend/components/admin/ManageShippingRates.jsx
import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Search, Ship } from 'lucide-react';

const ManageShippingRates = ({ theme }) => {
  const [shippingRates, setShippingRates] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedRate, setSelectedRate] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchShippingRates();
  }, []);

  const fetchShippingRates = async () => {
    setIsLoading(true);
    try {
      const token = localStorage.getItem('authToken');
      const response = await fetch('/api/shipping-rates', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) throw new Error('Failed to fetch shipping rates');

      const data = await response.json();
      setShippingRates(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteRate = async (rateId) => {
    if (!window.confirm('Are you sure you want to delete this shipping rate?')) return;

    try {
      const token = localStorage.getItem('authToken');
      const response = await fetch(`/api/shipping-rates/${rateId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) throw new Error('Failed to delete shipping rate');

      setSuccess('Shipping rate deleted successfully');
      fetchShippingRates();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.message);
      setTimeout(() => setError(''), 3000);
    }
  };

  const handleEditRate = (rate) => {
    setSelectedRate(rate);
    setShowEditModal(true);
  };

  const filteredRates = shippingRates.filter(rate =>
    rate.importingCountryCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    rate.exportingCountryCode?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            placeholder="Search by Country..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white"
          />
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus size={20} />
          Add Shipping Rate
        </button>
      </div>

      {/* Success/Error Messages */}
      {success && (
        <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 text-green-800 dark:text-green-200 px-4 py-3 rounded-lg">
          {success}
        </div>
      )}

      {error && (
        <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 text-red-800 dark:text-red-200 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Shipping Rates Table */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-900">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Trade Route
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Air Rate (USD)
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Sea Rate (USD)
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {filteredRates.map((rate) => (
                  <tr key={rate.id} className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center text-sm text-gray-900 dark:text-gray-300">
                        <Ship size={16} className="mr-2 text-gray-400" />
                        {rate.exportingCountryCode} → {rate.importingCountryCode}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900 dark:text-gray-300">
                        ${rate.airRate ? rate.airRate.toFixed(2) : '0.00'}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900 dark:text-gray-300">
                        ${rate.seaRate ? rate.seaRate.toFixed(2) : '0.00'}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleEditRate(rate)}
                        className="text-indigo-600 hover:text-indigo-900 dark:text-indigo-400 dark:hover:text-indigo-300 mr-4"
                      >
                        <Edit2 size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteRate(rate.id)}
                        className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {filteredRates.length === 0 && (
            <div className="text-center py-12 text-gray-500 dark:text-gray-400">
              No shipping rates found
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      {showCreateModal && (
        <ShippingRateModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            fetchShippingRates();
            setSuccess('Shipping rate created successfully');
            setTimeout(() => setSuccess(''), 3000);
          }}
        />
      )}

      {showEditModal && selectedRate && (
        <ShippingRateModal
          rate={selectedRate}
          onClose={() => {
            setShowEditModal(false);
            setSelectedRate(null);
          }}
          onSuccess={() => {
            setShowEditModal(false);
            setSelectedRate(null);
            fetchShippingRates();
            setSuccess('Shipping rate updated successfully');
            setTimeout(() => setSuccess(''), 3000);
          }}
        />
      )}
    </div>
  );
};

// Shipping Rate Modal Component
const ShippingRateModal = ({ rate, onClose, onSuccess }) => {
  const isEdit = !!rate;
  const [formData, setFormData] = useState({
    importingCountryCode: rate?.importingCountryCode || '',
    exportingCountryCode: rate?.exportingCountryCode || '',
    airRate: rate?.airRate || '',
    seaRate: rate?.seaRate || ''
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const token = localStorage.getItem('authToken');
      const url = isEdit ? `/api/shipping-rates/${rate.id}` : '/api/shipping-rates';
      const method = isEdit ? 'PUT' : 'POST';

      const payload = {
        importingCountryCode: formData.importingCountryCode,
        exportingCountryCode: formData.exportingCountryCode,
        airRate: formData.airRate ? parseFloat(formData.airRate) : null,
        seaRate: formData.seaRate ? parseFloat(formData.seaRate) : null
      };

      console.log('Submitting shipping rate:', payload);

      const response = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to ${isEdit ? 'update' : 'create'} shipping rate`);
      }

      const result = await response.json();
      console.log('Success response:', result);
      onSuccess();
    } catch (err) {
      console.error('Error submitting shipping rate:', err);
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full mx-4">
        <div className="p-6">
          <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
            {isEdit ? 'Edit Shipping Rate' : 'Create Shipping Rate'}
          </h3>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Exporting Country
                </label>
                <input
                  type="text"
                  required
                  value={formData.exportingCountryCode}
                  onChange={(e) => setFormData({ ...formData, exportingCountryCode: e.target.value.toUpperCase() })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                  placeholder="e.g., CN"
                  maxLength="3"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Importing Country
                </label>
                <input
                  type="text"
                  required
                  value={formData.importingCountryCode}
                  onChange={(e) => setFormData({ ...formData, importingCountryCode: e.target.value.toUpperCase() })}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                  placeholder="e.g., SG"
                  maxLength="3"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Air Rate (USD) - Flat Rate
              </label>
              <input
                type="number"
                step="0.01"
                value={formData.airRate}
                onChange={(e) => setFormData({ ...formData, airRate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                placeholder="e.g., 15.00"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Flat rate charge for air shipping (e.g., $10, $15)
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Sea Rate (USD) - Flat Rate
              </label>
              <input
                type="number"
                step="0.01"
                value={formData.seaRate}
                onChange={(e) => setFormData({ ...formData, seaRate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                placeholder="e.g., 10.00"
              />
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Flat rate charge for sea shipping (e.g., $10, $15)
              </p>
            </div>

            {error && (
              <div className="bg-red-50 dark:bg-red-900/30 text-red-800 dark:text-red-200 px-3 py-2 rounded-lg text-sm">
                {error}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isLoading}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                {isLoading ? (isEdit ? 'Updating...' : 'Creating...') : (isEdit ? 'Update' : 'Create')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ManageShippingRates;

