package com.springpractice.orderservice.service;

import com.springpractice.orderservice.dto.InventoryResponse;
import com.springpractice.orderservice.dto.OrderLineItemsDto;
import com.springpractice.orderservice.dto.OrderRequest;
import com.springpractice.orderservice.model.Order;
import com.springpractice.orderservice.model.OrderLineItems;
import com.springpractice.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDto).toList();
        order.setOrderLineItems(orderLineItems);

        List<String> skuCodeForInventoryCheckUp = order.getOrderLineItems().stream().map(OrderLineItems::getSkuCode).toList();
        // Call Order service
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",uriBuilder -> uriBuilder.queryParam("skuCode",skuCodeForInventoryCheckUp).build())
                        .retrieve()
                                .bodyToMono(InventoryResponse[].class)
                                        .block();

        System.out.println(Arrays.stream(inventoryResponses).toList().get(0).getIsInStock());

        Boolean allProductsInstock =Arrays.stream(inventoryResponses).allMatch(InventoryResponse::getIsInStock);
        if(allProductsInstock){
            orderRepository.save(order);
        }else {
            throw new RuntimeException("Inventory is out of stock");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        return orderLineItems;
    }
}
