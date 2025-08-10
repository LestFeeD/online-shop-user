package com.shopir.user.repository;

import com.shopir.user.dto.response.OrderUserResponseDto;
import com.shopir.user.dto.response.TotalOrderResponseDto;
import com.shopir.user.entity.OrderUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderUserRepository extends JpaRepository<OrderUser, Long> {
    @Transactional
    @Query(value = "INSERT INTO order_user(\n" +
            "\t id_payment_status, id_payment_type, id_web_user, transaction_number, order_number)\n" +
            "\tVALUES ( 1, :idPaymentType, :idWebUser, :transactionNumber, :orderNumber) RETURNING id_order;", nativeQuery = true)
    Long saveOrder(@Param("idWebUser") Long idWebUser,
                    @Param("transactionNumber") String transactionNumber,
                    @Param("idPaymentType") Long idPaymentType,
                   @Param("orderNumber") Integer orderNumber);

    @Query(nativeQuery = true,
    value = "SELECT \n" +
            "    COUNT(DISTINCT ou.id_order) AS numberSales, \n" +
            "    SUM(op.quantity) AS generalSaleGoods, \n" +
            "    SUM(op.product_price) AS generalSale \n" +
            "FROM order_user ou\n" +
            "JOIN order_product op ON op.id_order = ou.id_order\n" +
            "WHERE ou.date_supply BETWEEN :startDate AND :endDate ")
    TotalOrderResponseDto findTotalSales(@Param("startDate")  Date startDate, @Param("endDate") Date endDate);

    @Query(nativeQuery = true,
            value = "SELECT ou.order_number AS orderNumber, p.name_product AS nameProduct,\n" +
                    "ou.date_supply AS dateSupply FROM order_user ou\n" +
                    "JOIN order_product op ON op.id_order = ou.id_order\n" +
                    "JOIN product p ON p.id_product = op.id_product WHERE ou.id_order_status IN (:statusList) AND ou.id_web_user = :idUser")
    List<Object[]> fetchOrderProductInfo(Long idUser, List<Long> statusList);

    Optional<OrderUser> findByIdOrderAndWebUser_IdWebUser(Long idOrderUser, Long idWebUser);
}
