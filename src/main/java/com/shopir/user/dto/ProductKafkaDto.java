package com.shopir.user.dto;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductKafkaDto {
    private Long idProduct;
    private String nameProduct;
    private BigDecimal price;
    private Long idCart;
    private Long idOrder;

}
