package com.shopir.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_status")
public class OrderStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrderStatus;
    private String nameOrderStatus;

    @OneToMany(mappedBy = "orderStatus", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<OrderUser> orderUsers;
}
