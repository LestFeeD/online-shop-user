package com.shopir.user.dto.response;

import java.sql.Date;

public interface OrderProductInfoView {
    Integer getOrderNumber();
    String getNameProduct();
    Date getDateSupply();
}

