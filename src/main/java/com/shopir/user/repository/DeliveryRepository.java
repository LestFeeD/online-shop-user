package com.shopir.user.repository;

import com.shopir.user.entity.City;
import com.shopir.user.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}
