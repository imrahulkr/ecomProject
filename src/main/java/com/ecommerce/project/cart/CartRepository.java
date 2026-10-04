package com.ecommerce.project.cart;

import com.ecommerce.project.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // Recomputes the stored total of every cart that holds the product (after repricing).
    @Modifying
    @Query("UPDATE Cart c SET c.totalPriceMinorUnits = (SELECT COALESCE(SUM(ci.productPriceMinorUnits * ci.quantity), 0) "
            + "FROM CartItem ci WHERE ci.cart = c) "
            + "WHERE c.cartId IN (SELECT ci2.cart.cartId FROM CartItem ci2 WHERE ci2.product.productId = :productId)")
    int recalculateTotalsForProduct(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Cart c SET c.totalPriceMinorUnits = (SELECT COALESCE(SUM(ci.productPriceMinorUnits * ci.quantity), 0) "
            + "FROM CartItem ci WHERE ci.cart = c) WHERE c.cartId IN :cartIds")
    int recalculateTotals(@Param("cartIds") List<Long> cartIds);

    @Query("SELECT c FROM Cart c WHERE c.user.email = ?1")
    Cart findCartByEmail(String email);
    @Query("SELECT c FROM Cart c WHERE c.user.email = ?1 AND c.cartId = ?2")
    Cart findCartByEmailAndCartId(String emailId, Long cartId);
    @Query("SELECT c FROM Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product p WHERE p.id = ?1")
    List<Cart> findCartsByProductId(Long productId);
}
