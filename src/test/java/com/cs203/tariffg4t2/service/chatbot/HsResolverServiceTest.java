package com.cs203.tariffg4t2.service.chatbot;

import com.cs203.tariffg4t2.dto.chatbot.HsCandidateDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveRequestDTO;
import com.cs203.tariffg4t2.dto.chatbot.HsResolveResponseDTO;
import com.cs203.tariffg4t2.model.basic.Product;
import com.cs203.tariffg4t2.repository.basic.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HsResolverServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private HsChatSessionLogService hsChatSessionLogService;

    @InjectMocks
    private HsResolverService hsResolverService;

    private Product chickenProduct;
    private Product goatProduct;
    private HsResolveRequestDTO request;

    @BeforeEach
    void setUp() {
        // Setup test products
        chickenProduct = new Product();
        chickenProduct.setHsCode("010511");
        chickenProduct.setDescription("Live poultry, chickens");
        chickenProduct.setCategory("Live Animals");

        goatProduct = new Product();
        goatProduct.setHsCode("010420");
        goatProduct.setDescription("Live goats");
        goatProduct.setCategory("Live Animals");

        // Setup test request
        request = new HsResolveRequestDTO();
        request.setProductName("chicken");
        request.setDescription("live chickens for farming");
        request.setSessionId("test-session");
        request.setQueryId("test-query");
        request.setConsentLogging(true);
    }

    @Test
    void testResolveHsCode_WithMatchingProducts() {
        // given
        when(productRepository.searchByToken("chicken")).thenReturn(Arrays.asList(chickenProduct));
        when(productRepository.searchByToken("live")).thenReturn(Arrays.asList(chickenProduct, goatProduct));
        when(productRepository.searchByToken("farming")).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getQueryId()).isEqualTo("test-query");
        assertThat(response.getSessionId()).isEqualTo("test-session");
        assertThat(response.getCandidates()).isNotEmpty();
        assertThat(response.getCandidates()).hasSizeLessThanOrEqualTo(3);
        
        HsCandidateDTO topCandidate = response.getCandidates().get(0);
        assertThat(topCandidate.getHsCode()).isEqualTo("010511");
        assertThat(topCandidate.getConfidence()).isGreaterThan(0.0);
        assertThat(topCandidate.getRationale()).isEqualTo("Live poultry, chickens");
        assertThat(topCandidate.getSource()).isEqualTo("PRODUCT_DATABASE");

        verify(hsChatSessionLogService, times(1)).recordInteraction(
                eq("test-session"),
                eq(true),
                any(HsResolveRequestDTO.class),
                any(HsResolveResponseDTO.class)
        );
    }

    @Test
    void testResolveHsCode_NoMatchingProducts() {
        // given
        when(productRepository.searchByToken(anyString())).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCandidates()).isEmpty();
        assertThat(response.getDisambiguationQuestions()).isEmpty();
        
        verify(hsChatSessionLogService, times(1)).recordInteraction(
                anyString(),
                eq(true),
                any(HsResolveRequestDTO.class),
                any(HsResolveResponseDTO.class)
        );
    }

    @Test
    void testResolveHsCode_GeneratesSessionId_WhenNotProvided() {
        // given
        request.setSessionId(null);
        when(productRepository.searchByToken(anyString())).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getSessionId()).isNotEmpty();
    }

    @Test
    void testResolveHsCode_GeneratesQueryId_WhenNotProvided() {
        // given
        request.setQueryId(null);
        when(productRepository.searchByToken(anyString())).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response.getQueryId()).isNotNull();
        assertThat(response.getQueryId()).isNotEmpty();
    }

    @Test
    void testResolveHsCode_LimitsTo3Candidates() {
        // given
        Product product1 = new Product();
        product1.setHsCode("010511");
        product1.setDescription("Product 1");

        Product product2 = new Product();
        product2.setHsCode("010512");
        product2.setDescription("Product 2");

        Product product3 = new Product();
        product3.setHsCode("010513");
        product3.setDescription("Product 3");

        Product product4 = new Product();
        product4.setHsCode("010514");
        product4.setDescription("Product 4");

        when(productRepository.searchByToken(anyString()))
                .thenReturn(Arrays.asList(product1, product2, product3, product4));

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response.getCandidates()).hasSize(3);
    }

    @Test
    void testResolveHsCode_RanksByConfidence() {
        // given
        Product exactMatch = new Product();
        exactMatch.setHsCode("010511");
        exactMatch.setDescription("Live chickens for farming");

        Product partialMatch = new Product();
        partialMatch.setHsCode("010420");
        partialMatch.setDescription("Live goats");

        when(productRepository.searchByToken("live")).thenReturn(Arrays.asList(exactMatch, partialMatch));
        when(productRepository.searchByToken("chickens")).thenReturn(Arrays.asList(exactMatch));
        when(productRepository.searchByToken("farming")).thenReturn(Arrays.asList(exactMatch));

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response.getCandidates()).isNotEmpty();
        HsCandidateDTO firstCandidate = response.getCandidates().get(0);
        assertThat(firstCandidate.getHsCode()).isEqualTo("010511");
        
        if (response.getCandidates().size() > 1) {
            HsCandidateDTO secondCandidate = response.getCandidates().get(1);
            assertThat(firstCandidate.getConfidence()).isGreaterThanOrEqualTo(secondCandidate.getConfidence());
        }
    }

    @Test
    void testResolveHsCode_HandlesNullProductName() {
        // given
        request.setProductName(null);
        when(productRepository.searchByToken(anyString())).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCandidates()).isEmpty();
    }

    @Test
    void testResolveHsCode_HandlesEmptyDescription() {
        // given
        request.setDescription("");
        when(productRepository.searchByToken(anyString())).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCandidates()).isEmpty();
    }

    @Test
    void testResolveHsCode_NoDisambiguationQuestions() {
        // given
        when(productRepository.searchByToken(anyString())).thenReturn(Arrays.asList(chickenProduct));

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response.getDisambiguationQuestions()).isEmpty();
    }

    @Test
    void testResolveHsCode_NoNotice() {
        // given
        when(productRepository.searchByToken(anyString())).thenReturn(Arrays.asList(chickenProduct));

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        assertThat(response.getNotice()).isNull();
    }

    @Test
    void testResolveHsCode_ConsentLoggingFalse() {
        // given
        request.setConsentLogging(false);
        when(productRepository.searchByToken(anyString())).thenReturn(List.of());

        // when
        HsResolveResponseDTO response = hsResolverService.resolveHsCode(request);

        // then
        verify(hsChatSessionLogService, times(1)).recordInteraction(
                anyString(),
                eq(false),
                any(HsResolveRequestDTO.class),
                any(HsResolveResponseDTO.class)
        );
    }
}
