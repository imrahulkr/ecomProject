package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.*;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    AuthUtil authUtil;

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

        List<CartItem> cartItems = cart.getCartItems();;
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

//        List<OrderItemDTO> orderItemDTOs = orderItems.stream().map(orderItem ->
//                modelMapper.map(orderItem, OrderItemDTO.class)).toList();
//        orderDTO.setOrderItems(orderItemDTOs);

        orderItems.forEach(orderItem ->
                orderDTO.getOrderItems().add(modelMapper.map(orderItem, OrderItemDTO.class)));

        orderDTO.setAddressId(addressId);
        orderDTO.setOrderDate(LocalDate.now());
        return orderDTO;
    }

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        // Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(Sort.Direction.ASC, "categoryId") : Sort.by(Sort.Direction.DESC, "categoryId");
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        //Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Order> orderpage = orderRepository.findAll(pageDetails);
        //List<Order> orders = orderRepository.findAll();
        List<Order> orders = orderpage.getContent();
        if(orders.isEmpty()) throw new APIException("No Order created till now");
        List<OrderDTO> orderDTOS = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();
        //return orders;
        //return new OrderResponse(orderDTOS);
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
        //Pageable pageDetails = PageRequest.of(pageNumber, pageSize);
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

        if(orders.isEmpty()) throw new APIException("No Order created till now");
        List<OrderDTO> orderDTOS = orders.stream()
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
