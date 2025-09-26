import axios from 'axios'
import type { TariffCalculationRequest, TariffCalculationResponse } from '../types/tariff'

const baseURL = import.meta.env.VITE_API_BASE_URL || '' // empty when using Vite proxy

export const api = axios.create({
  baseURL,
  timeout: 15000,
})

export async function getTariffCalculate(params: TariffCalculationRequest) {
  // Backend is GET /api/tariff/calculate?homeCountry=...&...
  const res = await api.get<TariffCalculationResponse>('/api/tariff/calculate', { params })
  return res.data.data
}
