package com.shopir.user.dto.response;

import lombok.*;

import java.sql.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderUserResponseDto {
    private Integer orderNumber;
    private List<String> nameProduct;
    private Date dateSupply;
}
