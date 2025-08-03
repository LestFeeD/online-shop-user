package com.shopir.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class CreateOrderRequestDto {
    private List<Long> idCarts;
    private Long idPaymentType;
}
