package com.shopir.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCart;
    private Integer quantity;

    @ManyToOne
    @JoinColumn(name = "id_web_user")
    private WebUser webUser;
}
