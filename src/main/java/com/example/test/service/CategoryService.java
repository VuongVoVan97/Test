package com.example.test.service;

import com.example.test.entity.Category;
import com.example.test.request.CategoryRequest;

import java.util.List;

public interface CategoryService {

    Category getCategoryById(Long id) throws Exception;

    List<Category> getAllCategories() throws Exception;

    Category createCategory(CategoryRequest request) throws Exception;

    Category updateCategory(Long id, CategoryRequest request) throws Exception;

    void deleteCategory(Long id) throws Exception;
}
