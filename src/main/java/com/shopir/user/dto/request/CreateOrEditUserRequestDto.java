package com.shopir.user.dto.request;

import com.shopir.user.validation.ExtendedEmailValidator;
import com.shopir.user.validation.PhoneNumber;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateOrEditUserRequestDto {
    private String name;
    private String surname;
    @PhoneNumber
    private String phone;
    @ExtendedEmailValidator
    private String email;
    private String password;
    private Long idCity;
    private String street;
    private String home;
    private Integer floor;
    private String additionalInformation;
}
