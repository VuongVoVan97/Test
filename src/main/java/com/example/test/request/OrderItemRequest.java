package com.example.test.request;

// Một dòng sản phẩm trong đơn hàng. Giá KHÔNG nhận từ client, server tự lấy từ Product.
public class OrderItemRequest {

    private Long productId;

    private Integer quantity;

    public OrderItemRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
