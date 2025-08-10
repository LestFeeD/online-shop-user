package com.shopir.user.controller;

import com.shopir.user.dto.request.CreateCartRequestDto;
import com.shopir.user.dto.response.CartResponse;
import com.shopir.user.service.AuthenticationService;
import com.shopir.user.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Cart")
@RestController
public class CartController {

    private final AuthenticationService authenticationService;
    private final CartService cartService;

    @Autowired
    public CartController(AuthenticationService authenticationService, CartService cartService) {
        this.authenticationService = authenticationService;
        this.cartService = cartService;
    }

    @Operation(
            summary = "Getting all the items from the cart.")
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

    @Operation(
            summary = "Receiving a basket according to his ID.")
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/cart/{idCart}")
    public ResponseEntity<CartResponse> findCart(@PathVariable("idCart") Long idCart) throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CartResponse cartResponse =  cartService.findCart(idUser, idCart);
        return ResponseEntity.ok(cartResponse);
    }

    @Operation(
            summary = "Creating a user's shopping cart.")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/cart")
    public ResponseEntity<Void> createCart(@RequestBody CreateCartRequestDto requestDto) {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long idCart =  cartService.createNewCart(idUser, requestDto);
        URI location = URI.create("/cart/" + idCart);

        return ResponseEntity.created(location).build();
    }

    @Operation(
            summary = "Adding the quantity of the product in the basket.")
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

    @Operation(
            summary = "Reducing the quantity of goods in the basket.")
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

    @Operation(
            summary = "Removing an product from the general shopping cart.")
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
