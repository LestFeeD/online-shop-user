package com.shopir.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    private String correlationId;
    private String productName;
}
