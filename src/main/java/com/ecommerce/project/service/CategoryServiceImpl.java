package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    //private List<Category> categories = new ArrayList<>();
    //private Long nextId = 1L;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String  sortBy, String sortOrder) {
        // Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(Sort.Direction.ASC, "categoryId") : Sort.by(Sort.Direction.DESC, "categoryId");
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        //Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);
        //List<Category> categories = categoryRepository.findAll();
        List<Category> categories = categoryPage.getContent();
       if(categories.isEmpty()) throw new APIException("No categories created till now");
        //return categoryRepository.findAll();
        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();
        //return categories;
        //return new CategoryResponse(categoryDTOS);
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(pageDetails.getPageNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setTotalElement(categoryPage.getTotalElements());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category categoryFromDB = categoryRepository.findByCategoryName(category.getCategoryName());
        if(categoryFromDB != null) throw new APIException("Category with this name : " + category.getCategoryName() + " already exists !!!");
        //category.setCategoryId((long) categories.size() + 1);

//        category.setCategoryId(nextId++);
//        categories.add(category);
        Category savedCategory = categoryRepository.save(category);
        CategoryDTO savedCategoryDTO = modelMapper.map(savedCategory,  CategoryDTO.class);
        return savedCategoryDTO;
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
//        Category category = categories.stream().filter(
//                        c -> c.getCategoryId().equals(categoryId))
//                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Category with id " + categoryId + " not found" ));
////        if(category != null){
//            categories.remove(category);
//            return "Removed category with id " + categoryId;
////        }
//        return "Category with id " + categoryId + " not found";


        if(!categoryRepository.existsById(categoryId)){
            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
            throw new ResourceNotFoundException("Category", "categoryId", categoryId);

        }
        Category deletedCategory = categoryRepository.findById(categoryId).get();
        categoryRepository.deleteById(categoryId);
        CategoryDTO deletedCategoryDTO = modelMapper.map(deletedCategory,  CategoryDTO.class);
        return deletedCategoryDTO;
    }

    @Override
    public CategoryDTO updateCategory( CategoryDTO categoryDTO, Long categoryId) {
        Category category = modelMapper.map(categoryDTO, Category.class);
//        Category curCategory = categories.stream().filter(
//                c -> c.getCategoryId().equals(categoryId)
//        ).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Category with id " + categoryId + " not found"));
//        curCategory.setCategoryName(category.getCategoryName());
//        return "Category Successfully Updated !!!! ";
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
//        Category savedCategory = optionalCategory.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        Category savedCategory = optionalCategory.orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));

        savedCategory.setCategoryName(category.getCategoryName());
        Category updatedCategory = categoryRepository.save(savedCategory);
        CategoryDTO updatedCategoryDTO = modelMapper.map(updatedCategory,  CategoryDTO.class);
        return updatedCategoryDTO;
    }
}
