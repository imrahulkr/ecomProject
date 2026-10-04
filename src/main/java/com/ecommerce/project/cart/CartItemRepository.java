package com.ecommerce.project.cart;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT COALESCE(SUM(ci.productPriceMinorUnits * ci.quantity), 0) FROM CartItem ci WHERE ci.cart.cartId = ?1")
    long calculateTotalPriceMinorUnits(Long cartId);

    // Keeps open carts in step with a product's current price, so a buyer can't hold an old
    // (lower) price in their cart and check out with it.
    @Modifying
    @Query("UPDATE CartItem ci SET ci.productPriceMinorUnits = :priceMinorUnits, ci.discount = :discount WHERE ci.product.productId = :productId")
    int repriceForProduct(Long productId, long priceMinorUnits, double discount);

    @Query("SELECT DISTINCT ci.cart.cartId FROM CartItem ci WHERE ci.product.productId = :productId")
    List<Long> findCartIdsByProductId(Long productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.product.productId = :productId")
    int deleteByProductId(Long productId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = ?1 AND ci.product.id = ?2")
    CartItem findCartItemByProductIdAndCartId(Long cartId, Long productId);
    @Modifying
    @Query("DELETE FROM CartItem  ci where ci.cart.id = ?1 AND ci.product.id = ?2")
    void deleteCartItemByProductIdAndCartId(Long cartId, Long productId);
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = ?1")
    void deleteAllByCartId(Long cartId);
}