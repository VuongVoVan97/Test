package com.example.test.service.impl;

import com.example.test.entity.Customer;
import com.example.test.entity.Order;
import com.example.test.entity.Voucher;
import com.example.test.exception.NotFoundException;
import com.example.test.repository.VoucherRepository;
import com.example.test.service.VoucherService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class VoucherServiceImpl implements VoucherService {

    private static final BigDecimal MIN_ORDER_AMOUNT = new BigDecimal("500000");
    private static final BigDecimal SALE50_MIN_ORDER_AMOUNT = new BigDecimal("2000000");

    private final VoucherRepository voucherRepository;

    public VoucherServiceImpl(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Override
    public Voucher getVoucherByCode(String code) throws Exception {
        if (!isCodeValid(code)) {
            throw new IllegalArgumentException("Voucher code is required");
        }

        return voucherRepository.findByCode(code.trim())
                .orElseThrow(() -> new NotFoundException("Voucher not found: " + code));
    }

    @Override
    public boolean checkVoucher(String code, BigDecimal orderAmount) throws Exception {
        Voucher voucher = getVoucherByCode(code);
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(voucher.getActive())) {
            return false;
        }
        if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
            return false;
        }
        if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
            return false;
        }
        if (voucher.getUsageLimit() != null
                && voucher.getUsedCount() != null
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return false;
        }

        return orderAmount != null
                && (voucher.getMinOrderAmount() == null
                || orderAmount.compareTo(voucher.getMinOrderAmount()) >= 0);
    }

    @Override
    public boolean canUseVoucher(
            Customer customer, Order order, String code) {

        if (customer == null || order == null) {
            return false;
        }

        // Order phải thuộc về đúng customer đang kiểm tra
        if (!Objects.equals(customer.getId(), order.getCustomerId())) {
            return false;
        }

        if (!isCustomerAllowed(customer)) {
            return false;
        }

        if (!isCodeValid(code)) {
            return false;
        }

        return isOrderEligible(order, code.trim());
    }

    private boolean isCustomerAllowed(Customer customer) {
        if ("BANNED".equalsIgnoreCase(customer.getStatus())) {
            return false;
        }

        int orderCount = customer.getOrderCount() == null ? 0 : customer.getOrderCount();

        return "VIP".equalsIgnoreCase(customer.getLevel())
                || orderCount >= 3;
    }

    private boolean isCodeValid(String code) {
        return code != null && !code.trim().isEmpty();
    }

    private boolean isOrderEligible(Order order, String code) {
        BigDecimal total = order.getTotalAmount();

        if (total == null
                || total.compareTo(MIN_ORDER_AMOUNT) < 0) {
            return false;
        }

        if ("SALE50".equalsIgnoreCase(code)
                && total.compareTo(SALE50_MIN_ORDER_AMOUNT) < 0) {
            return false;
        }

        return true;
    }
}
