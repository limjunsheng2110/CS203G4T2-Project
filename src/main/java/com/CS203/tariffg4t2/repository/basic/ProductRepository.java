package com.CS203.tariffg4t2.repository.basic;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.CS203.tariffg4t2.model.basic.Product;

public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByHsCode(String hsCode);
}
