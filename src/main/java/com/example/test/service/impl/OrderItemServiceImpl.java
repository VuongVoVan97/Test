package com.example.test.service.impl;

import com.example.test.entity.OrderItem;
import com.example.test.entity.Product;
import com.example.test.repository.OrderItemRepository;
import com.example.test.repository.ProductRepository;
import com.example.test.request.OrderItemRequest;
import com.example.test.service.OrderItemService;
import com.example.test.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository, ProductRepository productRepository, ProductService productService) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Override
    public List<OrderItem> getItemsByOrderId(Long orderId) throws Exception {

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }

        return orderItemRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public List<OrderItem> createItems(Long orderId, List<OrderItemRequest> requests) throws Exception {
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest request : requests) {

            // 1. Validate quantity > 0
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than 0"
                );
            }

            // 2. Lấy Product và kiểm tra tồn tại
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() ->
                            new Exception("Product not found: " + request.getProductId())
                    );

            // 3. Tạo OrderItem và copy snapshot từ Product
            OrderItem item = new OrderItem();

            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(request.getQuantity());

            // 4. Tính subtotal = unitPrice * quantity
            BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            item.setSubtotal(subtotal);

            // 5. Giảm tồn kho
            productService.decreaseStock(
                    product.getId(),
                    item.getQuantity()
            );

            // 6. Thêm vào danh sách
            items.add(item);
        }

        // 7. Lưu tất cả OrderItem
        return orderItemRepository.saveAll(items);
    }

    @Override
    public BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal subtotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    item.setSubtotal(subtotal);

                    return subtotal;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
