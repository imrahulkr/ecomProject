package com.ecommerce.project.service;


import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    ModelMapper modelMapper;


    public CartServiceImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

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
        newCartItem.setProductPrice(product.getPrice());

        // Save CartItem
        cartItemRepository.save(newCartItem);
        product.setQuantity(product.getQuantity());
        productRepository.save(product);
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
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

       CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

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

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
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
        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartRepository.save(cart);
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);
        return "Product : " + cartItem.getProduct().getProductName() + " has been deleted";
    }
    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItems) {
        System.out.println("createOrUpdateCartWithItems : ");
//        String emailId = authUtil.loggedInEmail();
//        Cart existingCart = cartRepository.findCartByEmail(emailId);
//        if(existingCart == null) {
//            existingCart = new Cart();
//            existingCart.setTotalPrice(0);
//            existingCart.setUser(authUtil.loggedInUser());
//            existingCart = cartRepository.save(existingCart);
//        } else {
//            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
//        }
//        double totalPrice = 0.00;
//        for(CartItemDTO cartItemDTO : cartItems) {
////            Long productId = cartItemDTO.getProduct().getProductId();
//            Long productId = cartItemDTO.getProductId();
//            Integer quantity = cartItemDTO.getQuantity();
//
//            Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product ", "productId", productId));
//            product.setQuantity(product.getQuantity() - quantity);
//            totalPrice += product.getSpecialPrice() * quantity;
//            CartItem cartItem = new CartItem();
//        }
//        return "";


        // Get user's email
        String emailId = authUtil.loggedInEmail();

        // Check if an existing cart is available or create a new one
        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        } else {
            // Clear all current items in the existing cart
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }

        double totalPrice = 0.00;

        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            // Find the product by ID
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            // Directly update product stock and total price
            // product.setQuantity(product.getQuantity() - quantity);
            totalPrice += product.getSpecialPrice() * quantity;

            // Create and save cart item
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }

        // Update the cart's total price and save
        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);
        return "Cart created/updated with the new items successfully";
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) return userCart;
        Cart cart = new Cart();
        cart.setUser(authUtil.loggedInUser());
        cart.setTotalPrice(0.0);
        return cartRepository.save(cart);
    }

    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if(carts.size() == 0) throw new APIException("No Carts created Yet !!!");

        List<CartDTO> cartDTOs = carts.stream().map(cart ->{
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> productDTOs = cart.getCartItems().stream().map(cartItem -> {
                        ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                        productDTO.setQuantity(cartItem.getQuantity());
                        return productDTO;
                    }).toList();
                    cartDTO.setProducts(productDTOs);
                    return cartDTO;
        }).collect(Collectors.toList());
        return cartDTOs;
    }
}