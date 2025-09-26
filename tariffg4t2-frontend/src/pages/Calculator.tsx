import TariffForm from '../components/TariffForm'
import TariffResult from '../components/TariffResult'
import { useTariff } from '../hooks/useTariff'

export default function Calculator() {
  const { submit, result, loading, error } = useTariff()

  return (
    <section className="card">
      <h2>Tariff Calculator</h2>
      <TariffForm onSubmit={submit} loading={loading} />
      {error && <p className="error">{error}</p>}
      {result && <TariffResult result={result} />}
    </section>
  )
}
