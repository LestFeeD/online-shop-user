package com.shopir.user.repository;

import com.shopir.user.entity.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleUserRepository extends JpaRepository<RoleUser, Long> {
    Optional<RoleUser> findByNameRole(String name);
}
