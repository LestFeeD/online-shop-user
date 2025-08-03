package com.shopir.user.repository;

import com.shopir.user.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO cart(\n" +
            "\tid_product, quantity, id_web_user)\n" +
            "\tVALUES (:idProduct, 1, :idWebUser);", nativeQuery = true)
     void  saveCart(@Param("idWebUser") Long idWebUser, @Param("idProduct") Long idProduct);

    @Query(value = "SELECT id_cart, id_product, quantity, id_web_user\n" +
            "\tFROM cart WHERE id_product = :idProduct AND id_web_user = :idWebUser", nativeQuery = true)
    Optional<Cart> findByIdWebUserAndIdProduct(@Param("idWebUser") Long idWebUser, @Param("idProduct") Long idProduct);

    @Transactional
    @Modifying
    @Query("UPDATE Cart c SET c.quantity = c.quantity + 1 WHERE c.id = :idCart")
    void incrementQuantity(@Param("idCart") Long idCart);

    @Transactional
    @Modifying
    @Query("UPDATE Cart c SET c.quantity = c.quantity - 1 WHERE c.id = :idCart")
    void decrementQuantity(@Param("idCart") Long idCart);

    Optional<List<Cart>> findAllCartByWebUser_IdWebUser(Long idWebUser);

    Optional<Cart> findByIdCartAndWebUser_IdWebUser(Long idCart, Long idWebUser);
}
