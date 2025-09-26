import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'

const Schema = z.object({
  homeCountry: z.string().min(1, 'Required'),
  destinationCountry: z.string().min(1, 'Required'),
  productName: z.string().min(1, 'Required'),
  productValue: z
    .number()
    .refine(Number.isFinite, { message: 'Enter a number' })
    .positive('Must be > 0'),
  hsCode: z.string().min(1, 'Required'),
  tradeAgreement: z.string().min(1, 'Required'),
})

export type FormValues = z.infer<typeof Schema>

export default function TariffForm({
  onSubmit,
  loading,
}: {
  onSubmit: (v: FormValues) => void
  loading: boolean
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(Schema),
    defaultValues: {
      homeCountry: 'SG',
      destinationCountry: 'US',
      productName: 'Electronics',
      productValue: 1000,
      hsCode: '8517',
      tradeAgreement: 'None', // e.g. "CPTPP", "RCEP", "None"
    },
  })

  const numberRegister = register('productValue', { valueAsNumber: true })

  return (
    <form onSubmit={handleSubmit(onSubmit)} style={{ marginTop: 12 }}>
      <div className="row">
        <div>
          <label>Home Country</label>
          <input placeholder="e.g. SG" {...register('homeCountry')} />
          {errors.homeCountry && <div className="error">{errors.homeCountry.message}</div>}
        </div>
        <div>
          <label>Destination Country</label>
          <input placeholder="e.g. US" {...register('destinationCountry')} />
          {errors.destinationCountry && <div className="error">{errors.destinationCountry.message}</div>}
        </div>
      </div>

      <div className="row" style={{ marginTop: 12 }}>
        <div>
          <label>Product Name</label>
          <input placeholder="e.g. Electronics" {...register('productName')} />
          {errors.productName && <div className="error">{errors.productName.message}</div>}
        </div>
        <div>
          <label>Product Value</label>
          <input type="number" step="0.01" {...numberRegister} />
          {errors.productValue && <div className="error">{errors.productValue.message}</div>}
        </div>
      </div>

      <div className="row" style={{ marginTop: 12 }}>
        <div>
          <label>HS Code</label>
          <input placeholder="e.g. 8517" {...register('hsCode')} />
          {errors.hsCode && <div className="error">{errors.hsCode.message}</div>}
        </div>
        <div>
          <label>Trade Agreement</label>
          <input placeholder="e.g. CPTPP / RCEP / None" {...register('tradeAgreement')} />
          {errors.tradeAgreement && <div className="error">{errors.tradeAgreement.message}</div>}
        </div>
      </div>

      <div style={{ marginTop: 16 }}>
        <button className="primary" disabled={loading} type="submit">
          {loading ? 'Calculating…' : 'Calculate'}
        </button>
      </div>
    </form>
  )
}
