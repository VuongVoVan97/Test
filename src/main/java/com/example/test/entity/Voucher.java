package com.example.test.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "description")
    private String description;

    // PERCENT hoặc FIXED_AMOUNT
    @Column(name = "discount_type", nullable = false)
    private String discountType;

    // Phần trăm giảm hoặc số tiền giảm cố định
    @Column(name = "discount_value", nullable = false,
            precision = 19, scale = 2)
    private BigDecimal discountValue;

    // Số tiền giảm tối đa
    @Column(name = "max_discount_amount", precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount;

    // Giá trị đơn hàng tối thiểu
    @Column(name = "min_order_amount", nullable = false,
            precision = 19, scale = 2)
    private BigDecimal minOrderAmount;

    // Tổng số lượt được sử dụng
    @Column(name = "usage_limit")
    private Integer usageLimit;

    // Tổng số lượt đã sử dụng
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Voucher() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Voucher(Long id, String code, String description, String discountType, BigDecimal discountValue, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount, Integer usageLimit, Integer usedCount, LocalDateTime startAt, LocalDateTime endAt, Boolean active) {
        this.id = id;
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
    }

    // Generate Getter và Setter trong IntelliJ
}
