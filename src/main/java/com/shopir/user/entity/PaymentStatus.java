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
@Table(name = "payment_status")
public class PaymentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPaymentStatus;
    private String name;

    @OneToMany(mappedBy = "paymentStatus", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<OrderUser> orderUsers;
}
