import type { TariffCalculationResult } from '../types/tariff'

function fmt(n?: number, digits = 2) {
  return typeof n === 'number' && !Number.isNaN(n) ? n.toFixed(digits) : '-'
}

export default function TariffResult({ result }: { result: TariffCalculationResult }) {
  const when = result.calculationDate ? new Date(result.calculationDate).toLocaleString() : '-'
  const ratePct = typeof result.tariffRate === 'number' ? (result.tariffRate * 100) : undefined

  return (
    <div style={{ marginTop: 16 }}>
      <h3>Result</h3>
      <table className="table">
        <tbody>
          <tr><th>Home Country</th><td>{result.homeCountry}</td></tr>
          <tr><th>Destination Country</th><td>{result.destinationCountry}</td></tr>
          <tr><th>Product</th><td>{result.productName}</td></tr>
          <tr><th>Product Value</th><td>{result.currency} {fmt(result.productValue)}</td></tr>
          <tr><th>Tariff Rate</th><td>{ratePct !== undefined ? `${ratePct.toFixed(2)}%` : '-'}</td></tr>
          <tr><th>Tariff Amount</th><td>{result.currency} {fmt(result.tariffAmount)}</td></tr>
          <tr><th>Total Cost</th><td><strong>{result.currency} {fmt(result.totalCost)}</strong></td></tr>
          <tr><th>Trade Agreement</th><td>{result.tradeAgreement || '-'}</td></tr>
          <tr><th>Calculated</th><td>{when}</td></tr>
        </tbody>
      </table>
    </div>
  )
}
