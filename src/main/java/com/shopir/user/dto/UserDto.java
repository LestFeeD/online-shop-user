package com.shopir.user.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private Long idUser;
    private Long idRole;
    private String name;
    private String surname;
    private String email;
    private String phone;
    private String password;
}
