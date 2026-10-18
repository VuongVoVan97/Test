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


    @Override
    @Transactional
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

        // Không cần gọi save() nếu product là entity đang được
        // JPA quản lý trong transaction hiện tại.
    }


    @Override
    @Transactional
    public void increaseStock(Long productId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than 0"
            );
        }
        // TODO: cộng lại stock, nếu đang OUT_OF_STOCK thì chuyển về ACTIVE
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new NotFoundException("Product not found: " + productId));

        if (!ProductStatus.ACTIVE.equals(product.getStatus())
                && !ProductStatus.OUT_OF_STOCK.equals(product.getStatus())) {
            throw new IllegalStateException(
                    "Cannot increase stock for this product status"
            );
        }


        int totalStock = Math.addExact(product.getStock(), quantity);
        product.setStock(totalStock);

        if (ProductStatus.OUT_OF_STOCK.equals(product.getStatus())) {
            product.setStatus(ProductStatus.ACTIVE);
        }

    }
}
