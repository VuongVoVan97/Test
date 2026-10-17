package com.example.test.service.impl;

import com.example.test.entity.OrderItem;
import com.example.test.repository.OrderItemRepository;
import com.example.test.request.OrderItemRequest;
import com.example.test.service.OrderItemService;
import com.example.test.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository, ProductService productService) {
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
    }

    @Override
    public List<OrderItem> getItemsByOrderId(Long orderId) throws Exception {
        // TODO: trả về các dòng của đơn
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    @Transactional
    public List<OrderItem> createItems(Long orderId, List<OrderItemRequest> requests) throws Exception {
        // TODO: với mỗi request:
        //   1. validate quantity > 0, productId tồn tại
        //   2. lấy Product -> copy name, price vào OrderItem (snapshot)
        //   3. subtotal = unitPrice * quantity
        //   4. productService.decreaseStock(...)
        //   5. lưu OrderItem
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public BigDecimal calculateTotal(List<OrderItem> items) {
        // TODO: cộng subtotal của tất cả items
        throw new UnsupportedOperationException("TODO");
    }
}
