package com.shopir.user.repository;

import com.shopir.user.entity.City;
import com.shopir.user.entity.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;
import java.util.stream.Collectors;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    @Query(value = "SELECT id_confirmation_token " +
            "FROM confirmation_token " +
            "WHERE expires_at < :now " +
            "AND confirmed_at IS NULL", nativeQuery = true)
    Set<Long> findAllExpiredToken(@Param("now") Timestamp timestamp);

    @Query(value = "SELECT * FROM confirmation_token WHERE user_token = :user_token", nativeQuery = true)
    ConfirmationToken findByToken(@Param("user_token")String token);
}
