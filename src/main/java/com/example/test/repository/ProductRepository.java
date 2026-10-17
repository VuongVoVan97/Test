package com.example.test.repository;

import com.example.test.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    List<Product> findByCategoryId(Long categoryId);
}
