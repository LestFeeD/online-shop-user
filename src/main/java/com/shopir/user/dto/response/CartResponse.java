package com.shopir.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CartResponse {
    private Long idCart;
    private Long idWebUser;
    private Long idProduct;
    private String nameProduct;
    private BigDecimal price;
    private Integer quantity;
}
