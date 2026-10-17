package com.example.test.service;

import com.example.test.entity.Customer;
import com.example.test.entity.Order;
import com.example.test.entity.Voucher;

import java.math.BigDecimal;

public interface VoucherService {

    Voucher getVoucherByCode(String code) throws Exception;

    boolean checkVoucher(String code, BigDecimal orderAmount)
            throws Exception;

    boolean canUseVoucher(Customer customer, Order order, String code);
}
