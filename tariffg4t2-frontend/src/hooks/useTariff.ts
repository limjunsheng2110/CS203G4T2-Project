import { useState } from 'react'
import type { TariffCalculationRequest, TariffCalculationResult } from '../types/tariff'
import { getTariffCalculate } from '../lib/api'

export function useTariff() {
  const [result, setResult] = useState<TariffCalculationResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(values: TariffCalculationRequest) {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const data = await getTariffCalculate(values)
      setResult(data)
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || 'Request failed'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return { submit, result, loading, error }
}
