package com.e_comerce.backend.payload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    
    private Long categoryId;

    @NotBlank(message = "Category name is required")
    @Size(min = 3, message = "Category name must be between 3 characters long")
    private String categoryName;
}
