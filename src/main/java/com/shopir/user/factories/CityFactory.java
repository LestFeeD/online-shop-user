package com.shopir.user.factories;

import com.shopir.user.dto.response.CityResponseDto;
import com.shopir.user.entity.City;
import org.springframework.stereotype.Component;

@Component
public class CityFactory {
    public CityResponseDto makeCityDto(City entity) {

        return CityResponseDto.builder()
                .idCity(entity.getIdCity())
                .nameCity(entity.getNameCity())
                .build();
    }
}
