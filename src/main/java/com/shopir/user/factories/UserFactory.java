package com.shopir.user.factories;

import com.shopir.user.dto.response.CityResponseDto;
import com.shopir.user.dto.response.UserInformationResponseDto;
import com.shopir.user.entity.City;
import com.shopir.user.entity.WebUser;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    public UserInformationResponseDto makeUserDto(WebUser entity) {

        return UserInformationResponseDto.builder()
                .idUser(entity.getIdWebUser())
                .name(entity.getName())
                .surname(entity.getSurname())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .build();
    }
}
