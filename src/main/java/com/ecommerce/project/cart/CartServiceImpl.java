package com.ecommerce.project.cart;


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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final AuthUtil authUtil;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ModelMapper modelMapper;

    @Value("${app.currency}")
    private String currency;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        // Find Existing Cart or Create new for the logged in user
        Cart cart = createCart();
        // Retrive priduct details (using productId);
        Product product = productRepository.findById(productId)
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
        newCartItem.setProductPriceMinorUnits(product.getPriceMinorUnits());
        newCartItem.setCurrency(product.getCurrency());

        // Save CartItem
        cartItemRepository.save(newCartItem);
        cart.setTotalPriceMinorUnits(cart.getTotalPriceMinorUnits() + (product.getSpecialPriceMinorUnits() * quantity));
        cartRepository.save(cart);

        // return updated cart Info
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
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
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if(cart == null) {
            throw new ResourceNotFoundException("Cart ", "cartId", cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
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
        String emailId = authUtil.loggedInEmail();
        Long cartId = cartRepository.findCartByEmail(emailId).getCartId();
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart ", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ", "productId", productId));

        if (product.getQuantity() < quantity) {
            throw new APIException("Product " + product.getProductName() + " is either not available or has less quantity");
        }

        CartItem currCartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if(currCartItem == null) {
            throw new APIException("Product " + product.getProductName() + " does not exist in Cart !!!");
        }
        currCartItem.setQuantity(currCartItem.getQuantity() + quantity);

        // Save CartItem
        CartItem updatedCartItem =  cartItemRepository.save(currCartItem);
        if(updatedCartItem.getQuantity() == 0) cartItemRepository.deleteById(updatedCartItem.getCartItemId());

        cart.setTotalPriceMinorUnits(cart.getTotalPriceMinorUnits() + (product.getSpecialPriceMinorUnits() * quantity));
        cartRepository.save(cart);

        // return updated cart Info
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
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
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart ", "cartId", cartId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if(cartItem == null) {
            throw new ResourceNotFoundException("Product ", "productId", productId);
        }
        cart.setTotalPriceMinorUnits(cart.getTotalPriceMinorUnits() - (cartItem.getProductPriceMinorUnits() * cartItem.getQuantity()));
        cartRepository.save(cart);
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);
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

        long totalPriceMinorUnits = 0L;

        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.productId();
            Integer quantity = cartItemDTO.quantity();

            // Find the product by ID
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            totalPriceMinorUnits += product.getSpecialPriceMinorUnits() * quantity;

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

        // Update the cart's total price and save
        existingCart.setTotalPriceMinorUnits(totalPriceMinorUnits);
        cartRepository.save(existingCart);
        return "Cart created/updated with the new items successfully";
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

    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if (carts.isEmpty()) return Collections.emptyList();

        return carts.stream().map(cart ->{
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> productDTOs = cart.getCartItems().stream().map(cartItem -> {
                        ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                        productDTO.setQuantity(cartItem.getQuantity());
                        return productDTO;
                    }).toList();
                    cartDTO.setProducts(productDTOs);
                    return cartDTO;
        }).collect(Collectors.toList());
    }
}
