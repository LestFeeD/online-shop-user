package com.shopir.user.repository;

import com.shopir.user.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Repository
public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO order_product(\n" +
            "\t id_product, quantity, product_price, id_order)\n" +
            "\tVALUES ( :idProduct, :quantity, :productPrice, :idOrder);", nativeQuery = true)
    void saveOrderProduct(@Param("idProduct") Long idProduct,
                          @Param("quantity") Integer quantity,
                          @Param("productPrice") BigDecimal productPrice,
                          @Param("idOrder") Long idOrder);
}
