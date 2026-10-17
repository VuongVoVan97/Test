package com.example.test.controller;

import com.example.test.request.OrderRequest;
import com.example.test.entity.Order;
import com.example.test.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id)
            throws Exception {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // Create order
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequest request) throws Exception {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // Update order
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderRequest request) throws Exception {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    // Delete order
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id)
            throws Exception {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}