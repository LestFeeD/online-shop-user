package com.shopir.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopir.user.dto.ProductKafkaDto;
import com.shopir.user.dto.request.GetIdProductRequestDto;
import com.shopir.user.entity.CartProduct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final Map<Long, CompletableFuture<List<ProductKafkaDto>>> pendingListRequests = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<ProductKafkaDto>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Long>> pendingLongRequests = new ConcurrentHashMap<>();


    @Autowired
    public KafkaConsumerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public ProductKafkaDto getNameProductById(Long idProduct) throws Exception {
        CompletableFuture<ProductKafkaDto> future = new CompletableFuture<>();
        pendingRequests.put(idProduct, future);

        kafkaTemplate.send("product-request", String.valueOf(idProduct));

        return future.get(20, TimeUnit.SECONDS);
    }

    @KafkaListener(topics = "product-response", groupId = "user-group")
    public void handleProductByIdResponse(String json) throws JsonProcessingException {
        ProductKafkaDto dto = objectMapper.readValue(json, ProductKafkaDto.class);
        Long idProduct = dto.getIdProduct();

        CompletableFuture<ProductKafkaDto> future = pendingRequests.remove(idProduct);
        if (future != null) {
            future.complete(dto);
        }
    }

    public ProductKafkaDto getProductByIdCart(Long idCart) throws Exception {
        CompletableFuture<ProductKafkaDto> future = new CompletableFuture<>();
        pendingRequests.put(idCart, future);

        kafkaTemplate.send("cart-product-request", String.valueOf(idCart));

        return future.get(20, TimeUnit.SECONDS);
    }

    @KafkaListener(topics = "cart-product-response", groupId = "user-group")
    public void handleProductByIdCartResponse(String json) throws JsonProcessingException {
        ProductKafkaDto dto = objectMapper.readValue(json, ProductKafkaDto.class);
        Long idCart = dto.getIdCart();

        CompletableFuture<ProductKafkaDto> future = pendingRequests.remove(idCart);
        if (future != null) {
            future.complete(dto);
        }
    }


    public Long getIdProductByIdCart(GetIdProductRequestDto requestDto) throws Exception {
        CompletableFuture<Long> future = new CompletableFuture<>();
        pendingLongRequests.put(requestDto.getIdCart(), future);

        kafkaTemplate.send("cart-idProduct-request", String.valueOf(requestDto.getIdCart()), "request");

        return future.get(20, TimeUnit.SECONDS);
    }

    @KafkaListener(topics = "idProduct-response", groupId = "user-group")
    public void handleIdProductResponse(String json) throws JsonProcessingException {
        GetIdProductRequestDto dto = objectMapper.readValue(json, GetIdProductRequestDto.class);

        Long idProduct = Long.parseLong(String.valueOf(dto.getIdProduct()));
        CompletableFuture<Long> future = pendingLongRequests.remove(dto.getIdCart());

        if (future != null) {
            future.complete(idProduct);
        }
    }

    public List<ProductKafkaDto> getProductByIdOrder(Long idOrder) throws Exception {
        CompletableFuture<List<ProductKafkaDto>> future = new CompletableFuture<>();
        pendingListRequests.put(idOrder, future);

        kafkaTemplate.send("order-product-request", String.valueOf(idOrder), "request");

        return future.get(20, TimeUnit.SECONDS); // вернёт List<ProductKafkaDto>
    }


    @KafkaListener(topics = "order-product-response", groupId = "user-group")
    public void handleProductResponse(String json) throws JsonProcessingException {
        List<ProductKafkaDto> dtos = objectMapper.readValue(json, new TypeReference<List<ProductKafkaDto>>() {});

        if (dtos.isEmpty()) {
            logger.warn("Received empty product list from Kafka.");
            return;
        }

        Long idOrder = dtos.get(0).getIdOrder();

        CompletableFuture<List<ProductKafkaDto>> future = pendingListRequests.remove(idOrder);
        if (future != null) {
            future.complete(dtos);
        } else {
            logger.warn("No pending request found for order id: " + idOrder);
        }
    }



}
