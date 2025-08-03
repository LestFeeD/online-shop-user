package com.shopir.user.service;

import com.shopir.user.dto.ProductKafkaDto;
import com.shopir.user.dto.request.CreateCartRequestDto;
import com.shopir.user.dto.response.CartResponse;
import com.shopir.user.entity.Cart;
import com.shopir.user.exceptions.BadRequestException;
import com.shopir.user.exceptions.NotFoundException;
import com.shopir.user.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final KafkaConsumerService kafkaConsumerService;

    @Autowired
    public CartService(CartRepository cartRepository, KafkaConsumerService kafkaConsumerService) {
        this.cartRepository = cartRepository;
        this.kafkaConsumerService = kafkaConsumerService;
    }

    public List<CartResponse> finaAllCartByUser(Long idWebUser) throws Exception {
        Optional<List<Cart>> optionalCarts  = cartRepository.findAllCartByWebUser_IdWebUser(idWebUser);
        if (optionalCarts.isEmpty()) {
            return new ArrayList<>();
        }
        List<CartResponse> cartResponses = new ArrayList<>();
        for(Cart cart:  optionalCarts.get()) {
            ProductKafkaDto productKafkaDto = kafkaConsumerService.getProductByIdCart(cart.getIdCart());
            CartResponse cartResponse = CartResponse.builder()
                    .idCart(cart.getIdCart())
                    .idWebUser(idWebUser)
                    .idProduct(productKafkaDto.getIdProduct())
                    .nameProduct(productKafkaDto.getNameProduct())
                    .price(productKafkaDto.getPrice())
                    .quantity(cart.getQuantity())
                    .build();
            cartResponses.add(cartResponse);
        }
        return cartResponses;
    }

    public CartResponse findCart(Long idWebUser, Long idProduct) throws Exception {
        Optional<Cart> optionalCart  = cartRepository.findByIdWebUserAndIdProduct(idWebUser, idProduct);
        if(optionalCart.isEmpty()) {
            throw new BadRequestException("Not found cart by idUser  and idProduct");
        }
        ProductKafkaDto productKafkaDto = kafkaConsumerService.getNameProductById(idProduct);
        Cart cart = optionalCart.get();
        return CartResponse.builder()
                .idCart(cart.getIdCart())
                .idWebUser(idWebUser)
                .idProduct(idProduct)
                .nameProduct(productKafkaDto.getNameProduct())
                .price(productKafkaDto.getPrice())
                .quantity(cart.getQuantity())
                .build();
    }

    @Transactional
    public void createNewBasket(Long idUser, CreateCartRequestDto requestDto) {
        if (idUser == null || requestDto.getIdProduct() == null) {
            throw new BadRequestException("User ID and Product ID must not be null");
        }

        Optional<Cart> existingCart = cartRepository.findByIdWebUserAndIdProduct(
                idUser, requestDto.getIdProduct()
        );

        if (existingCart.isPresent()) {
            throw new BadRequestException("Cart already exists for this user and product");
        }
        cartRepository.saveCart(idUser, requestDto.getIdProduct());
    }

    public void plusQuantityCart(Long idCart, Long idWebUser) {
        Optional<Cart> optionalCart = cartRepository.findByIdCartAndWebUser_IdWebUser(idCart, idWebUser);
       if(optionalCart.isEmpty()) {
           throw new NotFoundException("Cart not found for user");
       }
        Cart cart = optionalCart.get();
        cartRepository.incrementQuantity(cart.getIdCart());
    }

    public void minusQuantityCart(Long idCart, Long idWebUser) {
        Optional<Cart> optionalCart = cartRepository.findByIdCartAndWebUser_IdWebUser(idCart, idWebUser);
        if(optionalCart.isEmpty()) {
            throw new NotFoundException("Cart not found for user");
        }
        Cart cart = optionalCart.get();
        if(cart.getQuantity() > 1) {
            cartRepository.decrementQuantity(idCart);
        } else {
            throw new BadRequestException("The quantity in the basket cannot be less than 1 ");
        }
    }

    @Transactional
    public void deleteCart(Long idCart) {
        Cart cart = cartRepository.findById(idCart).orElseThrow(() -> new NotFoundException("Not found cart by id"));
        cartRepository.delete(cart);
    }
}
