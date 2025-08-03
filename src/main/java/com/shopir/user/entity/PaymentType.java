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
@Table(name = "payment_type")
public class PaymentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPaymentType;
    private String name;

    @OneToMany(mappedBy = "paymentType", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<OrderUser> orderUsers;

}
