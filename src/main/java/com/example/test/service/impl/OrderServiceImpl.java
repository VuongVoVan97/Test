package com.example.test.service.impl;

import com.example.test.exception.NotFoundException;
import com.example.test.repository.CustomerRepository;
import com.example.test.request.OrderRequest;
import com.example.test.entity.Order;
import com.example.test.repository.OrderRepository;
import com.example.test.service.OrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderServiceImpl(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public Order getOrderById(Long id) throws Exception {
        if (id == null) {
            throw new IllegalArgumentException("Order id must not be null");
        }

        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    @Override
    public Order createOrder(OrderRequest request) throws Exception {
        validate(request);

        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(request.getTotalAmount());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());

        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public Order updateOrder(Long id, OrderRequest request) throws Exception {
        validate(request);

        Order order = getOrderById(id);

        order.setCustomerId(request.getCustomerId());
        order.setTotalAmount(request.getTotalAmount());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public void deleteOrder(Long id) throws Exception {
        orderRepository.delete(getOrderById(id));
    }

    private void validate(OrderRequest request) throws Exception {
        if (Objects.isNull(request)) {
            throw new IllegalArgumentException("Order request is not valid");
        }
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("Order customerId is required");
        }
        if (request.getTotalAmount() == null
                || request.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Order totalAmount is required and must be >= 0");
        }
        if (!customerRepository.existsById(request.getCustomerId())) {
            throw new NotFoundException("Customer not found: " + request.getCustomerId());
        }
    }
}
