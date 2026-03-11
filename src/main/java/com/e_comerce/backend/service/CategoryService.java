package com.e_comerce.backend.service;

import java.util.List;

import com.e_comerce.backend.model.Category;
import com.e_comerce.backend.payload.dto.CategoryDTO;
import com.e_comerce.backend.payload.response.CategoryResponse;

import jakarta.persistence.criteria.CriteriaBuilder.In;

public interface CategoryService {
    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    CategoryDTO addCategory(CategoryDTO categoryDTO);
    CategoryDTO deleteCategory(Long categoryId);
    CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);
}
