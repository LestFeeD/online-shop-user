package com.shopir.user.controller;

import com.shopir.user.dto.request.CreateOrderRequestDto;
import com.shopir.user.dto.response.OrderUserResponseDto;
import com.shopir.user.dto.response.TotalOrderResponseDto;
import com.shopir.user.service.AuthenticationService;
import com.shopir.user.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.Date;
import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final AuthenticationService authenticationService;

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    public OrderController(AuthenticationService authenticationService, OrderService orderService) {
        this.authenticationService = authenticationService;
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @GetMapping("/order/{idOrder}")
    public ResponseEntity<OrderUserResponseDto> findOrder(@PathVariable Long idOrder) throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        orderService.findOrder( idOrder);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    @PostMapping("/order")
    public ResponseEntity<Long> createOrder(@RequestBody CreateOrderRequestDto requestDto) throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long id =  orderService.createOrder( requestDto);
        URI location = URI.create("/order/" + id);

        return ResponseEntity
                .created(location)
                .body(id);
    }

    @PreAuthorize("hasRole('ADMIN', 'MANAGER' )")
    @GetMapping("/order")
    public ResponseEntity<TotalOrderResponseDto> getSaleWithinCertainTime(@RequestParam("startDate") Date startDate, @RequestParam("endDate") Date endDate) throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        orderService.saleWithinCertainTime(startDate, endDate);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/history-order")
    public ResponseEntity<List<OrderUserResponseDto>> getOrders(@RequestParam("status") String status) throws Exception {
        Long idUser = authenticationService.getCurrentUserId();
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<OrderUserResponseDto> orders =  orderService.receivingOrdersByStatus(idUser, status);
        return ResponseEntity.ok(orders);
    }
}
