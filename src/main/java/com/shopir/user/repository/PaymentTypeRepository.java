package com.shopir.user.repository;

import com.shopir.user.entity.PaymentStatus;
import com.shopir.user.entity.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTypeRepository extends JpaRepository<PaymentType, Long> {
}
