package com.cs203.tariffg4t2.repository.basic;

import com.cs203.tariffg4t2.model.basic.Product;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByHsCode(String hsCode);
}
