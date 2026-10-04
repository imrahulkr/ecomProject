package com.ecommerce.project.category;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.category.dto.CategoryDTO;
import com.ecommerce.project.category.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String  sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        List<Category> categories = categoryPage.getContent();
        List<CategoryDTO> categoryDTOS = categories.isEmpty() ? Collections.emptyList() : categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();
        CategoryResponse categoryResponse = new CategoryResponse(
                categoryDTOS,
                pageDetails.getPageNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages(),
                categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category categoryFromDB = categoryRepository.findByCategoryName(category.getCategoryName());
        if(categoryFromDB != null) throw new APIException("Category with this name : " + category.getCategoryName() + " already exists !!!");
        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory,  CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        if(!categoryRepository.existsById(categoryId)){
            throw new ResourceNotFoundException("Category", "categoryId", categoryId);
        }
        Category deletedCategory = categoryRepository.findById(categoryId).get();
        categoryRepository.deleteById(categoryId);
        return modelMapper.map(deletedCategory,  CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory( CategoryDTO categoryDTO, Long categoryId) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
        Category savedCategory = optionalCategory.orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));

        savedCategory.setCategoryName(category.getCategoryName());
        Category updatedCategory = categoryRepository.save(savedCategory);
        return modelMapper.map(updatedCategory,  CategoryDTO.class);
    }
}
