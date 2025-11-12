package com.cs203.tariffg4t2.service.tariffLogic;

import com.cs203.tariffg4t2.dto.request.TariffCalculationRequestDTO;
import com.cs203.tariffg4t2.service.basic.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingCostServiceTest {

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingCostService shippingCostService;

    private TariffCalculationRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new TariffCalculationRequestDTO();
        testRequest.setShippingMode("AIR");
        testRequest.setImportingCountry("US");
        testRequest.setExportingCountry("CN");
        testRequest.setWeight(new BigDecimal("100"));
    }

    @Test
    void calculateShippingCost_ValidRequest_Success() {
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("550.00"), result);
        verify(shippingService, times(1)).getShippingRate("AIR", "US", "CN");
    }

    @Test
    void calculateShippingCost_NullRate_ReturnsZero() {
        when(shippingService.getShippingRate("AIR", "US", "CN")).thenReturn(null);

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateShippingCost_NullWeight_ReturnsZero() {
        testRequest.setWeight(null);
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateShippingCost_ZeroWeight_ReturnsZero() {
        testRequest.setWeight(BigDecimal.ZERO);
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateShippingCost_NegativeWeight_ReturnsZero() {
        testRequest.setWeight(new BigDecimal("-10"));
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateShippingCost_SeaMode_Success() {
        testRequest.setShippingMode("SEA");
        when(shippingService.getShippingRate("SEA", "US", "CN"))
                .thenReturn(new BigDecimal("2.25"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("225.00"), result);
    }

    @Test
    void calculateShippingCost_HighPrecision_RoundsCorrectly() {
        testRequest.setWeight(new BigDecimal("33.333"));
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.555"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("185.16"), result); // 33.333 * 5.555 = 185.16415, rounds to 185.16
    }

    @Test
    void calculateShippingCost_RoundingEdgeCase_RoundsUp() {
        testRequest.setWeight(new BigDecimal("10"));
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("1.555"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("15.55"), result); // 10 * 1.555 = 15.55
    }

    @Test
    void calculateShippingCost_RoundingEdgeCase_RoundsDown() {
        testRequest.setWeight(new BigDecimal("10"));
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("1.553"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("15.53"), result); // 10 * 1.553 = 15.53
    }

    @Test
    void calculateShippingCost_VeryLargeWeight_CalculatesCorrectly() {
        testRequest.setWeight(new BigDecimal("10000"));
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("3.75"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("37500.00"), result);
    }

    @Test
    void calculateShippingCost_VerySmallWeight_CalculatesCorrectly() {
        testRequest.setWeight(new BigDecimal("0.001"));
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("0.01"), result); // 0.001 * 5.50 = 0.0055, rounds to 0.01
    }

    @Test
    void calculateShippingCost_NullShippingMode_ReturnsZero() {
        testRequest.setShippingMode(null);
        when(shippingService.getShippingRate(null, "US", "CN")).thenReturn(null);

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateShippingCost_DifferentCountryPair_Success() {
        testRequest.setImportingCountry("SG");
        testRequest.setExportingCountry("JP");
        when(shippingService.getShippingRate("AIR", "SG", "JP"))
                .thenReturn(new BigDecimal("7.80"));

        BigDecimal result = shippingCostService.calculateShippingCost(testRequest);

        assertEquals(new BigDecimal("780.00"), result); // 100 * 7.80
    }

    @Test
    void getShippingRatePerKg_ValidRequest_Success() {
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = shippingCostService.getShippingRatePerKg(testRequest);

        assertEquals(new BigDecimal("5.50"), result);
        verify(shippingService, times(1)).getShippingRate("AIR", "US", "CN");
    }

    @Test
    void getShippingRatePerKg_NullRate_ReturnsZero() {
        when(shippingService.getShippingRate("AIR", "US", "CN")).thenReturn(null);

        BigDecimal result = shippingCostService.getShippingRatePerKg(testRequest);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getShippingRatePerKg_SeaMode_Success() {
        testRequest.setShippingMode("SEA");
        when(shippingService.getShippingRate("SEA", "US", "CN"))
                .thenReturn(new BigDecimal("2.25"));

        BigDecimal result = shippingCostService.getShippingRatePerKg(testRequest);

        assertEquals(new BigDecimal("2.25"), result);
    }

    @Test
    void getShippingRatePerKg_HighPrecision_RoundsCorrectly() {
        when(shippingService.getShippingRate("AIR", "US", "CN"))
                .thenReturn(new BigDecimal("5.5555"));

        BigDecimal result = shippingCostService.getShippingRatePerKg(testRequest);

        assertEquals(new BigDecimal("5.56"), result);
    }
}

