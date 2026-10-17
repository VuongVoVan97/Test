package com.example.test.controller;

import com.example.test.entity.Product;
import com.example.test.request.ProductRequest;
import com.example.test.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // GET /product hoặc GET /product?categoryId=1
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) Long categoryId) throws Exception {
        return ResponseEntity.ok(productService.getAllProducts(categoryId));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) throws Exception {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) throws Exception {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
