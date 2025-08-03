package com.shopir.user.repository;

import com.shopir.user.entity.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Repository
public interface WebUserRepository extends JpaRepository<WebUser, Long> {
    @Modifying
    @Transactional
    @Query(nativeQuery = true,
    value = "SELECT * FROM web_user WHERE email = :email AND password = :password")
    Optional<WebUser> findWebUser(@Param("email") String email, @Param("password") String password);

    Optional<WebUser> findByEmail(String email);
}
