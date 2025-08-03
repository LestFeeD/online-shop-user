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
@Table(name = "role_user")
public class RoleUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRoleUser;
    private String nameRole;

    @OneToMany(mappedBy = "roleUser", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<WebUser> webUsers;

}
