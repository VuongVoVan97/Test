package com.example.test.service.impl;

import com.example.test.entity.Category;
import com.example.test.repository.CategoryRepository;
import com.example.test.request.CategoryRequest;
import com.example.test.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category getCategoryById(Long id) throws Exception {
        // TODO: tìm theo id, không có thì ném NotFoundException
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<Category> getAllCategories() throws Exception {
        // TODO: trả về danh sách category
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Category createCategory(CategoryRequest request) throws Exception {
        // TODO: validate (name bắt buộc, không trùng tên) rồi lưu
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Category updateCategory(Long id, CategoryRequest request) throws Exception {
        // TODO: validate, cập nhật các field rồi lưu
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteCategory(Long id) throws Exception {
        // TODO: không cho xóa nếu còn product thuộc category này (hoặc chuyển sang active = false)
        throw new UnsupportedOperationException("TODO");
    }
}
