package com.shopir.user.UnitTests.service;

import com.shopir.user.dto.ProductKafkaDto;
import com.shopir.user.dto.request.CreateCartRequestDto;
import com.shopir.user.entity.Cart;
import com.shopir.user.exceptions.BadRequestException;
import com.shopir.user.repository.CartRepository;
import com.shopir.user.service.CartService;
import com.shopir.user.service.KafkaConsumerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTests {
    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private KafkaConsumerService kafkaConsumerService;

    @Test
    void finaAllCartByUser_findOneCartUser_returnListCartResponse() throws Exception {
        Cart cart = Mockito.mock(Cart.class);
        when(cart.getIdCart()).thenReturn(1L);
        when(cart.getQuantity()).thenReturn(2);

        List<Cart> carts = new ArrayList<>();
        carts.add(cart);

        ProductKafkaDto productKafkaDto = Mockito.mock();
        when(productKafkaDto.getIdProduct()).thenReturn(1L);
        when(productKafkaDto.getNameProduct()).thenReturn("Product Name");
        when(productKafkaDto.getPrice()).thenReturn(BigDecimal.valueOf(100.0));

        when(cartRepository.findAllCartByWebUser_IdWebUser(1L)).thenReturn(Optional.ofNullable(carts));
        when(kafkaConsumerService.getProductByIdCart(1L)).thenReturn(productKafkaDto);

     cartService.finaAllCartByUser(1L);
        verify(cartRepository).findAllCartByWebUser_IdWebUser(1L);
    }

    @Test
    void findCart_findCartUserByIdProductAndIdUser_returnCartResponse() throws Exception {
        Cart cart = new Cart();
        cart.setIdCart(1L);
        cart.setQuantity(2);

        ProductKafkaDto productKafkaDto = Mockito.mock();


        when(cartRepository.findByIdCartAndWebUser_IdWebUser(1L, 1L)).thenReturn(Optional.of(cart));
        when(kafkaConsumerService.getNameProductByIdCart(1L)).thenReturn(productKafkaDto);

        cartService.findCart(1L, 1L);

        verify(cartRepository).findByIdCartAndWebUser_IdWebUser(1L, 1L);
    }

    @Test
    void createNewBasket_createBasket() throws Exception {
        CreateCartRequestDto requestDto = CreateCartRequestDto.builder()
                .idProduct(1L)
                .build();
        Cart cart = new Cart();
        cart.setIdCart(1L);


        when(cartRepository.findByIdWebUserAndIdProduct(1L, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cart));

        doNothing().when(cartRepository).saveCart(1L, requestDto.getIdProduct());

        cartService.createNewCart(1L, requestDto);

        verify(cartRepository, times(2)).findByIdWebUserAndIdProduct(1L, 1L);
    }

    @Test
    void plusQuantityCart_addOne() throws Exception {
        Cart cart = new Cart();
        cart.setIdCart(1L);

        when(cartRepository.findByIdCartAndWebUser_IdWebUser(1L, 1L)).thenReturn(Optional.ofNullable(cart));
        doNothing().when(cartRepository).incrementQuantity(1L);

        cartService.plusQuantityCart(1L, 1L);

        verify(cartRepository).incrementQuantity(1L);
    }

    @Test
    void minusQuantityCart_minusOne() throws Exception {
        Cart cart = new Cart();
        cart.setIdCart(1L);
        cart.setQuantity(2);

        when(cartRepository.findByIdCartAndWebUser_IdWebUser(1L, 1L)).thenReturn(Optional.ofNullable(cart));
        doNothing().when(cartRepository).decrementQuantity(1L);

        cartService.minusQuantityCart(1L, 1L);

        verify(cartRepository).decrementQuantity(1L);
    }

    @Test
    void minusQuantityCart_quantityEquals1() throws Exception {
        Cart cart = new Cart();
        cart.setIdCart(1L);
        cart.setQuantity(1);

        when(cartRepository.findByIdCartAndWebUser_IdWebUser(1L, 1L)).thenReturn(Optional.ofNullable(cart));

        assertThrows(BadRequestException.class, () -> {
            cartService.minusQuantityCart(1L, 1L);});

        verify(cartRepository, never()).decrementQuantity(1L);
    }

    @Test
    void deleteCart_getIdCartForDelete() throws Exception {
        Cart cart = new Cart();
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        doNothing().when(cartRepository).delete(cart);

        cartService.deleteCart(1L);
        verify(cartRepository).delete(cart);
    }
}

