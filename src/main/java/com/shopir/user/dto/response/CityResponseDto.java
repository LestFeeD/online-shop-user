package com.shopir.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CityResponseDto {
    private Long idCity;
    private String nameCity;
}
