package com.shopir.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_user")
public class OrderUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrder;
    private Date dateSupply;
    @JsonProperty("order_number")
    private Integer orderNumber;

    @ManyToOne
    @JoinColumn(name = "id_payment_status")
    private PaymentStatus paymentStatus;

    @ManyToOne
    @JoinColumn(name = "id_payment_type")
    private PaymentType paymentType;

    @ManyToOne
    @JoinColumn(name = "id_web_user")
    private WebUser webUser;

    @ManyToOne
    @JoinColumn(name = "id_order_status")
    private OrderStatus orderStatus;

    private String transactionNumber;

    @OneToMany(mappedBy = "orderUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<Delivery> deliveries;

    @OneToMany(mappedBy = "orderUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<OrderProduct> orderProducts;
}
