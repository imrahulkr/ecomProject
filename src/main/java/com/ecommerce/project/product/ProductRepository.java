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
    // Every catalog-facing lookup filters on active - see Product.active.
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageDetails);

    Page<Product> findByProductNameLikeIgnoreCaseAndActiveTrue(String productName, Pageable pageDetails);

    Product findByProductNameIgnoreCaseAndActiveTrue(String productName);

    Optional<Product> findByProductIdAndActiveTrue(Long productId);

    Page<Product> findByActiveTrue(Pageable pageDetails);

    Page<Product> findByUserAndActiveTrue(User seller, Pageable pageDetails);

    // Ownership check happens in the WHERE clause, not after a plain findById - a seller can't
    // even see another seller's product exists (404, not a fetch-then-403 branch).
    Optional<Product> findByProductIdAndUser_UserIdAndActiveTrue(Long productId, Long userId);

    // Single conditional UPDATE, not read-then-write: the WHERE clause guards availability so
    // concurrent reservations for the same product can never both succeed against stock that
    // only exists once. affected-rows == 0 means someone else already claimed the remaining stock.
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :quantity WHERE p.productId = :productId AND p.quantity >= :quantity")
    int decrementStockIfAvailable(Long productId, Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :quantity WHERE p.productId = :productId")
    int incrementStock(Long productId, Integer quantity);

    // Seller/admin stock edits: applies the change the editor made (delta), not the absolute
    // number they typed, so stock reserved or released while the form was open isn't lost.
    // affected-rows == 0 means the result would go negative.
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :delta WHERE p.productId = :productId AND p.quantity + :delta >= 0")
    int adjustStock(Long productId, int delta);

    @Query("SELECT p.quantity FROM Product p WHERE p.productId = :productId")
    Integer findQuantityById(Long productId);
}
