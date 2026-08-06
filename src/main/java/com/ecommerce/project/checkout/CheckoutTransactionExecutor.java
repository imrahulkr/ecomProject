package com.ecommerce.project.checkout;

import com.ecommerce.project.address.Address;
import com.ecommerce.project.address.AddressRepository;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartItem;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.inventory.InventoryService;
import com.ecommerce.project.inventory.StockReservation;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.order.OrderItemRepository;
import com.ecommerce.project.order.OrderRepository;
import com.ecommerce.project.order.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Split out from CheckoutServiceImpl so @Transactional actually applies: CheckoutServiceImpl
// calls this from a non-transactional method (the payment provider call that follows must run
// outside any DB transaction), and a self-invoked @Transactional method on the same class would
// silently run without a transaction at all - see ReservationExpiryTransactionHelper for the
// same pattern in Phase 3.
@Component
@RequiredArgsConstructor
class CheckoutTransactionExecutor {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // Single transaction: every reservation's conditional stock UPDATE and the Order/OrderItem
    // inserts either all commit together or all roll back together - never a reservation left
    // holding stock for an order that doesn't exist, or vice versa.
    @Transactional
    OrderCreationResult createOrderWithReservation(String userEmail, Long userId, Long addressId) {
        Cart cart = cartRepository.findCartByEmail(userEmail);
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }

        Address address = addressRepository.findByAddressId(addressId);
        if (address == null) {
            throw new ResourceNotFoundException("address", "addressId", addressId);
        }
        if (address.getUser() == null || !address.getUser().getUserId().equals(userId)) {
            throw new APIException("Address does not belong to the current user");
        }

        Order order = new Order();
        order.setEmail(userEmail);
        order.setAddress(address);
        order.setOrderDate(LocalDate.now());
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setAmountMinorUnits(cart.getTotalPriceMinorUnits());
        order.setCurrency(cart.getCurrency());
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            StockReservation reservation = inventoryService.reserve(
                    cartItem.getProduct().getProductId(), cartItem.getQuantity(), cart);
            reservation.setOrder(savedOrder);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setOrder(savedOrder);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPriceMinorUnits(cartItem.getProductPriceMinorUnits());
            orderItem.setCurrency(cartItem.getCurrency());
            orderItem.setSellerId(cartItem.getProduct().getUser().getUserId());
            orderItems.add(orderItem);
        }
        orderItems = orderItemRepository.saveAll(orderItems);

        return new OrderCreationResult(savedOrder, orderItems);
    }
}
