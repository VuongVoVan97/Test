package com.example.test.controller;

import com.example.test.entity.OrderItem;
import com.example.test.service.OrderItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order/{orderId}/items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    // Xem các sản phẩm trong đơn. Việc tạo item đi qua POST /order (OrderService.createOrder).
    @GetMapping
    public ResponseEntity<List<OrderItem>> getItems(@PathVariable Long orderId) throws Exception {
        return ResponseEntity.ok(orderItemService.getItemsByOrderId(orderId));
    }
}
