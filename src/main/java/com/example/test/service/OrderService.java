package com.example.test.service;

import com.example.test.entity.Order;
import com.example.test.request.OrderRequest;

public interface OrderService {

    Order getOrderById(Long id) throws Exception;

    Order createOrder(OrderRequest request) throws Exception;

    Order updateOrder(Long id, OrderRequest request) throws Exception;

    void deleteOrder(Long id) throws Exception;
}
