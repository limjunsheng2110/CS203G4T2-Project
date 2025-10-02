package com.CS203.tariffg4t2.controller;


import com.CS203.tariffg4t2.model.basic.Product;
import com.CS203.tariffg4t2.service.basic.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    ProductService productService;
    //GET ALL PRODUCTS
    @GetMapping("/all")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            return ResponseEntity.status(201).body(products);
        } catch (Exception e) {
            System.out.println("Error in getAllProducts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching products: " + e.getMessage());
        }
    }



    //GET PRODUCT BY HSCODE
    @GetMapping("/{hsCode}")
    public ResponseEntity<?> getProductByHsCode(@PathVariable String hsCode) {
        try {
            Product product = productService.getProductByHsCode(hsCode);
            if (product == null) {
                return ResponseEntity.status(404).body("Product with HS Code " + hsCode + " not found");
            }

            return ResponseEntity.ok(product);
        } catch (Exception e) {
            System.out.println("Error in getProductByHsCode: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching product: " + e.getMessage());
        }
    }


    //CREATE PRODUCT
    @PostMapping("/add")
    public ResponseEntity<?> addProduct(
            @RequestParam String hsCode,
            @RequestParam String description,
            @RequestParam(required = false) String category
    ) {

        try {
            Product newProduct = productService.createProduct(hsCode, description, category);
            return ResponseEntity.status(201).body(newProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error in addProduct: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error adding product: " + e.getMessage());
        }

    }


    //UPDATE PRODUCT
    @PutMapping("/update/{hsCode}")
    public ResponseEntity<?> updateProduct (
            @PathVariable String hsCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category
    ) {
        try {
            Product updatedProduct = productService.updateProductByHsCode(hsCode, description, category);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Invalid input: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error in updateProduct: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating product: " + e.getMessage());
        }

    }


    //DELETE PRODUCT BY HSCODE
    @DeleteMapping("/delete/{hsCode}")
    public ResponseEntity<?> deleteProductByHsCode(@PathVariable String hsCode) {
        try {
            boolean deleted = productService.deleteProductByHsCode(hsCode);
            if (!deleted) {
                return ResponseEntity.status(404).body("Product with HS Code " + hsCode + " not found");
            }
            return ResponseEntity.ok("Product with HS Code " + hsCode + " deleted");
        } catch (Exception e) {
            System.out.println("Error in deleteProductByHsCode: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting product: " + e.getMessage());
        }

    }
}
