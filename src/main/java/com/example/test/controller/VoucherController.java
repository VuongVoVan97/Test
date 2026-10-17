package com.example.test.controller;

import com.example.test.entity.Customer;
import com.example.test.entity.Order;
import com.example.test.service.CustomerService;
import com.example.test.service.OrderService;
import com.example.test.service.VoucherService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    private final VoucherService voucherService;
    private final CustomerService customerService;
    private final OrderService orderService;

    public VoucherController(
            VoucherService voucherService,
            CustomerService customerService,
            OrderService orderService) {
        this.voucherService = voucherService;
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkVoucher(
            @RequestParam Long customerId,
            @RequestParam Long orderId,
            @RequestParam String code) throws Exception {

        Customer customer =
                customerService.getCustomerById(customerId);

        Order order =
                orderService.getOrderById(orderId);

        boolean valid =
                voucherService.canUseVoucher(customer, order, code);

        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "orderId", orderId,
                "code", code,
                "valid", valid,
                "message", valid
                        ? "Voucher can be used"
                        : "Voucher cannot be used"
        ));
    }
}
