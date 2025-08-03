package com.shopir.user.dto.response;

import java.math.BigDecimal;

public interface TotalOrderResponseDto {
    Integer getNumberSales();
    Integer getGeneralSaleGoods();
    BigDecimal getGeneralSale();
}
