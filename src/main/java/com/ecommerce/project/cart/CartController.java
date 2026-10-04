package com.ecommerce.project.cart;

import com.ecommerce.project.exceptions.ResourceNotFoundException;

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


    // First path is canonical; the second is kept as a deprecated alias for existing clients.
    @PostMapping({"/carts", "/cart/create"})
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
        return new ResponseEntity<List<CartDTO>>(cartDTOs, HttpStatus.OK);
    }

    @GetMapping("/carts/users/cart")
    public ResponseEntity<CartDTO> getCartById(){
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);
        // 404, not 400: "no cart yet" is a normal state for a new user, not a bad request.
        if(cart == null) throw new ResourceNotFoundException("cart", "email", emailId);
        Long cartId = cart.getCartId();
        CartDTO cartDTO = cartService.getCart(emailId, cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    // First path is canonical; the second is kept as a deprecated alias for existing clients.
    @PutMapping({"/carts/products/{productId}/quantity/{operation}", "/cart/products/{productId}/quantity/{operation}"})
    public ResponseEntity<CartDTO> updateProductQuantity(@PathVariable Long productId, @PathVariable String operation){
        if (!operation.equalsIgnoreCase("increment") && !operation.equalsIgnoreCase("decrement")
                && !operation.equalsIgnoreCase("delete")) {
            throw new APIException("Unsupported cart quantity operation: " + operation);
        }
        CartDTO cartDTO = cartService.updateCartProduct(productId,
                operation.equalsIgnoreCase("increment") ? 1 : -1);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    // First path is canonical; the second is kept as a deprecated alias for existing clients.
    @DeleteMapping({"/carts/{cartId}/products/{productId}", "/carts/{cartId}/product/{productId}"})
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId, @PathVariable Long productId){
        String status = cartService.deleteProductFromCart(cartId, productId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @DeleteMapping("/carts/users/cart")
    public ResponseEntity<String> clearCart(){
        String status = cartService.clearCart();
        return new ResponseEntity<>(status, HttpStatus.OK);
    }
}
