package com.shopir.user.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EditUserResponseDto {
    private UserInformationResponseDto user;
    private String token;
}

