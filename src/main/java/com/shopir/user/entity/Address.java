package com.shopir.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAddress;

    @ManyToOne
    @JoinColumn(name = "id_city")
    private City city;

    private String street;
    private String home;
    private Integer floor;
    private String additionalInformation;

    @ManyToOne
    @JoinColumn(name = "id_web_user")
    private WebUser webUser;

}
