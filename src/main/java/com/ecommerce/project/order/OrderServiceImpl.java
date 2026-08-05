package com.ecommerce.project.order;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.cart.CartService;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ecommerce.project.address.Address;
import com.ecommerce.project.address.AddressRepository;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartItem;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.order.dto.OrderDTO;
import com.ecommerce.project.order.dto.OrderItemDTO;
import com.ecommerce.project.order.dto.OrderResponse;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.auth.User;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        // Getting User Cart
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("cart", "email", emailId);
        }
        Address address = addressRepository.findById(addressId).orElseThrow(() ->
                new ResourceNotFoundException("address", "addressId", addressId));


        // Creating new order with payment info
        Order order = new Order();
        order.setEmail(emailId);
        order.setAddress(address);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted !");

        Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        payment.setOrder(order);
        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);
        Order savedOrder = orderRepository.save(order);
        // Get items from the cart into the order item

        List<CartItem> cartItems = cart.getCartItems();
        if(cartItems.isEmpty()){
            throw new APIException("Cart is empty");
        }
        List<OrderItem> orderItems = new ArrayList<>();
        for(CartItem cartItem : cartItems){
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }
        orderItems =  orderItemRepository.saveAll(orderItems);

        // Update product stock
        cart.getCartItems().forEach(item -> {
            Integer orderQuantity = item.getQuantity();
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() - orderQuantity);
            productRepository.save(product);
            // clear the cart
            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
        });

        //send back the order summary
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        orderItems.forEach(orderItem ->
                orderDTO.getOrderItems().add(modelMapper.map(orderItem, OrderItemDTO.class)));

        orderDTO.setAddressId(addressId);
        orderDTO.setOrderDate(LocalDate.now());
        return orderDTO;
    }

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Order> orderpage = orderRepository.findAll(pageDetails);
        List<Order> orders = orderpage.getContent();
        List<OrderDTO> orderDTOS = orders.isEmpty() ? Collections.emptyList() : orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setContent(orderDTOS);
        orderResponse.setPageNumber(orderpage.getNumber());
        orderResponse.setPageSize(orderpage.getSize());
        orderResponse.setTotalPages(orderpage.getTotalPages());
        orderResponse.setTotalElement(orderpage.getTotalElements());
        orderResponse.setLastPage(orderpage.isLast());
        return orderResponse;
    }

    @Override
    public OrderDTO updateOrder(String emailId, Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
        order.setEmail(emailId);
        order.setOrderStatus(status);
        orderRepository.save(order);

        return modelMapper.map(order, OrderDTO.class);
    }

    @Override
    public OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        User seller = authUtil.loggedInUser();
        Page<Order> orderpage = orderRepository.findAll(pageDetails);
        List<Order> orders = orderpage.getContent().stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(orderItem -> {
                            var product = orderItem.getProduct();
                            if(product == null || product.getUser() == null) return false;
                            return product.getUser().
                                    getUserId().equals(seller.getUserId());
                        })).toList();

        List<OrderDTO> orderDTOS = orders.isEmpty() ? Collections.emptyList() : orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setContent(orderDTOS);
        orderResponse.setPageNumber(orderpage.getNumber());
        orderResponse.setPageSize(orderpage.getSize());
        orderResponse.setTotalPages(orderpage.getTotalPages());
        orderResponse.setTotalElement(orderpage.getTotalElements());
        orderResponse.setLastPage(orderpage.isLast());
        return orderResponse;
    }


}
