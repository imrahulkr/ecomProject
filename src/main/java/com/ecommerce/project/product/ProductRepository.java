package com.ecommerce.project.product;

import com.ecommerce.project.category.Category;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.auth.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.ecommerce.project.product.ProductRepository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByCategory(Category category, Pageable pageDetails);

    Page<Product> findByProductNameLikeIgnoreCase(String productName, Pageable pageDetails);

    Product findByProductNameIgnoreCase(String productName);

    Page<Product> findByUser(User seller, Pageable pageDetails);

    // Ownership check happens in the WHERE clause, not after a plain findById - a seller can't
    // even see another seller's product exists (404, not a fetch-then-403 branch).
    Optional<Product> findByProductIdAndUser_UserId(Long productId, Long userId);

    // Single conditional UPDATE, not read-then-write: the WHERE clause guards availability so
    // concurrent reservations for the same product can never both succeed against stock that
    // only exists once. affected-rows == 0 means someone else already claimed the remaining stock.
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :quantity WHERE p.productId = :productId AND p.quantity >= :quantity")
    int decrementStockIfAvailable(Long productId, Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :quantity WHERE p.productId = :productId")
    int incrementStock(Long productId, Integer quantity);
}
