package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api") // class level to represent every mapping starts with "/api"
public class CategoryController {
    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    //@GetMapping("/api/public/categories")
    //@RequestMapping(value="/api/public/categories", method = RequestMethod.GET)
    @RequestMapping(value="/public/categories", method = RequestMethod.GET)
    public ResponseEntity<CategoryResponse> getAllCategories(
            @RequestParam(name="pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name="pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORY_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ) {
//        List<Category> categories = categoryService.getAllCategories();
//        return new ResponseEntity<>(categories, HttpStatus.OK);
        CategoryResponse categoryResponse = categoryService.getAllCategories(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }
    //@PostMapping("/api/public/categories")
    @PostMapping("/public/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO savedCategoryDTO  = categoryService.createCategory(categoryDTO);
        //return new ResponseEntity<>("Category created successfully",  HttpStatus.CREATED);
        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.CREATED);
    }
    //@DeleteMapping("/api/admin/categories/{categoryId}")
    //@RequestMapping(value = "/api/admin/categories/{categoryId}", method=RequestMethod.DELETE)
    @RequestMapping(value = "/admin/categories/{categoryId}", method=RequestMethod.DELETE)
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryId) {
//        try {
//            String status = categoryService.deleteCategory(categoryId);
//            //return new ResponseEntity<>(status, HttpStatus.OK); // ===== Most Common
//            //return ResponseEntity.status(HttpStatus.OK).body(status);
//            return ResponseEntity.ok(status);  // ====  All of these three is fine
//        } catch (ResponseStatusException e){
//            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
//        }
        CategoryDTO categoryDTO = categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(categoryDTO, HttpStatus.OK);
    }
    //@PutMapping("/api/public/categories/{categoryId}")
    @PutMapping("/public/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO, @PathVariable Long categoryId) {
//        try {
            CategoryDTO updateCategoryDTO = categoryService.updateCategory(categoryDTO, categoryId);
            //return new ResponseEntity<>("Category Successfully Updated !!!! ",  HttpStatus.OK);
            return new ResponseEntity<>(updateCategoryDTO, HttpStatus.OK);
//        } catch (ResponseStatusException e) {
//            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
//        }
    }
}
