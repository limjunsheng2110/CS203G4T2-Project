package com.CS203.tariffg4t2.service.tariffLogic;

import com.CS203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.CS203.tariffg4t2.dto.response.TariffCalculationResultDTO;
import com.CS203.tariffg4t2.model.basic.Product;
import com.CS203.tariffg4t2.model.basic.TariffRate;
import com.CS203.tariffg4t2.service.basic.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TariffCalculatorService {

    @Autowired
    private TariffRateService tariffRateService;

    @Autowired
    private TariffFTAService tariffFTAService;

    @Autowired
    private ShippingCostService shippingCostService;

    @Autowired
    TariffValidationService tariffValidationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PreferentialRateService preferentialRateService;

    @Autowired
    private TradeAgreementService tradeAgreementService;

    @Autowired
    private TariffRateCRUDService tariffRateCRUDService;

    @Autowired
    private ShippingService shippingService;

    public TariffCalculationResultDTO calculateTariff(TariffCalculationRequestDTO request) {

        System.out.println("Received tariff calculation request: " + request);

        // First validate the request using TariffValidationService
        tariffValidationService.validateTariffRequest(request);

        System.out.println("Request validated successfully.");

        //then call TariffRateService to calculate based on ad valorem or specific
        //it should return an amount.

        BigDecimal dutyAmount;
        dutyAmount = tariffRateService.calculateTariffAmount(request);

        System.out.println("Initial calculated duty amount: " + dutyAmount);

        // get preferential rate from TariffFTAService
        dutyAmount = tariffFTAService.applyTradeAgreementDiscount(request, dutyAmount);

        System.out.println("Discounted duty amount after FTA: " + dutyAmount);

        // Calculate shipping cost using ShippingCostService, return amount
        BigDecimal shippingCost = shippingCostService.calculateShippingCost(request);

        System.out.println("Calculated shipping cost: " + shippingCost);
        System.out.println("Calculated duty amount: " + dutyAmount);
        System.out.println("Product value: " + request.getProductValue());

        //Total Cost = product value + duty amount + shipping cost
        BigDecimal totalCost = request.getProductValue().add(dutyAmount).add(shippingCost);

        // Build and return result

        //get product description from tariffRateService
        Product product = productService.getProductByHsCode(request.getHsCode());
        String productDescription = (product != null) ? product.getDescription() : "N/A";

        // Fetch trade agreement information
        String tradeAgreementName = tradeAgreementService.getTradeAgreementName(
                request.getExportingCountry(),
                request.getImportingCountry(),
                request.getHsCode());

        // Fetch tariff rate details for TariffType
        Optional<TariffRate> tariffRateOptional = tariffRateCRUDService.getTariffRateByDetails(
                request.getHsCode(),
                request.getImportingCountry(),
                request.getExportingCountry()
        );

        if (tariffRateOptional.isEmpty()) {
            throw new RuntimeException("No tariff rate found for the given parameters");
        }

        TariffRate tariffRate = tariffRateOptional.get();

        BigDecimal adValoremRate = tariffRate.getAdValoremRate();
        BigDecimal specificRate = tariffRate.getSpecificRateAmount();

        // Fetch preferential rates
        BigDecimal adValoremPreferentialRate = preferentialRateService.getAdValoremPreferentialRate(
                request.getImportingCountry(),
                request.getExportingCountry(),
                request.getHsCode()
        );

        BigDecimal specificPreferentialRate = preferentialRateService.getSpecificPreferentialRate(
                request.getImportingCountry(),
                request.getExportingCountry(),
                request.getHsCode()
        );


        // Re-fetch shipping cost rate for record (not strictly needed here)

        BigDecimal shippingCostRate = shippingService.getShippingRate(
                request.getShippingMode(), request.getImportingCountry(), request.getExportingCountry());

        String tariffType = tariffRate.getTariffType();

        return TariffCalculationResultDTO.builder()
                .importingCountry(request.getImportingCountry())
                .exportingCountry(request.getExportingCountry())
                .productValue(request.getProductValue())
                .productDescription(productDescription)
                .hsCode(request.getHsCode())
                .totalWeight(request.getTotalWeight())
                .heads(request.getHeads())
                .TariffType(tariffType)
                .tariffAmount(dutyAmount.setScale(2, RoundingMode.HALF_UP))
                .shippingCost(shippingCost.setScale(2, RoundingMode.HALF_UP))
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .tradeAgreement(tradeAgreementName)
                .calculationDate(LocalDateTime.now())
                .adValoremRate(adValoremRate)
                .specificRate(specificRate)
                .adValoremPreferentialRate(adValoremPreferentialRate)
                .specificPreferentialRate(specificPreferentialRate)
                .shippingRate(shippingCostRate)
                .build();
    }

}
