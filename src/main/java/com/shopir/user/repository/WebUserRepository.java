package com.shopir.user.repository;

import com.shopir.user.entity.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@Repository
public interface WebUserRepository extends JpaRepository<WebUser, Long> {

    @Query(nativeQuery = true,
    value = "SELECT * FROM web_user WHERE email = :email AND password = :password")
    Optional<WebUser> findWebUser(@Param("email") String email, @Param("password") String password);

    Optional<WebUser> findByEmail(String email);


    @Query(nativeQuery = true,
            value = "SELECT wu.id_web_user FROM web_user wu " +
                    "JOIN confirmation_token ct ON ct.id_web_user = wu.id_web_user " +
                    "WHERE ct.id_confirmation_token = :id_confirmation_token ")
    Long findByIdToken(@Param("id_confirmation_token") Long idToken);
}
