package com.cs203.tariffg4t2.service;

import com.cs203.tariffg4t2.model.basic.Product;
import com.cs203.tariffg4t2.repository.basic.ProductRepository;
import com.cs203.tariffg4t2.service.basic.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;
    private Product product3;
    private List<Product> productList;

    @BeforeEach
    void setUp() {
        // setup test products
        product1 = new Product();
        product1.setHsCode("010329");
        product1.setDescription("Live swine, weighing 50 kg or more");
        product1.setCategory("Live Animals");

        product2 = new Product();
        product2.setHsCode("020130");
        product2.setDescription("Meat of bovine animals, fresh or chilled");
        product2.setCategory("Meat and Edible Meat Offal");

        product3 = new Product();
        product3.setHsCode("030212");
        product3.setDescription("Fresh or chilled salmon");
        product3.setCategory("Fish and Crustaceans");

        productList = Arrays.asList(product1, product2, product3);
    }

    // tests for getting all products

    @Test
    void testGetAllProducts_Success() {
        // given
        when(productRepository.findAll()).thenReturn(productList);

        // when
        List<Product> result = productService.getAllProducts();

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("010329", result.get(0).getHsCode());
        assertEquals("Live swine, weighing 50 kg or more", result.get(0).getDescription());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void testGetAllProducts_EmptyList() {
        // given
        when(productRepository.findAll()).thenReturn(Arrays.asList());

        // when
        List<Product> result = productService.getAllProducts();

        // then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void testGetAllProducts_RepositoryException() {
        // given
        when(productRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        // when & then
        assertThrows(RuntimeException.class, () -> {
            productService.getAllProducts();
        });
    }

    // tests for getting product by HS code

    @Test
    void testGetProductByHsCode_Success() {
        // given - ProductService uses findById(), not findByHsCode()
        when(productRepository.findById("010329")).thenReturn(Optional.of(product1));

        // when
        Product result = productService.getProductByHsCode("010329");

        // then
        assertNotNull(result);
        assertEquals("010329", result.getHsCode());
        assertEquals("Live swine, weighing 50 kg or more", result.getDescription());
        assertEquals("Live Animals", result.getCategory());
        verify(productRepository, times(1)).findById("010329");
    }

    @Test
    void testGetProductByHsCode_NotFound_ReturnsNull() {
        // given - ProductService uses findById(), not findByHsCode()
        when(productRepository.findById("999999")).thenReturn(Optional.empty());

        // when
        Product result = productService.getProductByHsCode("999999");

        // then
        assertNull(result); // Service returns null when not found
        verify(productRepository, times(1)).findById("999999");
    }

    @Test
    void testGetProductByHsCode_DifferentProducts() {
        // given
        when(productRepository.findById("020130")).thenReturn(Optional.of(product2));

        // when
        Product result = productService.getProductByHsCode("020130");

        // then
        assertNotNull(result);
        assertEquals("020130", result.getHsCode());
        assertEquals("Meat of bovine animals, fresh or chilled", result.getDescription());
        verify(productRepository, times(1)).findById("020130");
    }

    @Test
    void testGetProductByHsCode_MultipleCallsSameCode() {
        // given
        when(productRepository.findById("030212")).thenReturn(Optional.of(product3));

        // when
        Product result1 = productService.getProductByHsCode("030212");
        Product result2 = productService.getProductByHsCode("030212");

        // then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("030212", result1.getHsCode());
        assertEquals("030212", result2.getHsCode());
        verify(productRepository, times(2)).findById("030212");
    }

    // tests to save a product

    @Test
    void testSaveProduct_Success() {
        // given
        Product newProduct = new Product();
        newProduct.setHsCode("040510");
        newProduct.setDescription("Butter and dairy spreads");
        newProduct.setCategory("Dairy Products");
        
        when(productRepository.save(any(Product.class))).thenReturn(newProduct);

        // when
        Product result = productRepository.save(newProduct);

        // then
        assertNotNull(result);
        assertEquals("040510", result.getHsCode());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testSaveProduct_UpdateExisting() {
        // given
        product1.setDescription("Updated description");
        when(productRepository.save(product1)).thenReturn(product1);

        // when
        Product result = productRepository.save(product1);

        // then
        assertNotNull(result);
        assertEquals("Updated description", result.getDescription());
        verify(productRepository, times(1)).save(product1);
    }

    // ========== FIND BY HS CODE TESTS ==========

    @Test
    void testFindByHsCode_Success() {
        // given
        when(productRepository.findByHsCode("010329")).thenReturn(Optional.of(product1));

        // when
        Optional<Product> result = productRepository.findByHsCode("010329");

        // then
        assertTrue(result.isPresent());
        assertEquals("010329", result.get().getHsCode());
    }

    @Test
    void testFindByHsCode_NotFound() {
        // given
        when(productRepository.findByHsCode("999999")).thenReturn(Optional.empty());

        // when
        Optional<Product> result = productRepository.findByHsCode("999999");

        // then
        assertFalse(result.isPresent());
    }

    // ========== COUNT TESTS ==========

    @Test
    void testCountProducts_Success() {
        // given
        when(productRepository.count()).thenReturn(3L);

        // when
        long result = productRepository.count();

        // then
        assertEquals(3L, result);
        verify(productRepository, times(1)).count();
    }

    @Test
    void testCountProducts_Zero() {
        // given
        when(productRepository.count()).thenReturn(0L);

        // when
        long result = productRepository.count();

        // then
        assertEquals(0L, result);
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteProduct_Success() {
        // given
        doNothing().when(productRepository).delete(any(Product.class));

        // when
        productRepository.delete(product1);

        // then
        verify(productRepository, times(1)).delete(any(Product.class));
    }

    @Test
    void testDeleteById_Success() {
        // given
        doNothing().when(productRepository).deleteById(anyString());

        // when
        productRepository.deleteById("010329");

        // then
        verify(productRepository, times(1)).deleteById("010329");
    }

    @Test
    void testDeleteAll_Success() {
        // given
        doNothing().when(productRepository).deleteAll();

        // when
        productRepository.deleteAll();

        // then
        verify(productRepository, times(1)).deleteAll();
    }

    // tests for existence checks

    @Test
    void testExistsById_True() {
        // given
        when(productRepository.existsById("010329")).thenReturn(true);

        // when
        boolean result = productRepository.existsById("010329");

        // then
        assertTrue(result);
    }

    @Test
    void testExistsById_False() {
        // given
        when(productRepository.existsById("999999")).thenReturn(false);

        // when
        boolean result = productRepository.existsById("999999");

        // then
        assertFalse(result);
    }
}