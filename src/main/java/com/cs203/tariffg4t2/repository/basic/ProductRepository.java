package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.Product;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByHsCode(String hsCode);
    
    /**
     * Search products by matching tokens in description or category
     * Case-insensitive search using ILIKE (PostgreSQL)
     * @param token Search token
     * @return List of matching products
     */
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :token, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :token, '%')) OR " +
           "p.hsCode LIKE CONCAT(:token, '%')")
    List<Product> searchByToken(@Param("token") String token);
}
