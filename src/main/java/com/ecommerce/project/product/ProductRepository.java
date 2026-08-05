package com.ecommerce.project.product;

import com.ecommerce.project.category.Category;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.ecommerce.project.product.ProductRepository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByCategory(Category category, Pageable pageDetails);

    Page<Product> findByProductNameLikeIgnoreCase(String productName, Pageable pageDetails);

    Product findByProductNameIgnoreCase(String productName);

    Page<Product> findByUser(User seller, Pageable pageDetails);
}
