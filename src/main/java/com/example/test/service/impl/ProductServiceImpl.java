package com.example.test.service.impl;

import com.example.test.entity.Product;
import com.example.test.enums.ProductStatus;
import com.example.test.exception.NotFoundException;
import com.example.test.repository.CategoryRepository;
import com.example.test.repository.ProductRepository;
import com.example.test.request.ProductRequest;
import com.example.test.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product getProductById(Long id) throws Exception {
        // TODO: tìm theo id, không có thì ném NotFoundException
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Product> getAllProducts(Long categoryId) throws Exception {
        // TODO: categoryId null -> lấy tất cả, ngược lại lọc theo category
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Product createProduct(ProductRequest request) throws Exception {
        // TODO: validate (sku, name, price >= 0, stock >= 0, category tồn tại, sku không trùng)
        //       set createdAt/updatedAt, status mặc định ACTIVE
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Product updateProduct(Long id, ProductRequest request) throws Exception {
        // TODO: validate, cập nhật field, set updatedAt
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteProduct(Long id) throws Exception {
        // TODO: nên chuyển status = INACTIVE thay vì xóa cứng (vì OrderItem còn tham chiếu productId)
        throw new UnsupportedOperationException("TODO");
    }


    // [REVIEW FIX] rollbackFor = Exception.class: NotFoundException là checked exception nên
    // @Transactional mặc định không rollback.
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long productId, int quantity) throws Exception {
        // 1. Quantity phải lớn hơn 0
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        // 2. Tìm product, không tồn tại thì ném lỗi
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new NotFoundException("Product not found: " + productId));

        // 3. Product phải ACTIVE
        // [REVIEW FIX] Tách riêng OUT_OF_STOCK để thông báo đúng, trước đây báo "not active" gây hiểu nhầm
        if (ProductStatus.OUT_OF_STOCK.equals(product.getStatus())) {
            throw new IllegalStateException("Insufficient stock");
        }
        if (!ProductStatus.ACTIVE.equals(product.getStatus())) {
            throw new IllegalStateException("Product is not active");
        }

        // 4. Stock phải đủ
        if (quantity > product.getStock()) {
            throw new IllegalStateException("Insufficient stock");
        }

        // 5. Trừ stock
        int remainingStock = product.getStock() - quantity;
        product.setStock(remainingStock);

        // 6. Nếu stock về 0 thì chuyển trạng thái
        if (remainingStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        // [REVIEW FIX] Cập nhật updatedAt khi đổi tồn kho
        product.setUpdatedAt(LocalDateTime.now());

        // Không cần gọi save() nếu product là entity đang được
        // JPA quản lý trong transaction hiện tại.
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long productId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than 0"
            );
        }
        // Cộng lại stock, nếu đang OUT_OF_STOCK thì chuyển về ACTIVE
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new NotFoundException("Product not found: " + productId));

        // [REVIEW FIX] Bỏ kiểm tra status: sản phẩm INACTIVE (đã "xóa mềm") vẫn phải được hoàn kho,
        // nếu không thì không hủy được các đơn cũ có sản phẩm đó.
        int totalStock = Math.addExact(product.getStock(), quantity);
        product.setStock(totalStock);

        // Chỉ OUT_OF_STOCK mới quay lại ACTIVE, INACTIVE giữ nguyên
        if (ProductStatus.OUT_OF_STOCK.equals(product.getStatus())) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        // [REVIEW FIX] Cập nhật updatedAt khi đổi tồn kho
        product.setUpdatedAt(LocalDateTime.now());
    }
}
