package com.example.test.service.impl;

import com.example.test.entity.Product;
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
        // TODO: quantity > 0; product phải ACTIVE; stock không đủ -> ném lỗi;
        //       trừ stock, nếu về 0 thì status = OUT_OF_STOCK.
        //       Chú ý @Version: xử lý ObjectOptimisticLockingFailureException khi 2 đơn tranh nhau.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    @Transactional
    public void increaseStock(Long productId, int quantity) throws Exception {
        // TODO: cộng lại stock, nếu đang OUT_OF_STOCK thì chuyển về ACTIVE
        throw new UnsupportedOperationException("TODO");
    }
}
