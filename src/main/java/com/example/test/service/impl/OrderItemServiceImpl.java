package com.example.test.service.impl;

import com.example.test.entity.OrderItem;
import com.example.test.entity.Product;
import com.example.test.exception.NotFoundException;
import com.example.test.repository.OrderItemRepository;
import com.example.test.repository.ProductRepository;
import com.example.test.request.OrderItemRequest;
import com.example.test.service.OrderItemService;
import com.example.test.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository, ProductRepository productRepository, ProductService productService) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Override
    public List<OrderItem> getItemsByOrderId(Long orderId) throws Exception {

        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }

        return orderItemRepository.findByOrderId(orderId);
    }

    // [REVIEW FIX] rollbackFor = Exception.class: NotFoundException là checked exception nên
    // @Transactional mặc định KHÔNG rollback. Trước đây nếu sản phẩm thứ 3 lỗi thì kho của
    // sản phẩm 1, 2 vẫn bị trừ dù không có OrderItem nào được lưu.
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderItem> createItems(Long orderId, List<OrderItemRequest> requests) throws Exception {
        // [REVIEW FIX] Validate đầu vào: trước đây orderId/requests null hoặc rỗng gây NPE hoặc tạo đơn rỗng
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest request : requests) {

            // [REVIEW FIX] productId null làm findById(null) ném lỗi của Spring Data (trả 500)
            if (request.getProductId() == null) {
                throw new IllegalArgumentException("Product ID must not be null");
            }

            // 1. Validate quantity > 0
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than 0"
                );
            }

            // 2. Lấy Product và kiểm tra tồn tại
            // [REVIEW FIX] Đổi new Exception(...) sang NotFoundException để client nhận 404 thay vì 500
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() ->
                            new NotFoundException("Product not found: " + request.getProductId())
                    );

            // 3. Tạo OrderItem và copy snapshot từ Product
            OrderItem item = new OrderItem();

            // [REVIEW FIX] Thiếu setOrderId: cột order_id là nullable = false nên saveAll sẽ lỗi
            item.setOrderId(orderId);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(request.getQuantity());

            // 4. Tính subtotal = unitPrice * quantity
            BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            item.setSubtotal(subtotal);

            // 5. Giảm tồn kho
            productService.decreaseStock(
                    product.getId(),
                    item.getQuantity()
            );

            // 6. Thêm vào danh sách
            items.add(item);
        }

        // 7. Lưu tất cả OrderItem
        return orderItemRepository.saveAll(items);
    }

    // [REVIEW FIX] Trước đây hàm này tính lại và ghi đè subtotal của từng item (side effect).
    // Giờ chỉ cộng subtotal đã có, đồng thời xử lý items null/rỗng.
    @Override
    public BigDecimal calculateTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
