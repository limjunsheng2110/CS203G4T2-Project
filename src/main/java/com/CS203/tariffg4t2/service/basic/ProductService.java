package com.CS203.tariffg4t2.service.basic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CS203.tariffg4t2.model.basic.Product;
import com.CS203.tariffg4t2.repository.basic.ProductRepository;

import java.util.List;

@Slf4j
@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    //CREATE PRODUCT

    public Product createProduct(String hsCode, String description, String category) {

        //HsCode and description cannot be empty

        if (hsCode == null || hsCode.isEmpty()) {
            throw new IllegalArgumentException("HS Code cannot be empty");
        }

        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        //Product already exists
        if (productRepository.existsById(hsCode)) {
            throw new IllegalArgumentException("Product with HS Code " + hsCode + " already exists");
        }

        Product p = new Product();
        p.setHsCode(hsCode);
        p.setDescription(description);
        p.setCategory(category);

        return productRepository.save(p);
    }


    //READ PRODUCT BY HSCODE
    public Product getProductByHsCode(String hsCode) {
        return productRepository.findById(hsCode).orElse(null);
    }

    //DELETE PRODUCT BY HSCODE
    public boolean deleteProductByHsCode(String hsCode) {

        if (hsCode == null || hsCode.isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be null or empty");
        }

        if (!productRepository.existsById(hsCode)) {
            return false;
        }
        productRepository.deleteById(hsCode);
        return true;
    }

    //UPDATE PRODUCT BY HSCODE

    public Product updateProductByHsCode(String hsCode, String description, String category) {
        Product existingProduct = productRepository.findById(hsCode).orElse(null);

        if (existingProduct == null) {
            throw new IllegalArgumentException("Product with HS Code " + hsCode + " does not exist");
        }

        if (description != null && !description.isEmpty()) {
            existingProduct.setDescription(description);
        }
        if (category != null) {
            existingProduct.setCategory(category);
        }

        return productRepository.save(existingProduct);
    }


    //GET ALL PRODUCTS

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


}
