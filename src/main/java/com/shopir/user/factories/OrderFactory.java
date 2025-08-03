package com.shopir.user.factories;

import com.shopir.user.dto.response.OrderUserResponseDto;
import com.shopir.user.dto.response.UserInformationResponseDto;
import com.shopir.user.entity.OrderUser;
import com.shopir.user.entity.WebUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderFactory {
    public OrderUserResponseDto makeOrderDto(OrderUser entity, List<String> nameProducts) {

        return OrderUserResponseDto.builder()
                .orderNumber(entity.getOrderNumber())
                .nameProduct(nameProducts)
                .dateSupply(entity.getDateSupply())
                .build();
    }
}
