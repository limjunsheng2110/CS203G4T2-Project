package com.cs203.tariffg4t2.controller;

import com.cs203.tariffg4t2.model.basic.Product;
import com.cs203.tariffg4t2.service.basic.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private Product product1;
    private Product product2;
    private List<Product> productList;

    @BeforeEach
    void setUp() {
        // Setup test products
        product1 = new Product();
        product1.setHsCode("010329");
        product1.setDescription("Live swine, weighing >= 50 kg");
        product1.setCategory("Live Animals");

        product2 = new Product();
        product2.setHsCode("020130");
        product2.setDescription("Meat of bovine animals, fresh or chilled");
        product2.setCategory("Meat");

        productList = Arrays.asList(product1, product2);
    }

    // test all the GET endpoints

    @Test
    void testGetAllProducts_Success() throws Exception {
        // given
        when(productService.getAllProducts()).thenReturn(productList);

        // when and then
        mockMvc.perform(get("/api/products/all"))
                .andExpect(status().isCreated()) // Your controller returns 201
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hsCode").value("010329"))
                .andExpect(jsonPath("$[0].description").value("Live swine, weighing >= 50 kg"))
                .andExpect(jsonPath("$[1].hsCode").value("020130"));
    }

    @Test
    void testGetAllProducts_EmptyList() throws Exception {
        // given
        when(productService.getAllProducts()).thenReturn(Arrays.asList());

        // when and then
        mockMvc.perform(get("/api/products/all"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAllProducts_ServiceException() throws Exception {
        // given
        when(productService.getAllProducts()).thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(get("/api/products/all"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("Error fetching products: Database error"));
    }

    // test GET by hscode

    @Test
    void testGetProductByHsCode_Success() throws Exception {
        // given
        when(productService.getProductByHsCode("010329")).thenReturn(product1);

        // when and then
        mockMvc.perform(get("/api/products/010329"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hsCode").value("010329"))
                .andExpect(jsonPath("$.description").value("Live swine, weighing >= 50 kg"))
                .andExpect(jsonPath("$.category").value("Live Animals"));
    }

    @Test
    void testGetProductByHsCode_NotFound() throws Exception {
        // given
        when(productService.getProductByHsCode("999999")).thenReturn(null);

        // when and then
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Product with HS Code 999999 not found"));
    }

    @Test
    void testGetProductByHsCode_ServiceException() throws Exception {
        // given
        when(productService.getProductByHsCode(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // when and then
        mockMvc.perform(get("/api/products/010329"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("Error fetching product: Database connection failed"));
    }

    // test creating of a product

    @Test
    void testAddProduct_Success() throws Exception {
        // given
        when(productService.createProduct("010329", "Live swine, weighing >= 50 kg", "Live Animals"))
                .thenReturn(product1);

        // when and then
        mockMvc.perform(post("/api/products/add")
                .param("hsCode", "010329")
                .param("description", "Live swine, weighing >= 50 kg")
                .param("category", "Live Animals"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hsCode").value("010329"))
                .andExpect(jsonPath("$.description").value("Live swine, weighing >= 50 kg"))
                .andExpect(jsonPath("$.category").value("Live Animals"));
    }

    @Test
    void testAddProduct_WithoutCategory() throws Exception {
        // given
        Product productWithoutCategory = new Product();
        productWithoutCategory.setHsCode("010329");
        productWithoutCategory.setDescription("Live swine, weighing >= 50 kg");

        when(productService.createProduct("010329", "Live swine, weighing >= 50 kg", null))
                .thenReturn(productWithoutCategory);

        // when and then
        mockMvc.perform(post("/api/products/add")
                .param("hsCode", "010329")
                .param("description", "Live swine, weighing >= 50 kg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hsCode").value("010329"));
    }

    @Test
    void testAddProduct_EmptyHsCode() throws Exception {
        // given
        when(productService.createProduct(eq(""), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("HS Code cannot be empty"));

        // when and then
        mockMvc.perform(post("/api/products/add")
                .param("hsCode", "")
                .param("description", "Some description")
                .param("category", "Some category"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid input: HS Code cannot be empty"));
    }

    @Test
    void testAddProduct_EmptyDescription() throws Exception {
        // given
        when(productService.createProduct(anyString(), eq(""), anyString()))
                .thenThrow(new IllegalArgumentException("Description cannot be empty"));

        // when and then
        mockMvc.perform(post("/api/products/add")
                .param("hsCode", "010329")
                .param("description", "")
                .param("category", "Live Animals"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid input: Description cannot be empty"));
    }

    @Test
    void testAddProduct_DuplicateHsCode() throws Exception {
        // given
        when(productService.createProduct(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Product with HS Code 010329 already exists"));

        // when and then
        mockMvc.perform(post("/api/products/add")
                .param("hsCode", "010329")
                .param("description", "Live swine, weighing >= 50 kg")
                .param("category", "Live Animals"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid input: Product with HS Code 010329 already exists"));
    }

    // test PUT product endpoint

    @Test
    void testUpdateProduct_Success() throws Exception {
        // given
        Product updatedProduct = new Product();
        updatedProduct.setHsCode("010329");
        updatedProduct.setDescription("Updated description");
        updatedProduct.setCategory("Updated category");

        when(productService.updateProductByHsCode("010329", "Updated description", "Updated category"))
                .thenReturn(updatedProduct);

        // when and then
        mockMvc.perform(put("/api/products/update/010329")
                .param("description", "Updated description")
                .param("category", "Updated category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hsCode").value("010329"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.category").value("Updated category"));
    }

    @Test
    void testUpdateProduct_OnlyDescription() throws Exception {
        // given
        Product updatedProduct = new Product();
        updatedProduct.setHsCode("010329");
        updatedProduct.setDescription("Updated description");
        updatedProduct.setCategory("Live Animals");

        when(productService.updateProductByHsCode("010329", "Updated description", null))
                .thenReturn(updatedProduct);

        // when and then
        mockMvc.perform(put("/api/products/update/010329")
                .param("description", "Updated description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void testUpdateProduct_OnlyCategory() throws Exception {
        // given
        Product updatedProduct = new Product();
        updatedProduct.setHsCode("010329");
        updatedProduct.setDescription("Live swine, weighing >= 50 kg");
        updatedProduct.setCategory("Updated category");

        when(productService.updateProductByHsCode("010329", null, "Updated category"))
                .thenReturn(updatedProduct);

        // when and then
        mockMvc.perform(put("/api/products/update/010329")
                .param("category", "Updated category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Updated category"));
    }

    @Test
    void testUpdateProduct_ProductNotFound() throws Exception {
        // given
        when(productService.updateProductByHsCode(eq("999999"), anyString(), anyString()))
                .thenReturn(null);

        // when and then
        mockMvc.perform(put("/api/products/update/999999")
                .param("description", "New description"))
                .andExpect(status().isNotFound()) // Changed from isBadRequest() to isNotFound()
                .andExpect(jsonPath("$").value("Product with HS Code 999999 not found"));
    }

    // test DELETE product endpoint

    @Test
    void testDeleteProduct_Success() throws Exception {
        // given
        when(productService.deleteProductByHsCode("010329")).thenReturn(true);

        // when and then
        mockMvc.perform(delete("/api/products/delete/010329"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Product with HS Code 010329 deleted"));
    }

    @Test
    void testDeleteProduct_NotFound() throws Exception {
        // given
        when(productService.deleteProductByHsCode("999999")).thenReturn(false);

        // when and then
        mockMvc.perform(delete("/api/products/delete/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Product with HS Code 999999 not found"));
    }

    @Test
    void testDeleteProduct_EmptyHsCode() throws Exception {
        // given
        when(productService.deleteProductByHsCode(""))
                .thenThrow(new IllegalArgumentException("Country code cannot be null or empty"));

        // when and then
        mockMvc.perform(delete("/api/products/delete/"))
                .andExpect(status().isNotFound()); // 404 because the path doesn't match
    }

    @Test
    void testDeleteProduct_ServiceException() throws Exception {
        // given
        when(productService.deleteProductByHsCode(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // when and then
        mockMvc.perform(delete("/api/products/delete/010329"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("Error deleting product: Database error"));
    }
}