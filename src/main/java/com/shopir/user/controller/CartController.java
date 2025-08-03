package com.shopir.user.controller;

import com.shopir.user.dto.request.CreateCartRequestDto;
import com.shopir.user.dto.response.CartResponse;
import com.shopir.user.service.AuthenticationService;
import com.shopir.user.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class CartController {

    private final AuthenticationService authenticationService;
    private final CartService cartService;

    @Autowired
    public CartController(AuthenticationService authenticationService, CartService cartService) {
        this.authenticationService = authenticationService;
        this.cartService = cartService;
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/all-cart")
    public ResponseEntity<List<CartResponse>> findAllCartByUserId() throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<CartResponse> cartResponses =  cartService.finaAllCartByUser(idUser);
        return ResponseEntity.ok(cartResponses);
    }

    @GetMapping("/cart")
    public ResponseEntity<CartResponse> findCart(@RequestParam("idProduct") Long idProduct) throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CartResponse cartResponse =  cartService.findCart(idUser, idProduct);
        return ResponseEntity.ok(cartResponse);
    }

    @PostMapping("/cart")
    public ResponseEntity<Void> createBasket(@RequestBody CreateCartRequestDto requestDto) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cartService.createNewBasket(idUser, requestDto);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/plus-cart/{idCart}")
    public ResponseEntity<Void> plusQuantityCart(@PathVariable("idCart") Long idCart) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cartService.plusQuantityCart(idCart, idUser);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/minus-cart/{idCart}")
    public ResponseEntity<Void> minusQuantityCart(@PathVariable("idCart") Long idCart) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        cartService.minusQuantityCart(idCart, idUser);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/cart")
    public ResponseEntity<Void> deleteCart(@PathVariable Long idCart) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        cartService.deleteCart(idCart);
        return ResponseEntity.ok().build();
    }

}
