package com.example.test.service;

import com.example.test.entity.Product;
import com.example.test.request.ProductRequest;

import java.util.List;

public interface ProductService {

    Product getProductById(Long id) throws Exception;

    List<Product> getAllProducts(Long categoryId) throws Exception;

    Product createProduct(ProductRequest request) throws Exception;

    Product updateProduct(Long id, ProductRequest request) throws Exception;

    void deleteProduct(Long id) throws Exception;

    // Dùng khi đặt hàng: trừ tồn kho, ném lỗi nếu không đủ hàng
    void decreaseStock(Long productId, int quantity) throws Exception;

    // Dùng khi hủy đơn: hoàn lại tồn kho
    void increaseStock(Long productId, int quantity) throws Exception;
}
