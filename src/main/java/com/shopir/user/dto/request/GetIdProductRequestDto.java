package com.shopir.user.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GetIdProductRequestDto {
    private Long idCart;
    private Long idProduct;
}
