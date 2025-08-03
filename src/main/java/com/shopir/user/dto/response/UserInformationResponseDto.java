package com.shopir.user.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInformationResponseDto {
    private Long idUser;
    private String name;
    private String surname;
    private String phone;
    private String email;

}
