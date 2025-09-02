package com.shopir.user.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "web_user")
public class WebUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idWebUser;
    private String name;
    private String surname;
    private String phone;
    private String email;
    private String temporaryMail;
    private String password;
    private Integer activated;

    @ManyToOne
    @JoinColumn(name = "id_role_user")
    private RoleUser roleUser;

    @OneToMany(mappedBy = "webUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<OrderUser> orderUsers;

    @OneToMany(mappedBy = "webUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<Cart> baskets;

    @OneToMany(mappedBy = "webUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<Address> addresses;

    @OneToMany(mappedBy = "webUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<ConfirmationToken> confirmationTokens;
}
