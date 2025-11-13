package com.cs203.tariffg4t2.service.basic;

import com.cs203.tariffg4t2.dto.basic.ShippingRateDTO;
import com.cs203.tariffg4t2.model.basic.Country;
import com.cs203.tariffg4t2.model.basic.ShippingRate;
import com.cs203.tariffg4t2.repository.basic.CountryRepository;
import com.cs203.tariffg4t2.repository.basic.ShippingRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippingRateRepository shippingRateRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private ShippingService shippingService;

    private Country usCountry;
    private Country cnCountry;
    private ShippingRate testShippingRate;
    private ShippingRateDTO testShippingRateDTO;

    @BeforeEach
    void setUp() {
        usCountry = new Country();
        usCountry.setCountryCode("US");
        usCountry.setCountryName("United States");

        cnCountry = new Country();
        cnCountry.setCountryCode("CN");
        cnCountry.setCountryName("China");

        testShippingRate = new ShippingRate();
        testShippingRate.setId(1L);
        testShippingRate.setImportingCountry(usCountry);
        testShippingRate.setExportingCountry(cnCountry);
        testShippingRate.setAirRate(new BigDecimal("5.50"));
        testShippingRate.setSeaRate(new BigDecimal("2.25"));

        testShippingRateDTO = new ShippingRateDTO(
                1L,
                "US",
                "CN",
                new BigDecimal("5.50"),
                new BigDecimal("2.25")
        );
    }

    @Test
    void getShippingRate_AirMode_ReturnsAirRate() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(rates);

        BigDecimal result = shippingService.getShippingRate("AIR", "US", "CN");

        assertEquals(new BigDecimal("5.50"), result);
        verify(shippingRateRepository, times(1)).findByImportingAndExportingCountry("US", "CN");
    }

    @Test
    void getShippingRate_SeaMode_ReturnsSeaRate() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(rates);

        BigDecimal result = shippingService.getShippingRate("SEA", "US", "CN");

        assertEquals(new BigDecimal("2.25"), result);
    }

    @Test
    void getShippingRate_LowercaseAirMode_ReturnsAirRate() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(rates);

        BigDecimal result = shippingService.getShippingRate("air", "US", "CN");

        assertEquals(new BigDecimal("5.50"), result);
    }

    @Test
    void getShippingRate_MixedCaseSeaMode_ReturnsSeaRate() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(rates);

        BigDecimal result = shippingService.getShippingRate("SeA", "US", "CN");

        assertEquals(new BigDecimal("2.25"), result);
    }

    @Test
    void getShippingRate_InvalidMode_ReturnsNull() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(rates);

        BigDecimal result = shippingService.getShippingRate("RAIL", "US", "CN");

        assertNull(result);
    }

    @Test
    void getShippingRate_NullMode_ReturnsNull() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(rates);

        BigDecimal result = shippingService.getShippingRate(null, "US", "CN");

        assertNull(result);
    }

    @Test
    void getShippingRate_EmptyRateList_ReturnsNull() {
        when(shippingRateRepository.findByImportingAndExportingCountry("US", "CN")).thenReturn(new ArrayList<>());

        BigDecimal result = shippingService.getShippingRate("AIR", "US", "CN");

        assertNull(result);
    }

    @Test
    void getShippingRate_NoRatesFound_ReturnsNull() {
        when(shippingRateRepository.findByImportingAndExportingCountry("JP", "KR")).thenReturn(List.of());

        BigDecimal result = shippingService.getShippingRate("AIR", "JP", "KR");

        assertNull(result);
    }

    @Test
    void getAllShippingRates_ReturnsAllRates() {
        List<ShippingRate> rates = List.of(testShippingRate);
        when(shippingRateRepository.findAll()).thenReturn(rates);

        List<ShippingRateDTO> result = shippingService.getAllShippingRates();

        assertEquals(1, result.size());
        assertEquals("US", result.get(0).getImportingCountryCode());
        assertEquals("CN", result.get(0).getExportingCountryCode());
    }

    @Test
    void getAllShippingRates_EmptyList_ReturnsEmptyList() {
        when(shippingRateRepository.findAll()).thenReturn(new ArrayList<>());

        List<ShippingRateDTO> result = shippingService.getAllShippingRates();

        assertTrue(result.isEmpty());
    }

    @Test
    void getShippingRateById_Exists_ReturnsRate() {
        when(shippingRateRepository.findById(1L)).thenReturn(Optional.of(testShippingRate));

        Optional<ShippingRateDTO> result = shippingService.getShippingRateById(1L);

        assertTrue(result.isPresent());
        assertEquals("US", result.get().getImportingCountryCode());
        assertEquals("CN", result.get().getExportingCountryCode());
    }

    @Test
    void getShippingRateById_NotExists_ReturnsEmpty() {
        when(shippingRateRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<ShippingRateDTO> result = shippingService.getShippingRateById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void createShippingRate_ValidDTO_CreatesRate() {
        when(countryRepository.findByCountryCode("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCode("CN")).thenReturn(Optional.of(cnCountry));
        when(shippingRateRepository.save(any(ShippingRate.class))).thenReturn(testShippingRate);

        ShippingRateDTO result = shippingService.createShippingRate(testShippingRateDTO);

        assertNotNull(result);
        assertEquals("US", result.getImportingCountryCode());
        assertEquals("CN", result.getExportingCountryCode());
        verify(shippingRateRepository, times(1)).save(any(ShippingRate.class));
    }

    @Test
    void createShippingRate_ImportingCountryNotFound_ThrowsException() {
        when(countryRepository.findByCountryCode("US")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            shippingService.createShippingRate(testShippingRateDTO);
        });
    }

    @Test
    void createShippingRate_ExportingCountryNotFound_ThrowsException() {
        when(countryRepository.findByCountryCode("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCode("CN")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            shippingService.createShippingRate(testShippingRateDTO);
        });
    }

    @Test
    void updateShippingRate_Exists_UpdatesRate() {
        when(shippingRateRepository.findById(1L)).thenReturn(Optional.of(testShippingRate));
        when(countryRepository.findByCountryCode("US")).thenReturn(Optional.of(usCountry));
        when(countryRepository.findByCountryCode("CN")).thenReturn(Optional.of(cnCountry));
        when(shippingRateRepository.save(any(ShippingRate.class))).thenReturn(testShippingRate);

        ShippingRateDTO updatedDTO = new ShippingRateDTO(
                1L, "US", "CN",
                new BigDecimal("6.00"),
                new BigDecimal("3.00")
        );

        Optional<ShippingRateDTO> result = shippingService.updateShippingRate(1L, updatedDTO);

        assertTrue(result.isPresent());
        verify(shippingRateRepository, times(1)).save(any(ShippingRate.class));
    }

    @Test
    void updateShippingRate_NotExists_ReturnsEmpty() {
        when(shippingRateRepository.findById(999L)).thenReturn(Optional.empty());

        ShippingRateDTO updatedDTO = new ShippingRateDTO(
                999L, "US", "CN",
                new BigDecimal("6.00"),
                new BigDecimal("3.00")
        );

        Optional<ShippingRateDTO> result = shippingService.updateShippingRate(999L, updatedDTO);

        assertFalse(result.isPresent());
        verify(shippingRateRepository, never()).save(any(ShippingRate.class));
    }

    @Test
    void deleteShippingRate_Exists_ReturnsTrue() {
        when(shippingRateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(shippingRateRepository).deleteById(1L);

        boolean result = shippingService.deleteShippingRate(1L);

        assertTrue(result);
        verify(shippingRateRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteShippingRate_NotExists_ReturnsFalse() {
        when(shippingRateRepository.existsById(999L)).thenReturn(false);

        boolean result = shippingService.deleteShippingRate(999L);

        assertFalse(result);
        verify(shippingRateRepository, never()).deleteById(anyLong());
    }
}

