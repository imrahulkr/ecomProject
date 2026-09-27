package com.ecommerce.project.cart;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.cart.dto.CartDTO;
import com.ecommerce.project.cart.dto.CartItemDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class CartController {
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final AuthUtil authUtil;


    @PostMapping("/cart/create")
    public ResponseEntity<String> createOrUpdateCart(@Valid @RequestBody List<@Valid CartItemDTO> cartItems){
        String response = cartService.createOrUpdateCartWithItems(cartItems);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long productId,
                                                    @Positive @PathVariable Integer quantity){
        CartDTO cartDTO = cartService.addProductToCart(productId, quantity);
        return new ResponseEntity<>(cartDTO, HttpStatus.CREATED);
    }

    @GetMapping("/carts")
    public ResponseEntity<List<CartDTO>> getCarts(){
        List<CartDTO> cartDTOs = cartService.getAllCarts();
        return new ResponseEntity<List<CartDTO>>(cartDTOs, HttpStatus.FOUND);
    }

    @GetMapping("/carts/users/cart")
    public ResponseEntity<CartDTO> getCartById(){
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);
        if(cart == null) throw new APIException("Cart is not present for this user, kindly add at least one product in cart");
        Long cartId = cart.getCartId();
        CartDTO cartDTO = cartService.getCart(emailId, cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PutMapping("/cart/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateProductQuantity(@PathVariable Long productId, @PathVariable String operation){
        if (!operation.equalsIgnoreCase("increment") && !operation.equalsIgnoreCase("decrement")
                && !operation.equalsIgnoreCase("delete")) {
            throw new APIException("Unsupported cart quantity operation: " + operation);
        }
        CartDTO cartDTO = cartService.updateCartProduct(productId,
                operation.equalsIgnoreCase("increment") ? 1 : -1);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @DeleteMapping("/carts/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId, @PathVariable Long productId){
        if (cartRepository.findCartByEmailAndCartId(authUtil.loggedInEmail(), cartId) == null) {
            throw new APIException("Cart does not belong to the authenticated user");
        }
        String status = cartService.deleteProductFromCart(cartId, productId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }
}
