package com.ecommerce.project.order;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.notification.email.event.OnItemDeliveredEvent;
import com.ecommerce.project.notification.email.event.OnItemShippedEvent;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import com.ecommerce.project.order.dto.FulfillmentUpdateDTO;
import com.ecommerce.project.order.dto.OrderDTO;
import com.ecommerce.project.order.dto.OrderItemDTO;
import com.ecommerce.project.order.dto.OrderResponse;
import com.ecommerce.project.auth.User;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;
    private final ApplicationEventPublisher eventPublisher;

    // @Transactional on every method here: Order.items is a lazy @OneToMany, and with
    // spring.jpa.open-in-view=false (see application.properties) there's no session left open
    // for ModelMapper (or, in getAllSellerOrders, the direct order.getItems() call) to lazily
    // initialize it once the repository call that loaded the Order has returned.
    @Override
    @Transactional
    public OrderDTO getOrderByIdForUser(String emailId, Long orderId) {
        Order order = orderRepository.findByOrderIdAndEmail(orderId, emailId)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
        return modelMapper.map(order, OrderDTO.class);
    }

    @Override
    @Transactional
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

    // No ownership restriction - this is the admin override path (AdminOrderController). It no
    // longer takes an emailId: the previous version overwrote the order's customer email with
    // whichever caller happened to update the status, which was a data-corrupting bug rather
    // than anything intentional.
    @Override
    @Transactional
    public OrderDTO updateOrderStatusAsAdmin(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("order", "orderId", orderId));
        order.setOrderStatus(status);
        orderRepository.save(order);

        return modelMapper.map(order, OrderDTO.class);
    }

    // Filters at the query level (JOIN on order items' product owner) rather than loading every
    // order and filtering in memory - the previous version did the latter, which both scaled
    // badly and doesn't match the ownership-scoping pattern used everywhere else.
    // Order.items on a multi-seller order contains every seller's line items, not just the
    // caller's - modelMapper.map(order, OrderDTO.class) would otherwise leak another seller's
    // products/quantities/pricing to this seller. Scoped down to only the caller's own items.
    @Override
    @Transactional
    public OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        User seller = authUtil.loggedInUser();
        Page<Order> orderpage = orderRepository.findBySellerId(seller.getUserId(), pageDetails);
        List<Order> orders = orderpage.getContent();

        List<OrderDTO> orderDTOS = orders.isEmpty() ? Collections.emptyList() : orders.stream()
                .map(order -> toSellerScopedOrderDTO(order, seller.getUserId()))
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

    private OrderDTO toSellerScopedOrderDTO(Order order, Long sellerId) {
        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
        List<OrderItemDTO> ownItems = order.getItems().stream()
                .filter(item -> sellerId.equals(item.getSellerId()))
                .map(item -> modelMapper.map(item, OrderItemDTO.class))
                .toList();
        orderDTO.setOrderItems(ownItems);
        return orderDTO;
    }

    // Seller-scoped: findByOrderItemIdAndSellerId returns empty (404) for any item that isn't
    // this seller's own, matching the ownership-scoping pattern used everywhere else rather than
    // fetch-then-check-then-403.
    @Override
    @Transactional
    public OrderItemDTO updateFulfillmentStatusAsSeller(Long sellerId, Long orderItemId, FulfillmentUpdateDTO update) {
        OrderItem orderItem = orderItemRepository.findByOrderItemIdAndSellerId(orderItemId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("orderItem", "orderItemId", orderItemId));
        return applyFulfillmentTransition(orderItem, update);
    }

    // Admin override: no ownership restriction, mirroring updateOrderStatusAsAdmin.
    @Override
    @Transactional
    public OrderItemDTO updateFulfillmentStatusAsAdmin(Long orderItemId, FulfillmentUpdateDTO update) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("orderItem", "orderItemId", orderItemId));
        return applyFulfillmentTransition(orderItem, update);
    }

    // State machine: PENDING -> SHIPPED -> DELIVERED, or PENDING -> CANCELLED. Every other
    // transition (skipping a state, going backwards, re-issuing the same state) is rejected.
    private OrderItemDTO applyFulfillmentTransition(OrderItem orderItem, FulfillmentUpdateDTO update) {
        if (!OrderStatus.PAID.name().equals(orderItem.getOrder().getOrderStatus())) {
            throw new APIException("Order is not paid yet - fulfillment cannot start");
        }

        FulfillmentStatus current = orderItem.getFulfillmentStatus();
        FulfillmentStatus target = update.getStatus();
        if (target == null) {
            throw new APIException("status is required");
        }

        if (current == FulfillmentStatus.PENDING && target == FulfillmentStatus.SHIPPED) {
            if (update.getTrackingNumber() == null || update.getTrackingNumber().isBlank()
                    || update.getCarrier() == null || update.getCarrier().isBlank()) {
                throw new APIException("trackingNumber and carrier are required to mark an item as shipped");
            }
            orderItem.setTrackingNumber(update.getTrackingNumber());
            orderItem.setCarrier(update.getCarrier());
            orderItem.setShippedAt(LocalDateTime.now());
            orderItem.setFulfillmentStatus(FulfillmentStatus.SHIPPED);
            OrderItem saved = orderItemRepository.save(orderItem);
            eventPublisher.publishEvent(new OnItemShippedEvent(this, saved));
            return modelMapper.map(saved, OrderItemDTO.class);
        }

        if (current == FulfillmentStatus.SHIPPED && target == FulfillmentStatus.DELIVERED) {
            orderItem.setDeliveredAt(LocalDateTime.now());
            orderItem.setFulfillmentStatus(FulfillmentStatus.DELIVERED);
            OrderItem saved = orderItemRepository.save(orderItem);
            eventPublisher.publishEvent(new OnItemDeliveredEvent(this, saved));
            return modelMapper.map(saved, OrderItemDTO.class);
        }

        if (current == FulfillmentStatus.PENDING && target == FulfillmentStatus.CANCELLED) {
            orderItem.setFulfillmentStatus(FulfillmentStatus.CANCELLED);
            OrderItem saved = orderItemRepository.save(orderItem);
            return modelMapper.map(saved, OrderItemDTO.class);
        }

        throw new APIException("Cannot transition fulfillment status from " + current + " to " + target);
    }
}
