package com.example.orderservice.service;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(orderRequest.getOrderLineItemsDtoList().stream().map(this::mapFromDto).collect(Collectors.toList()));
        List<String> skuCodes = orderRequest.getOrderLineItemsDtoList().stream().map(OrderLineItemsDto::getSkuCode).collect(Collectors.toList());
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();
        Boolean result = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
        if(Boolean.TRUE.equals(result))
            orderRepository.save(order);
        else
            throw new IllegalArgumentException("Product is not in stock, please try again later");
    }

    private OrderLineItems mapFromDto(OrderLineItemsDto itemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setQuantity(itemsDto.getQuantity());
        orderLineItems.setSkuCode(itemsDto.getSkuCode());
        orderLineItems.setPrice(itemsDto.getPrice());
        return orderLineItems;
    }
}
