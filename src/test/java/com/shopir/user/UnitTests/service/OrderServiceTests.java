package com.shopir.user.UnitTests.service;

import com.shopir.user.dto.ProductKafkaDto;
import com.shopir.user.dto.request.CreateOrderRequestDto;
import com.shopir.user.entity.Cart;
import com.shopir.user.entity.OrderUser;
import com.shopir.user.entity.WebUser;
import com.shopir.user.repository.CartRepository;
import com.shopir.user.repository.OrderProductRepository;
import com.shopir.user.repository.OrderUserRepository;
import com.shopir.user.service.KafkaConsumerService;
import com.shopir.user.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTests {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private  OrderProductRepository orderProductRepository;

    @Mock
    private OrderUserRepository orderUserRepository;

    @Mock
    private KafkaConsumerService kafkaConsumerService;


    @Test
    void createOrder_createOrderForUser() throws Exception {
        CreateOrderRequestDto requestDto = CreateOrderRequestDto.builder()
                .idCarts(List.of(1L))
                .idPaymentType(1L)
                .build();

        Cart cart = new Cart();
        cart.setIdCart(1L);
        cart.setQuantity(1);

        WebUser user = new WebUser();
        user.setIdWebUser(1L);
        cart.setWebUser(user);

        ProductKafkaDto productKafkaDto = new ProductKafkaDto();
        productKafkaDto.setIdProduct(1L);
        productKafkaDto.setNameProduct("Product");
        productKafkaDto.setPrice(BigDecimal.valueOf(20));

        OrderUser orderUser = new OrderUser();
        orderUser.setIdOrder(1L);

        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(kafkaConsumerService.getProductByIdCart(1L)).thenReturn(productKafkaDto);
        doNothing().when(cartRepository).delete(cart);
        when(orderUserRepository.saveOrder(eq(1L), anyString(), eq(1L), anyInt())).thenReturn(1L);
        when(orderUserRepository.findById(1L)).thenReturn(Optional.of(orderUser));
        doNothing().when(orderProductRepository).saveOrderProduct(
                1L, 1, BigDecimal.valueOf(20), 1L);

        orderService.createOrder( requestDto);

        verify(cartRepository).delete(cart);
        verify(orderUserRepository).saveOrder(eq(1L), anyString(), eq(1L), anyInt());
        verify(orderProductRepository).saveOrderProduct(1L, 1, BigDecimal.valueOf(20), 1L);

    }
}
