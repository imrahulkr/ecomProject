package com.ecommerce.project.cart;

import com.ecommerce.project.checkout.ShippingCalculator;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.cart.dto.CartDTO;
import com.ecommerce.project.cart.dto.CartItemDTO;
import com.ecommerce.project.product.dto.ProductDTO;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final AuthUtil authUtil;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ModelMapper modelMapper;
    private final ShippingCalculator shippingCalculator;

    @Value("${app.currency}")
    private String currency;

    @Override
    @Transactional
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new APIException("Cart quantity must be greater than zero");
        }
        // Find Existing Cart or Create new for the logged in user
        Cart cart = createCart();
        // Retrive priduct details (using productId);
        Product product = productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ", "productId", productId));
        // Perform Validations (like stock exists or not)
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Product " + product.getProductName() + " is either not available or has less quantity");
        }
        // Create CartItem
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setQuantity(quantity);
        newCartItem.setCart(cart);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPriceMinorUnits(product.getSpecialPriceMinorUnits());
        newCartItem.setCurrency(product.getCurrency());

        // Save CartItem
        cartItemRepository.save(newCartItem);
        refreshCartTotal(cart);
        cartRepository.save(cart);

        // return updated cart Info
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        applyTotals(cartDTO, cart);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Override
    @Transactional
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if(cart == null) {
            throw new ResourceNotFoundException("Cart ", "cartId", cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        applyTotals(cartDTO, cart);
        List<ProductDTO> productDTOs = cart.getCartItems().stream().map(product -> {
            ProductDTO productDTO = modelMapper.map(product.getProduct(), ProductDTO.class);
            productDTO.setQuantity(product.getQuantity());
            return productDTO;
        }).toList();
        cartDTO.setProducts(productDTOs);
        return cartDTO;
    }

    @Override
    @Transactional
    public CartDTO updateCartProduct(Long productId, Integer quantity) {
        if (quantity == null || (quantity != 1 && quantity != -1)) {
            throw new APIException("Cart quantity changes must be exactly one unit");
        }
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        if (userCart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }
        Long cartId = userCart.getCartId();
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart ", "cartId", cartId));

        Product product = productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ", "productId", productId));

        CartItem currCartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (currCartItem == null) {
            throw new APIException("Product " + product.getProductName() + " does not exist in Cart !!!");
        }

        int updatedQuantity = currCartItem.getQuantity() + quantity;
        if (updatedQuantity < 0) {
            throw new APIException("Cart quantity cannot be negative");
        }
        if (updatedQuantity > product.getQuantity()) {
            throw new APIException("Product " + product.getProductName() + " is either not available or has less quantity");
        }
        currCartItem.setQuantity(currCartItem.getQuantity() + quantity);

        // Save CartItem
        CartItem updatedCartItem =  cartItemRepository.save(currCartItem);
        if(updatedCartItem.getQuantity() == 0) cartItemRepository.deleteById(updatedCartItem.getCartItemId());

        refreshCartTotal(cart);
        cartRepository.save(cart);

        // return updated cart Info
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        applyTotals(cartDTO, cart);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Override
    @Transactional
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(authUtil.loggedInEmail(), cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart ", "cartId", cartId);
        }
        return removeProductFromCart(cart, productId);
    }

    @Override
    @Transactional
    public String removeProductFromCart(Cart cart, Long productId) {
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if(cartItem == null) {
            throw new ResourceNotFoundException("Product ", "productId", productId);
        }
        cartItemRepository.deleteCartItemByProductIdAndCartId(cart.getCartId(), productId);
        refreshCartTotal(cart);
        cartRepository.save(cart);
        return "Product : " + cartItem.getProduct().getProductName() + " has been deleted";
    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItems) {
        // Get user's email
        String emailId = authUtil.loggedInEmail();

        // Check if an existing cart is available or create a new one
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPriceMinorUnits(0L);
            existingCart.setCurrency(currency);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        } else {
            // Clear all current items in the existing cart
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }

        Set<Long> requestedProductIds = new HashSet<>();
        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.productId();
            Integer quantity = cartItemDTO.quantity();

            if (!requestedProductIds.add(productId)) {
                throw new APIException("A product may only appear once in the cart request");
            }

            // Find the product by ID
            Product product = productRepository.findByProductIdAndActiveTrue(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            if (quantity == null || quantity < 1 || quantity > product.getQuantity()) {
                throw new APIException("Product " + product.getProductName() + " is either not available or has less quantity");
            }

            // Create and save cart item
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPriceMinorUnits(product.getSpecialPriceMinorUnits());
            cartItem.setDiscount(product.getDiscount());
            cartItem.setCurrency(product.getCurrency());
            cartItemRepository.save(cartItem);
        }

        // Update the cart's total from the persisted line-item prices.
        refreshCartTotal(existingCart);
        cartRepository.save(existingCart);
        return "Cart created/updated with the new items successfully";
    }

    @Override
    @Transactional
    public String clearCart() {
        Cart cart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", authUtil.loggedInEmail());
        }
        cartItemRepository.deleteAllByCartId(cart.getCartId());
        cart.setTotalPriceMinorUnits(0L);
        cart.setAppliedCouponCode(null);
        cart.setDiscountMinorUnits(0L);
        cartRepository.save(cart);
        return "Cart has been cleared";
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) return userCart;
        Cart cart = new Cart();
        cart.setUser(authUtil.loggedInUser());
        cart.setTotalPriceMinorUnits(0L);
        cart.setCurrency(currency);
        return cartRepository.save(cart);
    }

    private void refreshCartTotal(Cart cart) {
        cart.setTotalPriceMinorUnits(cartItemRepository.calculateTotalPriceMinorUnits(cart.getCartId()));
    }

    @Transactional
    public List<CartDTO> getAllCarts() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart == null) return Collections.emptyList();

        CartDTO cartDTO = modelMapper.map(userCart, CartDTO.class);
        applyTotals(cartDTO, userCart);

        List<ProductDTO> productDTOs = userCart.getCartItems().stream().map(cartItem -> {
            ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
            productDTO.setQuantity(cartItem.getQuantity());
            return productDTO;
        }).toList();
        cartDTO.setProducts(productDTOs);
        return List.of(cartDTO);
    }

    // Shipping and the payable total shown in the cart - exactly what checkout will charge
    // (CheckoutTransactionExecutor uses the same ShippingCalculator).
    private void applyTotals(CartDTO cartDTO, Cart cart) {
        long afterDiscount = cart.getTotalPriceMinorUnits() - cart.getDiscountMinorUnits();
        long shipping = shippingCalculator.shippingFor(afterDiscount, !cart.getCartItems().isEmpty());
        cartDTO.setShippingMinorUnits(shipping);
        cartDTO.setFinalPriceMinorUnits(afterDiscount + shipping);
    }
}
