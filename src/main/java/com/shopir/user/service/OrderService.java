package com.shopir.user.service;

import com.shopir.user.dto.ProductKafkaDto;
import com.shopir.user.dto.request.CreateOrderRequestDto;
import com.shopir.user.dto.response.OrderUserResponseDto;
import com.shopir.user.dto.response.TotalOrderResponseDto;
import com.shopir.user.entity.*;
import com.shopir.user.exceptions.NotFoundException;
import com.shopir.user.factories.OrderFactory;
import com.shopir.user.repository.CartRepository;
import com.shopir.user.repository.OrderProductRepository;
import com.shopir.user.repository.OrderUserRepository;
import com.shopir.user.repository.WebUserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderProductRepository orderProductRepository;
    private final OrderUserRepository orderUserRepository;
    private final CartRepository cartRepository;
    private final OrderFactory orderFactory;
    private final KafkaConsumerService kafkaConsumerService;

    @Autowired
    public OrderService(OrderProductRepository orderProductRepository, OrderUserRepository orderUserRepository, CartRepository cartRepository, OrderFactory orderFactory, KafkaConsumerService kafkaConsumerService) {
        this.orderProductRepository = orderProductRepository;
        this.orderUserRepository = orderUserRepository;
        this.cartRepository = cartRepository;
        this.orderFactory = orderFactory;
        this.kafkaConsumerService = kafkaConsumerService;
    }

    public OrderUserResponseDto findOrder(Long idOrder) throws Exception {
        OrderUser orderUser = orderUserRepository.findById(idOrder).orElseThrow(() -> new NotFoundException("Order with id " + idOrder + " not found"));
        List<ProductKafkaDto> dtos = kafkaConsumerService.getProductByIdOrder(idOrder);
        List<String> nameProducts = new ArrayList<>();
        for (ProductKafkaDto productKafkaDto: dtos) {
            nameProducts.add(productKafkaDto.getNameProduct());
        }
        return orderFactory.makeOrderDto(orderUser, nameProducts);

    }

    @Transactional
    public Long createOrder(  CreateOrderRequestDto requestDto) throws Exception {
        Cart cartUser = cartRepository.findById(requestDto.getIdCarts().getFirst())
                .orElseThrow(() -> new NotFoundException("Cart with id " + requestDto.getIdCarts().getFirst() + " not found"));
        List<ProductWithQuantity> dtoList = new ArrayList<>();
        for (Long id: requestDto.getIdCarts()) {
            Cart cart = cartRepository.findById(id).orElseThrow();


            ProductKafkaDto productKafkaDto = kafkaConsumerService.getProductByIdCart(id);
            logger.debug("Get info about idProduct and IdOrder from productKafkaDto: idProduct = {}, idOrder = {}", productKafkaDto.getIdProduct(), productKafkaDto.getIdOrder());
            dtoList.add(new ProductWithQuantity(productKafkaDto, cart.getQuantity()));
            cartRepository.delete(cart);
        }
        UUID randomTransactionNumber =  UUID.randomUUID();
        UUID randomOrderNumber =  UUID.randomUUID();

        Integer orderNumber = randomOrderNumber.hashCode();
        String transactionNumber = String.valueOf(randomTransactionNumber);
       Long idOrder = orderUserRepository.saveOrder(cartUser.getWebUser().getIdWebUser(), transactionNumber, requestDto.getIdPaymentType(), orderNumber);
       OrderUser orderUser = orderUserRepository.findById(idOrder).orElseThrow();

        for (ProductWithQuantity entry : dtoList) {
            ProductKafkaDto dto = entry.product();
            int quantity = entry.quantity();

            orderProductRepository.saveOrderProduct(dto.getIdProduct(), quantity, dto.getPrice(), orderUser.getIdOrder());
        }
        return idOrder;
    }

    public TotalOrderResponseDto saleWithinCertainTime(Date startDate, Date endDate) {
        if(startDate != null && endDate != null) {
            return orderUserRepository.findTotalSales(startDate, endDate);
        } else if (startDate != null && endDate == null) {
            LocalDateTime endDateTime = LocalDateTime.now();
            Date endDateAuto = (Date) Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
            return orderUserRepository.findTotalSales(startDate, endDateAuto);
        } else {
            LocalDateTime endDateTime = LocalDateTime.now();
            LocalDateTime startDateTime = endDateTime.minusWeeks(2);

            Date startDateAuto = (Date) Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
            Date endDateAuto = (Date) Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
            return orderUserRepository.findTotalSales(startDateAuto, endDateAuto);
        }
    }

    @Cacheable(value = "order")
    public List<OrderUserResponseDto> receivingOrdersByStatus(Long idUser, String status) {
        List<Object[]> rows = new ArrayList<>();
        if(status.equals("Формируется") || status.equals("Доставляется")) {
            rows = orderUserRepository.fetchOrderProductInfo(idUser,  List.of(1L, 2L));
        } else if(status.equals("Доставлен"))  {
            rows = orderUserRepository.fetchOrderProductInfo(idUser, List.of(3L));
        } else {
            return new ArrayList<>();
        }

        Map<Integer, OrderUserResponseDto> grouped = new LinkedHashMap<>();

        for (Object[] row : rows) {
            Integer orderNumber = (Integer) row[0];
            String productName = (String) row[1];
            Date dateSupply = (Date) row[2];

            grouped.computeIfAbsent(orderNumber, key ->
                    new OrderUserResponseDto(orderNumber, new ArrayList<>(), dateSupply)
            ).getNameProduct().add(productName);
        }
        return new ArrayList<>(grouped.values()).stream()
                .sorted(Comparator.comparing(OrderUserResponseDto::getDateSupply).reversed())
                .toList();

    }

}
