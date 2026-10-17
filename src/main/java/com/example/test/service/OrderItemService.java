package com.example.test.service;

import com.example.test.entity.OrderItem;
import com.example.test.request.OrderItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface OrderItemService {

    List<OrderItem> getItemsByOrderId(Long orderId) throws Exception;

    // Tạo các dòng sản phẩm cho đơn: lấy giá từ Product, trừ tồn kho, lưu OrderItem
    List<OrderItem> createItems(Long orderId, List<OrderItemRequest> requests) throws Exception;

    // Tổng tiền các dòng = tổng subtotal
    BigDecimal calculateTotal(List<OrderItem> items);
}
