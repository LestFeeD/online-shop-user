package com.shopir.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "confirmation_token")
public class ConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_confirmation_token")
    private Long idConfirmationToken;

    @Column(name = "user_token")
    private String userToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne
    @JoinColumn(name = "id_web_user")
    private WebUser webUser;

    public ConfirmationToken(String token, LocalDateTime now, LocalDateTime expiresAt, WebUser webUser) {
        this.userToken = token;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.webUser = webUser;
    }
}
