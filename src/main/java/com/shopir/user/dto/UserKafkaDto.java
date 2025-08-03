package com.shopir.user.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserKafkaDto {
    private Long idUser;
    private String email;
    private String password;
    private String role;
}
