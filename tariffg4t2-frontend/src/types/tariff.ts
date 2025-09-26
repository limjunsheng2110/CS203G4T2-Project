// Frontend representations of your Java DTOs.
// BigDecimal -> number on the wire (Jackson default). LocalDateTime -> ISO string.

export interface TariffCalculationRequest {
  homeCountry: string;
  destinationCountry: string;
  productName: string;
  productValue: number;   // BigDecimal in Java, number in JSON
  hsCode: string;
  tradeAgreement: string;
}

export interface TariffCalculationResult {
  homeCountry: string;
  destinationCountry: string;
  productName: string;
  productValue: number;   // BigDecimal -> number
  tariffRate: number;     // BigDecimal -> number (e.g., 0.05 for 5%)
  tariffAmount: number;   // BigDecimal -> number
  totalCost: number;      // BigDecimal -> number
  currency: string;
  tradeAgreement: string;
  calculationDate: string; // LocalDateTime -> ISO string
}

// Wrapper your endpoint returns
export interface TariffCalculationResponse {
  message: string;
  data: TariffCalculationResult;
}
