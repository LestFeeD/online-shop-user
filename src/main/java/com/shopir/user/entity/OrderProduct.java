package com.shopir.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_product")
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrderProduct;

    @ManyToOne
    @JoinColumn(name = "id_order")
    private OrderUser orderUser;
}
